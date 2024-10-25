'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
    uploadRandomizedUser,
    processRegisterReply,
    saveUsersToCSV
}

const fs = require('fs'); // Needed for file writing.

var registeredUsers = [];
var userTokens = [];
var isCSVHeaderWritten = false; // To ensure headers are written only once

// Returns a random username constructed from lowercase letters.
function randomUsername(char_limit) {
    const letters = 'abcdefghijklmnopqrstuvwxyz';
    let username = '';
    let num_chars = Math.floor(Math.random() * char_limit);
    for (let i = 0; i < num_chars; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}

// Returns a random password, drawn from printable ASCII characters
function randomPassword(pass_len) {
    const skip_value = 33;
    const lim_values = 94;

    let password = '';
    for (let i = 0; i < pass_len; i++) {
        let chosen_char = Math.floor(Math.random() * lim_values) + skip_value;
        if (chosen_char === "'" || chosen_char === '"') {
            i -= 1;
        } else {
            password += String.fromCharCode(chosen_char);
        }
    }
    return password;
}

/**
 * Process reply of the user registration.
 */
function processRegisterReply(requestParams, response, context, ee, next) {
    if (typeof response.body !== 'undefined' && response.body.length > 0) {
        const userInfo = {
            user: JSON.parse(requestParams.body), // Parse user info back to object
            token: response.body.token || null // Store the token if returned
        };
        registeredUsers.push(userInfo); // Save user info along with the token
        if (userInfo.token) {
            userTokens.push(userInfo.token); // Save token separately if needed
        }
        // Save the new user to CSV after processing the reply
        saveUsersToCSV(userInfo.user); // Pass the new user object
    }
    return next();
}

/**
 * Register a random user.
 */
function uploadRandomizedUser(requestParams, context, ee, next) {
    let username = randomUsername(10);
    let pword = randomPassword(15);
    let email = username + "@campus.fct.unl.pt";
    let displayName = username;

    const user = {
        userId: username,
        pwd: pword,
        email: email,
        displayName: username
    };
    requestParams.body = JSON.stringify(user);
    return next();
}

/**
 * Save the registered user to a CSV file.
 */
function saveUsersToCSV(newUser) {
    console.log('Saving user');
    const csvFilePath = './data/registered_users.csv';
    const csvRow = `${newUser.userId},${newUser.pwd},${newUser.email},${newUser.displayName},${newUser.token || ''}\n`;

    // Check if the CSV file exists to determine if we need to write headers
    if (!fs.existsSync(csvFilePath)) {
        // Write headers if the file does not exist
        fs.writeFileSync(csvFilePath, 'userId,pwd,email,displayName,token\n');
    }

    // Append the new user's data
    fs.appendFileSync(csvFilePath, csvRow);
}

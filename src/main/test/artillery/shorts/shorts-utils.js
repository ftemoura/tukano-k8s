module.exports = {
    addMultipartFormData,
    saveShortId,
    saveBlobUrl
}


const fs = require('fs');
const FormData = require('form-data');

function addMultipartFormData(requestParams, context, ee, next) {
    const form = new FormData();
    form.append('files', fs.createReadStream("../data/teste.txt"));
    requestParams.body = form;
    return next();
}


function saveShortId(requestParams, response, context, ee, next) {
    console.log(JSON.parse(response.body).shortId);
    context.shortId = response.blobUrl;
    return next();
}

function saveBlobUrl(requestParams, response, context, ee, next) {
    //console.log(response.body);
    var res = JSON.parse(response.body);
    context.vars.blobUrl = res.blobUrl;
    context.vars.shortId = res.shortId;
    return next();
}


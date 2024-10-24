package tukano.impl;

import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import utils.Hash;

public class Token {
	private static Logger Log = Logger.getLogger(Token.class.getName());

	public static final long MAX_TOKEN_AGE = 1000000;
	private static Algorithm algorithm;

	public static void setSecret(String s) {
		algorithm = Algorithm.HMAC256(s);
	}

	public enum Service {
		AUTH,
		BLOBS
	}
	
	public static String get(Service service, String id) {
		try {
			String token = JWT.create()
					.withIssuer(service.toString())
					.withIssuedAt(Date.from(Instant.now()))
					.withSubject(id)
					.sign(algorithm);
			return token;
		} catch (JWTCreationException exception){
			return null;
			// Invalid Signing configuration / Couldn't convert Claims.
		}
	}

	public static String getSubject(String tokenStr) {
		return JWT.decode(tokenStr).getSubject();
	}

	public static boolean isValid(String tokenStr, Service service, String id) {
		DecodedJWT decodedJWT;
		try {
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(service.toString())
					.build();
			decodedJWT = verifier.verify(tokenStr);
			if (!decodedJWT.getSubject().equals(id) || // wrong id
					(decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli())) // is over
				return false;

		} catch (JWTVerificationException exception){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

	public static boolean isValid(String tokenStr, Service service) {
		DecodedJWT decodedJWT;
		try {
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(service.toString())
					.build();
			decodedJWT = verifier.verify(tokenStr);
			if (decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli()) // is over
				return false;

		} catch (JWTVerificationException exception){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

}

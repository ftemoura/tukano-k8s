package tukano.impl;

import java.time.Instant;
import java.util.Date;
import java.util.List;
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
		AUTH, // to be used for user authentication/authorization
		BLOBS, // to be used for blobs
		INTERNAL // to be used between internal services
	}

	public enum Role{
		USER(0),
		ADMIN(1);

		private int level;

		Role(int level) {
			this.level = level;
		}

		public int getLevel() {
			return this.level;
		}

	}
	
	public static String get(Service service, String id, Role role) {
		try {
			String token = JWT.create()
					.withIssuer(service.toString())
					.withIssuedAt(Date.from(Instant.now()))
					.withClaim("role", role.toString())
					.withSubject(id)
					.sign(algorithm);
			return token;
		} catch (JWTCreationException exception){
			return null;
			// Invalid Signing configuration / Couldn't convert Claims.
		}
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

	private static  DecodedJWT decode_token(String tokenStr, Service service) {
		JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer(service.toString())
				.build();
		return verifier.verify(tokenStr);
	}
	public static boolean isValid(String tokenStr, Service service, String id, Role role) {
		DecodedJWT decodedJWT;
		try {
			decodedJWT = decode_token(tokenStr, service);
			if (!decodedJWT.getSubject().equals(id) || // wrong id
					(decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli()) || // is over
					(Role.valueOf(decodedJWT.getClaim("role").toString()).getLevel() < role.getLevel()))// wrong role
				return false;

		} catch (JWTVerificationException exception){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

	public static boolean isValid(String tokenStr, Service service, String id) {
		DecodedJWT decodedJWT;
		try {
			decodedJWT = decode_token(tokenStr, service);
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
			decodedJWT = decode_token(tokenStr, service);
			if (decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli()) // is over
				return false;

		} catch (JWTVerificationException exception){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

}

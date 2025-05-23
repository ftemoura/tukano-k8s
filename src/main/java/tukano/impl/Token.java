package tukano.impl;

import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

public class Token {
	private static Logger Log = Logger.getLogger(Token.class.getName());

	public static final long MAX_TOKEN_AGE = 1000000;
	private static Algorithm algorithm;

	public static void setSecret(String s) {
		algorithm = Algorithm.HMAC256(s);
	}

	public enum Service {
		AUTH, // to be used for user authentication/authorization
		BLOBS // to be used for blobs
	}

	// Hierarchical RBAC
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

	public static String getIssuer(String tokenStr) {
		return JWT.decode(tokenStr).getIssuer();
	}

	public static String getClaim(String tokenStr, String claim) {
		return String.valueOf(JWT.decode(tokenStr).getClaim(claim));
	}

	public static boolean isEnoughRoleLevel(String tokenStr, Service service, Role role) {
		try {
			return Role.valueOf(String.valueOf(decodeToken(tokenStr, service).getClaim("role")).replace("\"", "").trim()).getLevel() >= role.getLevel();
		} catch (JWTVerificationException exception){
			return false;
		}
	}

	private static  DecodedJWT decodeToken(String tokenStr, Service service) {
		JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer(service.toString())
				.build();
		return verifier.verify(tokenStr);
	}
	public static boolean isValid(String tokenStr, Service service, String id, Role role) {
		DecodedJWT decodedJWT;
		try {
			decodedJWT = decodeToken(tokenStr, service);
			if (!decodedJWT.getSubject().equals(id) || // wrong id
					(decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli()) || // is over
					(Role.valueOf(String.valueOf(decodedJWT.getClaim("role")).replace("\"", "").trim()).getLevel() < role.getLevel()))// wrong role
				return false;

		} catch (JWTVerificationException exception ){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

	public static boolean isValid(String tokenStr, Service service, String id) {
		DecodedJWT decodedJWT;
		try {
			decodedJWT = decodeToken(tokenStr, service);
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
			decodedJWT = decodeToken(tokenStr, service);
			if (decodedJWT.getIssuedAt().getTime() + MAX_TOKEN_AGE < Instant.now().toEpochMilli()) // is over
				return false;

		} catch (JWTVerificationException exception){
			return false;
			// Invalid signature/claims
		}
		return true;
	}

}

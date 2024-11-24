package tukano.api;

import com.microsoft.azure.functions.HttpStatus;

import java.util.function.Function;

import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;

/**
 * 
 * Represents the result of an operation, either wrapping a result of the given type,
 * or an error.
 * 
 * @author smd
 *
 * @param <T> type of the result value associated with success
 */
public interface Result<T> {

	/**
	 * 
	 * @author smd
	 *
	 * Service errors:
	 * OK - no error, implies a non-null result of type T, except for for Void operations
	 * CONFLICT - something is being created but already exists
	 * NOT_FOUND - an access occurred to something that does not exist
	 * INTERNAL_ERROR - something unexpected happened
	 */
	enum ErrorCode{ OK, CONFLICT, NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR, NOT_IMPLEMENTED, TIMEOUT, PRECONDITION_FAILED};
	
	/**
	 * Tests if the result is an error.
	 */
	boolean isOK();
	
	/**
	 * obtains the payload value of this result
	 * @return the value of this result.
	 */
	T value();

	/**
	 *
	 * obtains the error code of this result
	 * @return the error code
	 * 
	 */
	ErrorCode error();
	
	/**
	 * Convenience method for returning non error results of the given type
	 * @param Class of value of the result
	 * @return the value of the result
	 */
	static <T> Result<T> ok( T result ) {
		return new OkResult<>(result);
	}

	/**
	 * Convenience method for returning non error results without a value
	 * @return non-error result
	 */
	static <T> Result<T> ok() {
		return new OkResult<>(null);	
	}
	
	/**
	 * Convenience method used to return an error 
	 * @return
	 */
	static <T> Result<T> error(ErrorCode error) {
		return new ErrorResult<>(error);		
	}
	
	
	static <T> Result<T> errorOrValue( Result<?> res,  T val) {
		if( res.isOK() )
			return ok( val );
		else
			return error( res.error() );
	}
	
	static <T> Result<T> errorOrValue( Result<?> res,  Result<T> other) {
		if( res.isOK() )			
			return other;
		else
			return error( res.error() );
	}
	
	static Result<Void> errorOrVoid( Result<?> res,  Result<?> other) {
		if( res.isOK() )			
			return other.isOK() ? ok() : error( other.error() );
		else
			return error( res.error() );
	}
	
	static <T, Q> Result<Void> errorOrVoid( Result<T> res,  Function<T, Result<Q>> b) {
		if( ! res.isOK() )
			return error( res.error() );
		
		var bres = b.apply(res.value());
		return bres.isOK() ? ok() : error( bres.error() );
	}
	
	static <T, Q> Result<Q> errorOrResult( Result<T> a, Function<T, Result<Q>> b) {
		if( a.isOK())
			return b.apply(a.value());
		else
			return error( a.error() );
	}
	
	static <T,Q> Result<Q> errorOrValue( Result<T> a, Function<T, Q> b) {
		if( a.isOK())
			return ok(b.apply(a.value()));
		else
			return error( a.error() );
	}

	static Result.ErrorCode errorCodeFromStatus(int status) {
		return switch (status) {
			case 200, 204 -> ErrorCode.OK;
			case 409 -> ErrorCode.CONFLICT;
			case 403 -> ErrorCode.FORBIDDEN;
			case 404 -> ErrorCode.NOT_FOUND;
			case 400 -> ErrorCode.BAD_REQUEST;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}

	static HttpStatus statusFromErrorCode(ErrorCode error) {
		return switch (error) {
			case OK -> HttpStatus.OK;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case CONFLICT -> HttpStatus.CONFLICT;
			case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
			case TIMEOUT -> HttpStatus.REQUEST_TIMEOUT;
			default -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}
}

/*
 * 
 */
class OkResult<T> implements Result<T> {

	final T result;
	
	OkResult(T result) {
		this.result = result;
	}
	
	@Override
	public boolean isOK() {
		return true;
	}

	@Override
	public T value() {
		return result;
	}

	@Override
	public ErrorCode error() {
		return ErrorCode.OK;
	}
	
	public String toString() {
		return "(OK, " + value() + ")";
	}
}

class ErrorResult<T> implements Result<T> {

	final ErrorCode error;
	
	ErrorResult(ErrorCode error) {
		this.error = error;
	}
	
	@Override
	public boolean isOK() {
		return false;
	}

	@Override
	public T value() {
		throw new RuntimeException("Attempting to extract the value of an Error: " + error());
	}

	@Override
	public ErrorCode error() {
		return error;
	}
	
	public String toString() {
		return "(" + error() + ")";		
	}
}

package tukano.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserDAO extends User {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String _etag;

    public String get_etag() {
        return _etag;
    }
    public void set_etag(String _etag) {
        this._etag = _etag;
    }

    @Override
    public String toString() {
        return "UserDAO [_etag=" + _etag + ", user=" + super.toString();
    }
}

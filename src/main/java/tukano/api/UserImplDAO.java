package tukano.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserImplDAO extends UserImpl {

    private String _ts;
    private String _etag;

    public UserImplDAO() {
    }
    public UserImplDAO(String _ts, String _etag) {
        this._ts = _ts;
        this._etag = _etag;
    }

    @Override
    public LocalDateTime getLastModified() {
        Instant instant = Instant.ofEpochSecond(Long.parseLong(_ts));
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /*
    public LocalDateTime get_ts() {
        Instant instant = Instant.ofEpochSecond(Long.parseLong(_ts));
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }


    public void set_ts(String _ts) {
        this._ts = _ts;
    }


    public String get_etag() {
        return _etag;
    }


    public void set_etag(String _etag) {
        this._etag = _etag;
    }*/

    @Override
    public String toString() {
        return "UserDAO [_ts=" + _ts + ", "+ _etag+ ", user=" + super.toString();
    }
}

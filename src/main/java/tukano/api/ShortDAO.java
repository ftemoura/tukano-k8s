package tukano.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShortDAO extends Short{
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
        return "ShortDAO [_etag=" + _etag + ", short=" + super.toString();
    }
}
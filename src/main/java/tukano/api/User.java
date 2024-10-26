package tukano.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
@JsonDeserialize(as = UserImpl.class)
public interface User {
    String getUserId();

    void setUserId(String userId);

    String getPwd();

    void setPwd(String pwd);

    String getEmail();

    void setEmail(String email);

    String getDisplayName();

    void setDisplayName(String displayName);

    String userId();

    String pwd();

    String email();

    String displayName();

    LocalDateTime getLastModified();

    void setLastModified(LocalDateTime t);

    User copyWithoutPassword();

    User updateFrom(User other);
}

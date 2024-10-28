package tukano.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Entity
public class User {
	@UpdateTimestamp
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String _ts;

	@Id
	@JsonProperty("id")
	private String userId;
	private String pwd;
	private String email;	
	private String displayName;


	public User() {}
	
	public User(String userId, String pwd, String email, String displayName) {
		this.pwd = pwd;
		this.email = email;
		this.userId = userId;
		this.displayName = displayName;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPwd() {
		return pwd;
	}
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String pwd() {
		return pwd;
	}
	public String email() {
		return email;
	}
	public String displayName() {
		return displayName;
	}
	public String get_ts() {
		return _ts;
	}
	public void set_ts(String ts) {
		_ts = ts;
	}
	@JsonIgnore
	public LocalDateTime getLastModified() {
		if (_ts == null) return null;

		try {
			// Check if _ts is a numeric string representing seconds since epoch
			if (_ts.matches("\\d+")) {
				Instant instant = Instant.ofEpochSecond(Long.parseLong(_ts));
				return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			} else {
				// Use a custom formatter that matches 'yyyy-MM-dd HH:mm:ss.SSSSSS'
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
				return LocalDateTime.parse(_ts, formatter);
			}
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid format for _ts: " + _ts, e);
		}
	}
	@Override
	public String toString() {
		return "User [ts="+_ts +", userId=" + userId + ", pwd=" + pwd + ", email=" + email + ", displayName=" + displayName + "]";
	}
	public User copyWithoutPassword() {
		return new User(userId, "", email, displayName);
	}

	public User updateFrom(User other) {
		return new User( userId,
				other.pwd() != null ? other.pwd() : pwd,
				other.email() != null ? other.email() : email,
				other.displayName() != null ? other.displayName() : displayName);
	}
}

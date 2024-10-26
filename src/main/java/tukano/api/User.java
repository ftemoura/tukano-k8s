package tukano.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	private String _ts;

	@Id
	private String id;
	private String pwd;
	private String email;	
	private String displayName;


	public User() {}
	
	public User(String id, String pwd, String email, String displayName) {
		this.pwd = pwd;
		this.email = email;
		this.id = id;
		this.displayName = displayName;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
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
		return "User [ts="+_ts +"userId=" + id + ", pwd=" + pwd + ", email=" + email + ", displayName=" + displayName + "]";
	}
	public User copyWithoutPassword() {
		return new User(id, "", email, displayName);
	}

	public User updateFrom(User other) {
		return new User( id,
				other.pwd() != null ? other.pwd() : pwd,
				other.email() != null ? other.email() : email,
				other.displayName() != null ? other.displayName() : displayName);
	}
}

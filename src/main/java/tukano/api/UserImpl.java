package tukano.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
public class UserImpl implements User {

	@UpdateTimestamp
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime lastModified;
	
	@Id
	private String userId;
	private String pwd;
	private String email;	
	private String displayName;

	public UserImpl() {}
	
	public UserImpl(String userId, String pwd, String email, String displayName) {
		this.pwd = pwd;
		this.email = email;
		this.userId = userId;
		this.displayName = displayName;
	}

	@Override
	public String getUserId() {
		return userId;
	}
	@Override
	public void setUserId(String userId) {
		this.userId = userId;
	}
	@Override
	public String getPwd() {
		return pwd;
	}
	@Override
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	@Override
	public String getEmail() {
		return email;
	}
	@Override
	public void setEmail(String email) {
		this.email = email;
	}
	@Override
	public String getDisplayName() {
		return displayName;
	}
	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	@Override
	public String userId() {
		return userId;
	}
	
	@Override
	public String pwd() {
		return pwd;
	}
	
	@Override
	public String email() {
		return email;
	}
	
	@Override
	public String displayName() {
		return displayName;
	}

	@Override
	@JsonIgnore
	public LocalDateTime getLastModified() {
		return lastModified;
	}

	@Override
	public void setLastModified(LocalDateTime t) {
		this.lastModified = t;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", pwd=" + pwd + ", email=" + email + ", displayName=" + displayName + "]";
	}
	
	@Override
	public User copyWithoutPassword() {
		return new UserImpl(userId, "", email, displayName);
	}
	
	@Override
	public User updateFrom(User other) {
		return new UserImpl( userId,
				other.pwd() != null ? other.pwd() : pwd,
				other.email() != null ? other.email() : email,
				other.displayName() != null ? other.displayName() : displayName);
	}
}

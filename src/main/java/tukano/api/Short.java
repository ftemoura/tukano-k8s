package tukano.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.UpdateTimestamp;
import tukano.impl.Token;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Represents a Short video uploaded by an user.
 * 
 * A short has an unique shortId and is owned by a given user; 
 * Comprises of a short video, stored as a binary blob at some bloburl;.
 * A post also has a number of likes, which can increase or decrease over time. It is the only piece of information that is mutable.
 * A short is timestamped when it is created.
 *
 */
@Entity
public class Short {

	@UpdateTimestamp
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String _ts;

	@Id
	@JsonProperty("id")
	String shortId;
	String ownerId;
	String blobUrl;
	long timestamp;
	long views;
	int totalLikes;

	public Short() {}
	
	public Short(String shortId, String ownerId, String blobUrl, long timestamp, int totalLikes, long views) {
		super();
		this.shortId = shortId;
		this.ownerId = ownerId;
		this.blobUrl = blobUrl;
		this.timestamp = timestamp;
		this.totalLikes = totalLikes;
		this.views = views;
	}

	public Short(String shortId, String ownerId, String blobUrl) {
		this( shortId, ownerId, blobUrl, System.currentTimeMillis(), 0, 0);
	}
	
	public String getShortId() {
		return shortId;
	}

	public void setShortId(String shortId) {
		this.shortId = shortId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getBlobUrl() {
		return blobUrl;
	}

	public void setBlobUrl(String blobUrl) {
		this.blobUrl = blobUrl;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getTotalLikes() {
		return totalLikes;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
	}
	public void setViews(long views) {
		this.views = views;
	}
	public long getViews() {
		return views;
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
	public void setLastModified(String t) {
		this._ts = t;
	}
	@Override
	public String toString() {
		return "Short [shortId=" + shortId + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
				+ timestamp + ", totalLikes=" + totalLikes + ", " + _ts +"]";
	}
	
	public Short copyWithLikes_And_Token( long totLikes) { // TODO token fix (maybe not working)
		var urlWithToken = String.format("%s?token=%s", blobUrl, Token.get(Token.Service.BLOBS, shortId));
		Short shr = new Short( shortId, ownerId, urlWithToken, timestamp, (int)totLikes, views);
		shr.setLastModified(_ts);
		return shr;
	}	
}
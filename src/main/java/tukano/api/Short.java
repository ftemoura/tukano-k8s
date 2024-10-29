package tukano.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.UpdateTimestamp;
import tukano.impl.Token;

import java.time.LocalDateTime;

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
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonIgnore
	private LocalDateTime lastModified;
	@Id
	@JsonProperty("id")
	String shortId;
	String ownerId;
	String blobUrl;
	long timestamp;
	int totalLikes;

	public Short() {}
	
	public Short(String shortId, String ownerId, String blobUrl, long timestamp, int totalLikes) {
		super();
		this.shortId = shortId;
		this.ownerId = ownerId;
		this.blobUrl = blobUrl;
		this.timestamp = timestamp;
		this.totalLikes = totalLikes;
	}

	public Short(String shortId, String ownerId, String blobUrl) {
		this( shortId, ownerId, blobUrl, System.currentTimeMillis(), 0);
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

	public LocalDateTime getLastModified() {
		return lastModified;
	}
	public void setLastModified(LocalDateTime t) {
		this.lastModified = t;
	}
	@Override
	public String toString() {
		return "Short [shortId=" + shortId + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
				+ timestamp + ", totalLikes=" + totalLikes + ", " + lastModified +"]";
	}
	
	public Short copyWithLikes_And_Token( long totLikes) { // TODO token fix (maybe not working)
		var urlWithToken = String.format("%s?token=%s", blobUrl, Token.get(Token.Service.BLOBS, blobUrl));
		Short shr = new Short( shortId, ownerId, urlWithToken, timestamp, (int)totLikes);
		shr.setLastModified(lastModified);
		return shr;
	}	
}
package org.cecatto.urlshortener.persistence;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class StoredUrl {

  @Id
  private String longUrl;
  private String hash;
  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public StoredUrl() {

  }

  public StoredUrl(String longUrl, String hash) {
    this.longUrl = longUrl;
    this.hash = hash;
  }

  public String getLongUrl() {
    return longUrl;
  }

  public String getHash() {
    return hash;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  @Override
  public String toString() {
    return longUrl + " - " + hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StoredUrl storedUrl = (StoredUrl) o;
    return longUrl.equals(storedUrl.longUrl) && hash.equals(storedUrl.hash) && Objects.equals(createdAt, storedUrl.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(longUrl, hash, createdAt);
  }

}

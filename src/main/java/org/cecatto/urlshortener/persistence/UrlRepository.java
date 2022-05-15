package org.cecatto.urlshortener.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<StoredUrl, String> {
}

package org.cecatto.urlshortener.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<StoredUrl, String> {

  Optional<StoredUrl> findByHash(String hash);

}

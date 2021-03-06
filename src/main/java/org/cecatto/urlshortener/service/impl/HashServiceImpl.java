package org.cecatto.urlshortener.service.impl;

import org.cecatto.urlshortener.persistence.StoredUrl;
import org.cecatto.urlshortener.persistence.UrlRepository;
import org.cecatto.urlshortener.service.HashService;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Component
public class HashServiceImpl implements HashService {

  private static final Logger log = LoggerFactory.getLogger(HashServiceImpl.class);

  private final UrlRepository urlRepository;

  @Autowired
  public HashServiceImpl(UrlRepository urlRepository) {
    this.urlRepository = urlRepository;
  }

  @Override
  public String hashUrl(URI longUrl) {
    var uriToSave = longUrl;

    if (!StringUtils.hasText(longUrl.getScheme())) {
      // default scheme to HTTP if nothing was provided
      log.info("Provided URL is missing the scheme, using 'http://'");
      uriToSave = URI.create("http://" + longUrl);
    }

    var hash = buildHash(uriToSave);

    return storeUrl(uriToSave, hash);
  }

  @Override
  public Optional<URI> lookup(String hash) {
    var maybeUrl = urlRepository.findByHash(hash);
    return maybeUrl.map(storedUrl -> URI.create(storedUrl.getLongUrl()));
  }

  private String buildHash(URI longUrl) {
    return buildHash(longUrl, "");
  }

  private String buildHash(URI longUrl, String salt) {
    return UUID.nameUUIDFromBytes((salt + longUrl.toString()).getBytes()).toString().substring(0, 8);
  }

  private String storeUrl(URI longUrl, String hash) {
    try {
      var storedUrl = urlRepository.save(new StoredUrl(longUrl.toString(), hash));
      log.info("Stored " + storedUrl);
      return storedUrl.getHash();
    } catch (DataIntegrityViolationException e) {
      if (e.getRootCause() instanceof PSQLException) {
        var psqlException = (PSQLException) e.getRootCause();
        if (PSQLState.UNIQUE_VIOLATION.getState().equals(psqlException.getSQLState())) {
          var maybeUrl = lookup(hash);
          if (maybeUrl.isPresent()) {
            var existingUrl = maybeUrl.get();
            if (existingUrl.equals(longUrl)) {
              // it means the url was already added to the database in the meantime, nothing to do
              return hash;
            } else {
              log.warn("Conflicted hash {} for url {} when storing it, found url {}. Retrying...", hash, longUrl, existingUrl);
              // retry with a salted hash (using the previous hash as salt)
              return storeUrl(longUrl, buildHash(longUrl, hash));
            }
          }
        }
      }
      throw e;
    }
  }
}

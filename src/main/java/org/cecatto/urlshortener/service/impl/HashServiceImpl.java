package org.cecatto.urlshortener.service.impl;

import org.cecatto.urlshortener.persistence.StoredUrl;
import org.cecatto.urlshortener.persistence.UrlRepository;
import org.cecatto.urlshortener.service.HashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
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
    var storedUrl = urlRepository.save(new StoredUrl(uriToSave.toString(), hash));
    log.info("Stored " + storedUrl);

    return hash;
  }

  @Override
  public URI lookup(String hash) {
    return null;
  }

  private String buildHash(URI longUrl) {
    return UUID.nameUUIDFromBytes(longUrl.toString().getBytes()).toString().substring(0, 8);
  }

}

package org.cecatto.urlshortener.service.impl;

import org.cecatto.urlshortener.service.HashService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.UUID;

@Component
public class HashServiceImpl implements HashService {

  @Override
  public String hashUrl(URI longUrl) {
    var uriToSave = longUrl;
    if (!StringUtils.hasText(longUrl.getScheme())) {
      // default scheme to HTTP if nothing was provided
      uriToSave = URI.create("http://" + longUrl.toString());
    }
    var shortUrl = buildHash(longUrl);
    return shortUrl;
  }

  @Override
  public URI lookup(String hash) {
    return null;
  }

  private String buildHash(URI longUrl) {
    return UUID.nameUUIDFromBytes(longUrl.toString().getBytes()).toString().substring(0, 8);
  }

}

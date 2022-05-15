package org.cecatto.urlshortener.service;

import java.net.URI;

public interface HashService {

  String hashUrl(URI longUrl);

  URI lookup(String hash);

}

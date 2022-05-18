package org.cecatto.urlshortener.service;

import java.net.URI;
import java.util.Optional;

public interface HashService {

  String hashUrl(URI longUrl);

  Optional<URI> lookup(String hash);

}

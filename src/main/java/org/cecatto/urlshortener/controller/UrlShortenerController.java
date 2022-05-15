package org.cecatto.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/")
@Validated
public class UrlShortenerController {

  public static final String PARAM_LONG_URL = "long_url";
  public static final String PARAM_HASH = "hash";
  public static final String PATH_LOOKUP = "/s/{" + PARAM_HASH + "}";
  public static final String PATH_CREATE_V1 = "/v1/create/{" + PARAM_LONG_URL + "}";

  @PutMapping(PATH_CREATE_V1)
  public ResponseEntity<Void> createV1(@PathVariable(PARAM_LONG_URL) URI longUrl) {
    var hash = "12345";
    var requestUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build();
    var scheme = requestUri.getScheme();
    var host = requestUri.getHost();
    return ResponseEntity.created(URI.create(scheme + "://" + host + PATH_LOOKUP + hash)).build();
  }

  @GetMapping(PATH_LOOKUP)
  public ResponseEntity<Void> lookup(@PathVariable(PARAM_HASH) String hash) {
    var longUrl = "http://www.example.com";
    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(longUrl)).build();
  }

}

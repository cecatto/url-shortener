package org.cecatto.urlshortener.controller;

import org.cecatto.urlshortener.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import java.net.URI;

@RestController
@RequestMapping("/")
@Validated
public class UrlShortenerController {

  public static final String PARAM_URL = "url";
  public static final String PARAM_HASH = "hash";
  public static final String PATH_LOOKUP = "/s/{" + PARAM_HASH + "}";
  public static final String PATH_CREATE_V1 = "/v1/create";

  private final HashService hashService;

  @Autowired
  public UrlShortenerController(HashService hashService) {
    this.hashService = hashService;
  }

  @PostMapping(PATH_CREATE_V1)
  public ResponseEntity<Void> createV1(@RequestParam(PARAM_URL) URI longUrl) {
    if (longUrl == null || !StringUtils.hasText(longUrl.toString())) {
      throw new IllegalArgumentException("url cannot be null or empty");
    }

    var hash = hashService.hashUrl(longUrl);
    var requestUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build();
    var location = new UriTemplate(requestUri.getScheme() + "://" + requestUri.toUri().getAuthority() + PATH_LOOKUP).expand(hash);
    return ResponseEntity.created(location).build();
  }

  @GetMapping(PATH_LOOKUP)
  public ResponseEntity<Void> lookup(@PathVariable(PARAM_HASH) String hash) {
    var longUrl = hashService.lookup(hash);

    if (longUrl == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(longUrl).build();
  }

}

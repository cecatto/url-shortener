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

  // ideally, this should come from configuration
  public static final String HASH_REGEX = "[a-z0-9-]{8,20}";
  public static final String PARAM_URL = "url";
  public static final String PARAM_HASH = "hash";
  public static final String PATH_LOOKUP = "/s/{" + PARAM_HASH + "}";
  public static final String PATH_LOOKUP_VALIDATED = "/s/{" + PARAM_HASH + ":" + HASH_REGEX + "}";
  public static final String PATH_CREATE_V1 = "/v1/create";

  private final HashService hashService;

  @Autowired
  public UrlShortenerController(HashService hashService) {
    this.hashService = hashService;
  }

  @PostMapping(PATH_CREATE_V1)
  public ResponseEntity<Void> createV1(@RequestParam(PARAM_URL) URI longUrl) {
    checkEmpty(longUrl, PARAM_URL);

    var hash = hashService.hashUrl(longUrl);
    var requestUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build();
    var location = new UriTemplate(requestUri.getScheme() + "://" + requestUri.toUri().getAuthority() + PATH_LOOKUP).expand(hash);
    return ResponseEntity.created(location).build();
  }

  @GetMapping(PATH_LOOKUP_VALIDATED)
  public ResponseEntity<Void> lookup(@PathVariable(PARAM_HASH) String hash) {
    checkEmpty(hash, PARAM_HASH);

    var longUrl = hashService.lookup(hash);

    if (longUrl.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(longUrl.get()).build();
  }

  private void checkEmpty(Object param, String paramName) {
    if (param == null || !StringUtils.hasText(param.toString())) {
      throw new IllegalArgumentException(paramName + " cannot be null or empty");
    }
  }

}

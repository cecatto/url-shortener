package org.cecatto.urlshortener.controller;

import org.cecatto.urlshortener.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionsHandler {

  private static final Logger log = LoggerFactory.getLogger(ExceptionsHandler.class);

  @ExceptionHandler({
      IllegalArgumentException.class,
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(Exception e) {
    return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> defaultHandler(Exception e) {
    var msg = "Some unexpected error happened";
    log.error(msg, e);
    return ResponseEntity.internalServerError().body(new ApiError(msg));
  }

}

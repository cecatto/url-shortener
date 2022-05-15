package org.cecatto.urlshortener.dto;

public class ApiError {

  public String errorMessage;

  public ApiError(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}

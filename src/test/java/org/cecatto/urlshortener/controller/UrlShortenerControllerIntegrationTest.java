package org.cecatto.urlshortener.controller;

import org.cecatto.urlshortener.dto.ApiError;
import org.cecatto.urlshortener.persistence.StoredUrl;
import org.cecatto.urlshortener.persistence.UrlRepository;
import org.cecatto.urlshortener.service.HashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("integration-tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UrlShortenerControllerIntegrationTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @SpyBean
  private HashService hashService;

  @Autowired
  private UrlRepository urlRepository;

  @BeforeEach
  public void setUp() {
    testRestTemplate.getRestTemplate().setRequestFactory(new NoRedirectSimpleClientHttpRequestFactory());
  }

  @AfterEach
  public void tearDown() {
    urlRepository.deleteAll();
  }

  @Test
  public void testCreateSuccessful() {
    var longUrl = "http://www.google.com";
    var expectedHash = "ed646a33";
    var response = makeCreateRequest(longUrl, Void.class);

    Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var expectedLocation = new UriTemplate(testRestTemplate.getRootUri() + UrlShortenerController.PATH_LOOKUP).expand(expectedHash);
    Assertions.assertEquals(expectedLocation, response.getHeaders().getLocation());

    var optionalStoredUrl = urlRepository.findById(longUrl);
    Assertions.assertTrue(optionalStoredUrl.isPresent());

    var storedUrl = optionalStoredUrl.get();
    Assertions.assertEquals(longUrl, storedUrl.getLongUrl());
    Assertions.assertEquals(expectedHash, storedUrl.getHash());
    Assertions.assertNotNull(storedUrl.getCreatedAt());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testCreateMissingValueIsRejected(String emptyUrl) {
    var response = makeCreateRequest(emptyUrl, ApiError.class);
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    var apiError = response.getBody();
    Assertions.assertNotNull(apiError);
    Assertions.assertTrue(apiError.errorMessage.startsWith(UrlShortenerController.PARAM_URL));
  }

  @ParameterizedTest
  @ValueSource(strings = {"www.example|.com", "http://www.ex[ample.com"})
  public void testCreateInvalidHashIsRejected(String invalidHash) {
    var response = makeCreateRequest(invalidHash, ApiError.class);
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    var apiError = response.getBody();
    Assertions.assertNotNull(apiError);
    Assertions.assertTrue(apiError.errorMessage.contains("Invalid URI syntax"));
  }

  @Test
  public void testCreateConcurrentlyMultipleUrls() throws Exception {
    makeConcurrentCreateRequests(null);
  }

  @Test
  public void testCreateConcurrentlySameUrl() throws Exception {
    makeConcurrentCreateRequests("https://www.google.com");
  }

  @Test
  public void testLookupSuccessful() {
    var longUrl = "http://www.google.com";
    var hash = "ed646a33";

    urlRepository.save(new StoredUrl(longUrl, hash));

    var response = testRestTemplate.getForEntity(UrlShortenerController.PATH_LOOKUP, Void.class, hash);

    Assertions.assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
    Assertions.assertNotNull(response.getHeaders().getLocation());
    Assertions.assertEquals(longUrl, response.getHeaders().getLocation().toString());
  }

  @Test
  public void testLookupNotFound() {
    var hash = "12345678";
    var response = testRestTemplate.getForEntity(UrlShortenerController.PATH_LOOKUP, Void.class, hash);

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Assertions.assertNull(response.getHeaders().getLocation());
    Mockito.verify(hashService, Mockito.times(1)).lookup(hash);
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testLookupMissingHashValueNotProcessed(String emptyHash) {
    var response = testRestTemplate.getForEntity(UrlShortenerController.PATH_LOOKUP, Void.class, emptyHash);

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Mockito.verify(hashService, Mockito.never()).lookup(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"short", "veeeeeeeeeeeerylooooooooooooong", "12345678@|"})
  public void testLookupInvalidHashIsNotProcessed(String invalidHash) {
    var response = testRestTemplate.getForEntity(UrlShortenerController.PATH_LOOKUP, Void.class, invalidHash);

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Mockito.verify(hashService, Mockito.never()).lookup(any());
  }

  private <T> ResponseEntity<T> makeCreateRequest(String longUrl, Class<T> expectedResponseClass) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    var formMap = new LinkedMultiValueMap<String, String>();
    // List.of() does not allow null values
    var valueList = new ArrayList<String>();
    valueList.add(longUrl);
    formMap.put(UrlShortenerController.PARAM_URL, valueList);

    return testRestTemplate.postForEntity(UrlShortenerController.PATH_CREATE_V1,
        new HttpEntity<>(formMap, headers),
        expectedResponseClass);
  }

  private void makeConcurrentCreateRequests(String sameUrl) throws InterruptedException {
    var numRequests = 10;
    var finishedLatch = new CountDownLatch(numRequests);
    var greenSignalLatch = new CountDownLatch(1);

    for (int i = 0; i < numRequests; i++) {
      final String longUrl = Optional.ofNullable(sameUrl).orElse("http://www.example.com/" + i);
      new Thread(() -> {
        try {
          greenSignalLatch.await();
        } catch (InterruptedException e) {
          Assertions.fail("Couldn't wait for green signal", e);
        }
        var response = makeCreateRequest(longUrl, Void.class);
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        finishedLatch.countDown();
      }).start();
    }

    greenSignalLatch.countDown();
    Assertions.assertTrue(finishedLatch.await(5000L, TimeUnit.MILLISECONDS), "Did not finish all requests in time");
  }

  private static class NoRedirectSimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
      super.prepareConnection(connection, httpMethod);
      connection.setInstanceFollowRedirects(false);
    }

  }

}

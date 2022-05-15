package org.cecatto.urlshortener.service;

import org.cecatto.urlshortener.persistence.StoredUrl;
import org.cecatto.urlshortener.persistence.UrlRepository;
import org.cecatto.urlshortener.service.impl.HashServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public class HashServiceImplTest {

  private UrlRepository mockedUrlRepository;
  private HashServiceImpl hashService;

  @BeforeEach
  public void setUp() {
    mockedUrlRepository = Mockito.mock(UrlRepository.class);
    hashService = new HashServiceImpl(mockedUrlRepository);
  }

  private static Stream<Arguments> validCasesForCreate() {
    return Stream.of(
        Arguments.of("http://www.example.com", "847310eb"),
        Arguments.of("https://www.example.com", "e149be13"),
        Arguments.of("https://www.linkedin.com/in/lcecatto/", "b0176ed1"),
        Arguments.of("https://*www.example.com", "e973bec8")
    );
  }

  @ParameterizedTest
  @MethodSource("validCasesForCreate")
  public void testCreateWithValidCases(String longUrl, String expectedHash) {
    var uri = URI.create(longUrl);
    var actualHash = hashService.hashUrl(uri);
    Assertions.assertEquals(expectedHash, actualHash);
    Mockito.verify(mockedUrlRepository, Mockito.times(1)).save(new StoredUrl(longUrl, expectedHash));
  }

  @Test
  public void testCreateWithMissingScheme() {
    var uri = URI.create("www.example.com");
    var expectedHash = "847310eb";
    var actualHash = hashService.hashUrl(uri);
    Assertions.assertEquals(expectedHash, actualHash);
    Mockito.verify(mockedUrlRepository, Mockito.times(1)).save(new StoredUrl("http://www.example.com", expectedHash));
  }

  @Test
  public void testLookupIsSuccessful() {
    var hash = "the_hash_value";
    var mockedUri = URI.create("http://www.example.com");
    Mockito.doReturn(Optional.of(new StoredUrl("http://www.example.com", hash))).when(mockedUrlRepository).findByHash(hash);
    var uri = hashService.lookup(hash);
    Assertions.assertEquals(mockedUri, uri);
  }

  @Test
  public void testLookupReturnsNull() {
    var hash = "the_hash_value";
    Mockito.doReturn(Optional.empty()).when(mockedUrlRepository).findByHash(hash);
    var uri = hashService.lookup(hash);
    Assertions.assertNull(uri);
  }

}

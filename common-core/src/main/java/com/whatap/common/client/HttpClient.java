package com.whatap.common.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpClient {
  private final RestTemplate restTemplate;

  public HttpClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public <T> T get(String url, Class<T> responseType) {
    return restTemplate.exchange(url, HttpMethod.GET, null, responseType).getBody();
  }

  public <T> T post(String url, Object requestBody, Class<T> responseType) {
    return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody), responseType).getBody();
  }
}

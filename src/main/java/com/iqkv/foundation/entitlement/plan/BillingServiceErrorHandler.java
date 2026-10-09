/*
 * Copyright 2026 iQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.entitlement.plan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * Custom error handler for the billing service RestTemplate.
 *
 * <p>Wraps 4xx/5xx responses from the billing service in {@link BillingServiceException}
 * for downstream services to handle appropriately. Extracts error details from the response
 * body when available.
 *
 * <p>Falls back to a generic error message when the response body cannot be read.
 */
public class BillingServiceErrorHandler implements ResponseErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(BillingServiceErrorHandler.class);

  @Override
  public boolean hasError(final ClientHttpResponse response) throws IOException {
    final HttpStatusCode statusCode = response.getStatusCode();
    return statusCode.is4xxClientError() || statusCode.is5xxServerError();
  }

  public void handleError(final ClientHttpResponse response) throws IOException {
    final var httpStatus = HttpStatus.valueOf(response.getStatusCode().value());

    // Try to extract error detail from response body
    String detail;
    try {
      final var body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
      detail = body.isBlank() ? httpStatus.getReasonPhrase() : body;
      log.warn("Billing service returned error: status={}, body={}", httpStatus.value(), body);
    } catch (final Exception e) {
      detail = httpStatus.getReasonPhrase();
      log.warn("Billing service error (failed to read response body): status={}, error={}",
          httpStatus.value(), e.getMessage());
    }

    throw new BillingServiceException(httpStatus, detail);
  }
}

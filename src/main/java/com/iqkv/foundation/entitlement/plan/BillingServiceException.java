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

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the billing service returns an error response with structured ProblemDetails.
 *
 * <p>Preserves the original HTTP status and ProblemDetail properties from the upstream
 * billing service response, allowing downstream services to propagate meaningful error
 * information to their clients rather than falling back silently.
 *
 * <p>Consuming services should catch this exception in {@code @ControllerAdvice} and
 * translate it to an appropriate HTTP 502 Bad Gateway response with the preserved details.
 */
public class BillingServiceException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String detail;
  private final Map<String, Object> properties;

  /**
   * Creates a new exception from an HTTP status and detail message.
   */
  public BillingServiceException(final HttpStatus httpStatus, final String detail) {
    this(httpStatus, detail, Map.of());
  }

  /**
   * Creates a new exception from an HTTP status, detail message, and additional properties.
   */
  public BillingServiceException(final HttpStatus httpStatus,
                                 final String detail,
                                 final Map<String, Object> properties) {
    super("Billing service error [" + httpStatus.value() + "]: " + detail);
    this.httpStatus = httpStatus;
    this.detail = detail;
    this.properties = Map.copyOf(properties);
  }

  /**
   * Creates a new exception from a Spring ProblemDetail.
   */
  public static BillingServiceException fromProblemDetail(final org.springframework.http.ProblemDetail problemDetail) {
    final var status = HttpStatus.valueOf(problemDetail.getStatus());
    final var detail = problemDetail.getDetail() != null ? problemDetail.getDetail() : "Unknown error";
    final var rawProperties = problemDetail.getProperties();
    final Map<String, Object> properties = rawProperties != null
        ? rawProperties.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                e -> String.valueOf(e.getKey()),
                java.util.Map.Entry::getValue))
        : Map.of();
    return new BillingServiceException(status, detail, properties);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getDetail() {
    return detail;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}

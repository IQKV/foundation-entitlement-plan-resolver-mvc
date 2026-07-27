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

/**
 * Thrown when the caller's active subscription plan does not include the requested feature.
 *
 * <p>Maps to HTTP {@code 403 Forbidden} — the caller is authenticated and authorized by role,
 * but their plan does not grant access to this capability. The response body carries
 * {@code featureCode} so the UI can show a targeted upgrade prompt.
 *
 * <p>Consuming services should catch this exception in a {@code @ControllerAdvice} and
 * translate it to the appropriate HTTP response.
 */
public class PlanFeatureNotAvailableException extends RuntimeException {

  private final String featureCode;

  public PlanFeatureNotAvailableException(final String featureCode) {
    super("Feature '" + featureCode + "' is not available on your current plan. Upgrade to access this feature.");
    this.featureCode = featureCode;
  }

  public String getFeatureCode() {
    return featureCode;
  }
}

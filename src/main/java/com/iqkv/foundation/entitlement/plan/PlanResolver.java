/*
 * Copyright 2026 IQKV Foundation Team.
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

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

/**
 * Resolves plan features for a given plan code in non-reactive services.
 *
 * <p>Fetches {@code GET /api/v1/billing/internal/plans} from the billing service at startup
 * and refreshes on a configurable schedule (default: every 10 minutes). The remote data is
 * held in a local in-memory map — no remote call is made at resolve time.
 *
 * <p>Falls back to the last known state when billing is temporarily unreachable — the map
 * is only empty if the service starts while billing is unavailable.
 *
 * <p>Registered automatically via {@link PlanResolverAutoConfiguration}.
 * Override the {@code planResolver} bean in a consuming service to customize behaviour.
 *
 * <p>Usage — inject and call at write time:
 * <pre>
 *   final PlanEntitlement planEntitlement = planResolver.resolveEntitlement(request.getHeader("X-Plan-Code"));
 *   if (planEntitlement.maxProjects() > 0 &amp;&amp; current >= planEntitlement.maxProjects()) {
 *       throw new PlanQuotaExceededException(...);
 *   }
 * </pre>
 */
public class PlanResolver {

  private static final Logger log = LoggerFactory.getLogger(PlanResolver.class);
  private static final String INTERNAL_PLANS_PATH = "/api/v1/billing/internal/plans";

  /**
   * Local DTO for deserializing the billing internal plans response.
   */
  record PlanEntry(String planCode, PlanEntitlement planEntitlement) {
  }

  private volatile Map<String, PlanEntitlement> plans = Map.of();

  private final RestTemplate restTemplate;
  private final String billingServiceUrl;

  public PlanResolver(
      final RestTemplate planResolverRestTemplate,
      @Value("${iqkv.billing.service-url:http://foundation-billing-service}") final String billingServiceUrl) {
    this.restTemplate = planResolverRestTemplate;
    this.billingServiceUrl = billingServiceUrl;
  }

  @PostConstruct
  public void loadOnStartup() {
    refresh();
  }

  /**
   * Refreshes the plan data from the billing service.
   * Runs on a fixed delay configured by {@code iqkv.billing.plan-refresh-interval}.
   */
  @Scheduled(fixedDelayString = "${iqkv.billing.plan-refresh-interval:PT10M}")
  public void refresh() {
    try {
      final ResponseEntity<PlanEntry[]> response = restTemplate.exchange(
          billingServiceUrl + INTERNAL_PLANS_PATH,
          HttpMethod.GET,
          HttpEntity.EMPTY,
          PlanEntry[].class
      );
      final PlanEntry[] entries = response.getBody();
      if (entries != null && entries.length > 0) {
        plans = Arrays.stream(entries)
            .filter(e -> e.planCode() != null && e.features() != null)
            .collect(Collectors.toUnmodifiableMap(PlanEntry::planCode, PlanEntry::features));
        log.info("Plan data refreshed: {} plans loaded", plans.size());
      } else {
        log.warn("Plan data refresh returned empty response — keeping last known state");
      }
    } catch (final Exception e) {
      log.warn("Failed to refresh plan data from billing service, using last known state: {}",
          e.getMessage());
    }
  }

  /**
   * Returns the {@link PlanEntitlement} for the given plan code.
   * Falls back to {@link PlanEntitlement#NONE} when the plan code is unknown or data is unavailable.
   */
  public PlanEntitlement resolveEntitlement(final String planCode) {
    if (planCode == null || planCode.isBlank()) {
      return PlanEntitlement.NONE;
    }
    return plans.getOrDefault(planCode, PlanEntitlement.NONE);
  }
}

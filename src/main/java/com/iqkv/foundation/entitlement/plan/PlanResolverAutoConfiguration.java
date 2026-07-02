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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Boot auto-configuration for the billing plan resolver.
 *
 * <p>Activated automatically when this library is on the classpath — no explicit
 * {@code @Import} or {@code @EnableXxx} needed in the consuming service.
 *
 * <p>Registers three beans, each guarded by {@link ConditionalOnMissingBean} so a
 * consuming service can override any of them:
 * <ul>
 *   <li>{@code entitlementBillingPlanRestTemplate} — bare {@link RestTemplate}; override to add
 *       auth headers, timeouts, or a custom error handler.</li>
 *   <li>{@link PlanResolver} — polls {@code GET /api/v1/billing/internal/plans};
 *       controlled by {@code iqkv.billing.service-url} and
 *       {@code iqkv.billing.plan-refresh-interval}.</li>
 *   <li>{@link PlanFeatureGuard} — stateless guard; override to add custom metrics or
 *       audit logging around feature checks.</li>
 * </ul>
 *
 * <p>Requires {@code @EnableScheduling} — applied here so the consuming service does not
 * need to add it manually. If the consuming service already enables scheduling, Spring
 * deduplicates safely.
 *
 * <p>Configuration properties:
 * <pre>
 * iqkv:
 *   billing:
 *     service-url: http://foundation-billing-service   # default
 *     plan-refresh-interval: PT10M                    # default, ISO-8601 duration
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
@EnableScheduling
@Import(PlanResolverRestTemplateConfig.class)
public class PlanResolverAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public PlanResolver planResolver(
      @Qualifier("entitlementBillingPlanRestTemplate") final RestTemplate entitlementBillingPlanRestTemplate,
      @Value("${iqkv.billing.service-url:http://foundation-billing-service}") final String billingServiceUrl) {
    return new PlanResolver(entitlementBillingPlanRestTemplate, billingServiceUrl);
  }

  @Bean
  @ConditionalOnMissingBean
  public PlanFeatureGuard planFeatureGuard(final PlanResolver planResolver) {
    return new PlanFeatureGuard(planResolver);
  }
}

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

/**
 * Spring Boot auto-configuration library for billing plan entitlement enforcement.
 *
 * <h2>What this library provides</h2>
 * <ul>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanFeature} — DTO for a single feature entry.</li>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanEntitlement} — DTO for the full plan feature set
 *       including typed quota fields ({@code maxUsers}, {@code maxProjects}) and an open feature map.</li>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanFeatureNotAvailableException} — thrown by
 *       {@link com.iqkv.foundation.entitlement.plan.PlanFeatureGuard#require} when the caller's plan
 *       does not include the requested feature. Maps to HTTP {@code 403}.</li>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanResolver} — resolves plan features by plan code;
 *       fetches data from the billing service at startup and on a configurable schedule.</li>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanFeatureGuard} — stateless guard for
 *       feature entitlement checks at the HTTP boundary.</li>
 *   <li>{@link com.iqkv.foundation.entitlement.plan.PlanResolverAutoConfiguration} — Spring Boot
 *       auto-configuration; activated automatically when this jar is on the classpath.</li>
 * </ul>
 *
 * <h2>Configuration properties</h2>
 * <pre>
 * iqkv:
 *   billing:
 *     service-url: http://foundation-billing-service   # URL of the billing service
 *     plan-refresh-interval: PT10M                    # ISO-8601 duration, default 10 minutes
 * </pre>
 *
 * <h2>Overriding beans</h2>
 * <p>All three beans ({@code entitlementBillingPlanRestTemplate}, {@code planResolver},
 * {@code planFeatureGuard}) are guarded by {@code @ConditionalOnMissingBean}.
 * Declare your own {@code @Bean} of the same type or name to take full control.
 */
package com.iqkv.foundation.entitlement.plan;

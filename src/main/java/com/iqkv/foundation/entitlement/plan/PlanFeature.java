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

/**
 * A single named feature entry as returned by the billing service
 * {@code GET /api/v1/billing/internal/plans} endpoint.
 *
 * <p>The feature code is also the map key on {@link PlanEntitlement#features()}; carrying it
 * here keeps each entry self-contained for serialisation and display. Unknown JSON fields
 * are ignored by the Jackson deserializer.
 *
 * @param code        machine-readable identifier, matches the map key
 *                    (e.g. {@code "priority_support"})
 * @param title       human-readable label
 * @param value       feature value as a string ({@code "true"}/{@code "false"} or a number)
 * @param description optional description shown on pricing pages
 */
public record PlanFeature(
    String code,
    String title,
    String value,
    String description
) {
}

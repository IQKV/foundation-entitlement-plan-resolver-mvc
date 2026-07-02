# Foundation Entitlement Plan Resolver MVC 🔐

Spring Boot auto-configuration library that provides an in-memory billing plan catalog cache,
plan feature DTOs, a feature guard, and `PlanFeatureNotAvailableException` for non-reactive
microservices that enforce billing plan entitlements.

## About

This library gives any MVC (servlet-stack) microservice a zero-boilerplate way to answer the
question _"does the caller's plan include feature X?"_ at the HTTP boundary:

- **In-memory plan cache** — `PlanResolver` fetches `GET /api/v1/billing/internal/plans` from the
  billing service at startup and refreshes on a configurable schedule (default: every 10 minutes).
  No remote call is made at check time — all entitlement decisions are resolved from local memory.
- **Graceful degradation** — falls back to the last known state when the billing service is
  temporarily unreachable. The cache is only empty when the service starts while billing is
  completely unavailable.
- **Feature guard** — `PlanFeatureGuard` provides a single authoritative gate with two methods:
  `hasFeature()` for conditional logic and `require()` to assert and throw on failure.
- **Typed quota fields** — `PlanEntitlement` carries `maxUsers` and `maxProjects` as named `int`
  fields for compile-time safety, plus an open `Map<String, PlanFeature>` for extensible boolean
  features that can be added without recompilation.
- **Auto-configured** — zero setup in the consuming service. All beans are registered via Spring
  Boot auto-configuration and each is individually overridable via `@ConditionalOnMissingBean`.

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Tech Stack

- Java 25
- Spring Boot (auto-configuration, `RestTemplate`, `@Scheduled`)
- Maven 3.9+

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.iqkv</groupId>
    <artifactId>foundation-entitlement-plan-resolver-mvc</artifactId>
    <version>0.24.0</version>
</dependency>
```

No `@EnableXxx` or `@Import` annotation is needed. The library registers itself via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Configuration

```yaml
iqkv:
    billing:
        service-url: http://foundation-billing-service # default
        plan-refresh-interval: PT10M # default, ISO-8601 duration
```

| Property                             | Default                             | Description                                             |
| ------------------------------------ | ----------------------------------- | ------------------------------------------------------- |
| `iqkv.billing.service-url`           | `http://foundation-billing-service` | Base URL of the billing service                         |
| `iqkv.billing.plan-refresh-interval` | `PT10M`                             | How often to refresh the plan cache (ISO-8601 duration) |

## Usage

### Feature guard at the controller boundary

Inject `PlanFeatureGuard` and call `require()` before delegating to the service layer.
The `planCode` is sourced from the caller's JWT `plan_code` claim.

```java
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final PlanFeatureGuard planFeatureGuard;
    private final AnalyticsService analyticsService;

    // ...

    @GetMapping("/advanced")
    public ResponseEntity<AnalyticsReport> advancedReport(
            @RequestHeader("X-Plan-Code") String planCode) {
        planFeatureGuard.require(planCode, "advanced_analytics");
        return ResponseEntity.ok(analyticsService.getAdvancedReport());
    }
}
```

Map `PlanFeatureNotAvailableException` to HTTP 403 in a `@ControllerAdvice`:

```java
@ExceptionHandler(PlanFeatureNotAvailableException.class)
public ResponseEntity<ErrorResponse> handlePlanFeatureNotAvailable(
        PlanFeatureNotAvailableException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("PLAN_FEATURE_NOT_AVAILABLE", ex.getMessage(), ex.getFeatureCode()));
}
```

### Quota enforcement at write time

Use `PlanResolver` directly when you need typed quota fields:

```java
@Service
public class ProjectService {

    private final PlanResolver planResolver;

    // ...

    public Project createProject(String planCode, CreateProjectRequest request) {
        final PlanEntitlement entitlement = planResolver.forPlan(planCode);
        if (entitlement.maxProjects() > 0 && currentCount >= entitlement.maxProjects()) {
            throw new PlanQuotaExceededException("projects", entitlement.maxProjects());
        }
        // proceed with creation
    }
}
```

### Conditional display logic

```java
PlanEntitlement entitlement = planResolver.forPlan(planCode);

if (entitlement.has("custom_domain")) {
    // show custom domain settings
}

if (entitlement.isPerSeat()) {
    // show per-seat billing breakdown
}
```

## API Reference

### `PlanResolver`

| Method                     | Description                                                                              |
| -------------------------- | ---------------------------------------------------------------------------------------- |
| `forPlan(String planCode)` | Returns `PlanFeatures` for the plan; falls back to `PlanFeatures.NONE` for unknown codes |
| `refresh()`                | Manually trigger a cache refresh (also runs on the configured schedule)                  |

### `PlanEntitlement`

| Member             | Type                       | Description                                                       |
| ------------------ | -------------------------- | ----------------------------------------------------------------- |
| `maxUsers`         | `int`                      | Max users allowed; `0` means unlimited                            |
| `maxProjects`      | `int`                      | Max projects allowed; `0` means unlimited                         |
| `features`         | `Map<String, PlanFeature>` | Open feature map keyed by feature code                            |
| `pricingModel`     | `String`                   | `"FLAT"` or `"PER_SEAT"`; `null` treated as flat                  |
| `has(String code)` | `boolean`                  | Returns `true` if the feature exists and its value is `"true"`    |
| `isPerSeat()`      | `boolean`                  | Returns `true` when `pricingModel` is `"PER_SEAT"`                |
| `NONE`             | constant                   | Safe fallback with all quotas set to `1` and an empty feature map |

### `PlanFeatureGuard`

| Method                                            | Description                                                                    |
| ------------------------------------------------- | ------------------------------------------------------------------------------ |
| `hasFeature(String planCode, String featureCode)` | Returns `true` if the feature is enabled; never throws                         |
| `require(String planCode, String featureCode)`    | Throws `PlanFeatureNotAvailableException` if the feature is disabled or absent |

### `PlanFeature`

Record carrying a single feature entry: `code`, `title`, `value` (`"true"`/`"false"` or a number), `description`.

### `PlanFeatureNotAvailableException`

`RuntimeException` thrown by `PlanFeatureGuard.require()`. Carries `featureCode` for targeted
UI upgrade prompts. Maps to HTTP `403 Forbidden`.

## Customization

All auto-configured beans are guarded by `@ConditionalOnMissingBean`. Declare your own bean
to override any of them:

```java
// Custom RestTemplate with auth headers and timeouts
@Bean("entitlementBillingPlanRestTemplate")
public RestTemplate entitlementBillingPlanRestTemplate() {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add(new BearerTokenInterceptor(internalApiToken));
    // configure timeouts, error handler, etc.
    return rt;
}

// Custom PlanResolver (e.g. to add metrics)
@Bean
public PlanResolver planResolver(
        @Qualifier("entitlementBillingPlanRestTemplate") RestTemplate rt,
        @Value("${iqkv.billing.service-url}") String url) {
    return new MeteredPlanResolver(rt, url, meterRegistry);
}

// Custom PlanFeatureGuard (e.g. to add audit logging)
@Bean
public PlanFeatureGuard planFeatureGuard(PlanResolver planResolver) {
    return new AuditingPlanFeatureGuard(planResolver, auditService);
}
```

## Publishing to Maven Central

Releases are published automatically via the
[`publish-maven-central`](.github/workflows/publish-maven-central.yml) GitHub Actions workflow,
which is triggered when a GitHub Release is created from the `maven-central` branch.

The workflow:

1. Checks out the source at the release tag
2. Strips the `-SNAPSHOT` suffix from the POM version
3. Runs `mvn clean verify` to confirm the build passes
4. Deploys with `mvn clean deploy -P maven-central`, which attaches sources, Javadoc, and
   GPG-signs all artifacts before uploading to OSSRH (Sonatype Central)

Required GitHub secrets: `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `OSSRH_USERNAME`, `OSSRH_TOKEN`.

## Development

```bash
# Build and install to local Maven repository
./mvnw clean install -Dcheckstyle.skip=true

# Build with checkstyle
./mvnw clean install

# Deploy to local Maven repository only (skip Central upload)
./mvnw clean deploy -DskipRemoteStaging=true
```

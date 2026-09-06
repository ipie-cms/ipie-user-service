package in.gov.ipie.service.user.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import in.gov.ipie.common.i18n.MessageResolver;
import in.gov.ipie.common.security.config.IpieSecurityProperties;
import in.gov.ipie.common.security.config.ResourceServerAutoConfiguration;
import in.gov.ipie.common.security.hmac.HmacSignatureVerificationFilter;
import in.gov.ipie.common.security.hmac.HmacSigningProperties;
import in.gov.ipie.common.security.hmac.NonceStore;
import in.gov.ipie.common.security.ratelimit.RateLimitFilter;
import in.gov.ipie.common.security.ratelimit.RateLimitProperties;
import in.gov.ipie.common.security.ratelimit.RateLimiter;

/**
 * This service's own {@code SecurityFilterChain}, the only way to add a filter on top of the
 * platform baseline (see {@link ResourceServerAutoConfiguration}'s Javadoc) - needed here to
 * protect the public, unauthenticated endpoints in {@code ipie.security.public-paths}
 * (self-registration, email verification, the pillar-link callback) from abuse (this
 * platform's own SRS, NFR-6), and to verify {@link HmacSignatureVerificationFilter} against this
 * service's own internal, service-to-service only endpoint ({@code /internal/logins/notify}, see
 * {@code ipie.security.hmac.protected-paths}) - the same additive defense-in-depth pattern
 * ipie-iam-service's {@code SecurityConfig} already uses for its own internal endpoints. Both
 * filters are fully implemented in {@code ipie-common-libs} already - this class only wires them
 * in, reusing {@link ResourceServerAutoConfiguration#configureBaseline} so every other behaviour
 * (JWT validation, CORS, public-path handling) stays identical to every other service.
 *
 * <p>{@link HmacSigningProperties}/{@link NonceStore}, and likewise {@link ObjectMapper}/{@link
 * MessageResolver} (both only needed so {@link RateLimitFilter} can render its 429 in the
 * platform's standard {@code ApiError} shape - it short-circuits before any controller, so
 * {@code GlobalExceptionHandler} never sees the rejection), are injected via this class's own
 * constructor rather than as {@code @Bean}-method parameters, purely to keep {@link
 * #userServiceSecurityFilterChain}'s own parameter count under Checkstyle's {@code
 * ParameterNumber} limit (max 7) now that it wires two filters instead of one.
 *
 * <p>Guarded by the same {@code ipie.security.enabled} condition {@code
 * ResourceServerAutoConfiguration#ipieSecurityFilterChain} uses - without this, defining any
 * {@code SecurityFilterChain} bean here would unconditionally back off *both* of that class's
 * beans (each is {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}), silently breaking
 * the {@code ipie.security.enabled=false} local-development escape hatch for this service only.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private final HmacSigningProperties hmacSigningProperties;
    private final NonceStore nonceStore;
    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    SecurityConfig(
            HmacSigningProperties hmacSigningProperties,
            NonceStore nonceStore,
            ObjectMapper objectMapper,
            MessageResolver messageResolver) {
        this.hmacSigningProperties = hmacSigningProperties;
        this.nonceStore = nonceStore;
        this.objectMapper = objectMapper;
        this.messageResolver = messageResolver;
    }

    @Bean
    @ConditionalOnProperty(prefix = "ipie.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain userServiceSecurityFilterChain(
            HttpSecurity http,
            IpieSecurityProperties properties,
            @Qualifier("ipieCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            RateLimitProperties rateLimitProperties,
            RateLimiter rateLimiter,
            MeterRegistry meterRegistry)
            throws Exception {
        ResourceServerAutoConfiguration.configureBaseline(http, properties, corsConfigurationSource);
        // Rate limiting runs first (reject abusive traffic cheaply) before the more expensive HMAC
        // signature verification, both ahead of UsernamePasswordAuthenticationFilter.
        http.addFilterBefore(
                new RateLimitFilter(rateLimitProperties, rateLimiter, meterRegistry, objectMapper, messageResolver),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(
                new HmacSignatureVerificationFilter(hmacSigningProperties, nonceStore, meterRegistry), RateLimitFilter.class);
        return http.build();
    }
}

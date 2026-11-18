package vn.specom.hrm.media.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import vn.specom.hrm.security.RemoteAuthorityResolver;
import vn.specom.hrm.security.ResourceServerSecurity;

import java.time.Duration;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * The file content endpoint carries no token: a browser tab or an &lt;img&gt; cannot send one. It is protected by
     * an HMAC signature with an expiry instead, issued only after the permission and company checks.
     */
    private static final String[] PUBLIC = {
            "/api/v1/media/files/*/content",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    RemoteAuthorityResolver authorityResolver(MediaProperties props) {
        return new RemoteAuthorityResolver(props.iamServiceUrl(), Duration.ofSeconds(60));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, RemoteAuthorityResolver resolver, ObjectMapper mapper)
            throws Exception {
        return ResourceServerSecurity.apply(http, resolver, mapper, PUBLIC).build();
    }
}

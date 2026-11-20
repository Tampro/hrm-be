package vn.specom.hrm.asset.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import vn.specom.hrm.security.CurrentUser;
import vn.specom.hrm.security.HrmPrincipal;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditingConfig {
    /** created_by / updated_by hold the Keycloak subject; "system" for writes made outside a request. */
    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(CurrentUser.find().map(HrmPrincipal::keycloakId).orElse("system"));
    }
}

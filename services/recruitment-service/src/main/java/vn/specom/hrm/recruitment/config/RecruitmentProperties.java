package vn.specom.hrm.recruitment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param iamServiceUrl      IAM, asked for the caller's authorities with the caller's own token
 * @param employeeServiceUrl employee-service, called with the caller's token (branches, departments)
 */
@Validated
@ConfigurationProperties(prefix = "hrm.recruitment")
public record RecruitmentProperties(@NotBlank String iamServiceUrl, @NotBlank String employeeServiceUrl) {
}

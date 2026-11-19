package vn.specom.hrm.attendance.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param iamServiceUrl IAM, asked for the caller's authorities with the caller's own token */
@Validated
@ConfigurationProperties(prefix = "hrm.attendance")
public record AttendanceProperties(@NotBlank String iamServiceUrl) {
}

package vn.specom.hrm.attendance.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param iamServiceUrl      IAM, asked for the caller's authorities with the caller's own token
 * @param employeeServiceUrl employee-service, called with the caller's token (employees, holidays)
 */
@Validated
@ConfigurationProperties(prefix = "hrm.attendance")
public record AttendanceProperties(@NotBlank String iamServiceUrl, @NotBlank String employeeServiceUrl) {
}

package vn.specom.hrm.meeting.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param iamServiceUrl      IAM, asked for the caller's authorities with the caller's own token
 * @param employeeServiceUrl employee-service, called with the caller's token (organizers, attendees, assignees)
 */
@Validated
@ConfigurationProperties(prefix = "hrm.meeting")
public record MeetingProperties(@NotBlank String iamServiceUrl, @NotBlank String employeeServiceUrl) {
}

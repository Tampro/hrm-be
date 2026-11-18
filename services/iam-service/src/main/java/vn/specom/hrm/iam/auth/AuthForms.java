package vn.specom.hrm.iam.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request bodies of /api/v1/auth. */
public final class AuthForms {
    private AuthForms() {
    }

    public record LoginForm(
            @Schema(description = "Username", example = "hr.demo", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 100) String username,
            @Schema(description = "Password", example = "********", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 128) String password,
            @Schema(description = "reCAPTCHA token; required when reCAPTCHA is enabled in the settings")
            @Size(max = 4000) String recaptchaToken) {
    }

    public record RefreshTokenForm(
            @Schema(description = "Refresh token from login or the previous refresh",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String refreshToken) {
    }

    public record InitialPasswordForm(
            @Schema(description = "Username", example = "hr.demo", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 100) String username,
            @Schema(description = "Temporary password given by the administrator",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 128) String currentPassword,
            @Schema(description = "New password; the realm password policy applies",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(min = 8, max = 128) String newPassword,
            @Schema(description = "reCAPTCHA token; required when reCAPTCHA is enabled in the settings")
            @Size(max = 4000) String recaptchaToken) {
    }

    @Schema(description = "Tokens. The FE sends accessToken as Bearer and calls refresh-token on 401.")
    public record TokenDto(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn) {
    }
}

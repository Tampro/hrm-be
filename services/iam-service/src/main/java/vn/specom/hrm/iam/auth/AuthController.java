package vn.specom.hrm.iam.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.iam.auth.AuthForms.InitialPasswordForm;
import vn.specom.hrm.iam.auth.AuthForms.LoginForm;
import vn.specom.hrm.iam.auth.AuthForms.RefreshTokenForm;
import vn.specom.hrm.iam.auth.AuthForms.TokenDto;

@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Log in with username and password",
            description = "Returns Keycloak tokens. Access token lives 15 minutes, the refresh token rotates.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in"),
            @ApiResponse(responseCode = "400", description = "Temporary password must be changed first (AUTH_004)"),
            @ApiResponse(responseCode = "401", description = "Wrong credentials (AUTH_001), account locked or "
                    + "company inactive (AUTH_002), too many attempts (AUTH_008)"),
            @ApiResponse(responseCode = "403", description = "No HRM profile for this account (AUTH_006)"),
            @ApiResponse(responseCode = "503", description = "Keycloak unreachable (AUTH_005)")})
    @PostMapping("/login")
    public ApiMessageDto<TokenDto> login(@Valid @RequestBody LoginForm form) {
        return ApiMessageDto.ok(authService.login(form.username(), form.password()));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New tokens; the old refresh token is now invalid"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired (AUTH_003), "
                    + "account locked or company inactive (AUTH_002)")})
    @PostMapping("/refresh-token")
    public ApiMessageDto<TokenDto> refresh(@Valid @RequestBody RefreshTokenForm form) {
        return ApiMessageDto.ok(authService.refresh(form.refreshToken()));
    }

    @Operation(summary = "End the session of a refresh token",
            description = "Always answers 200; access tokens already issued stay valid until they expire.")
    @PostMapping("/logout")
    public ApiMessageDto<Void> logout(@Valid @RequestBody RefreshTokenForm form) {
        authService.logout(form.refreshToken());
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Replace the temporary password at first login, then log in")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password set; returns tokens"),
            @ApiResponse(responseCode = "400", description = "Password policy not met (AUTH_007)"),
            @ApiResponse(responseCode = "401", description = "Wrong current password (AUTH_001), "
                    + "account locked (AUTH_002, AUTH_008)")})
    @PostMapping("/initial-password")
    public ApiMessageDto<TokenDto> initialPassword(@Valid @RequestBody InitialPasswordForm form) {
        return ApiMessageDto.ok(authService.setInitialPassword(form.username(), form.currentPassword(),
                form.newPassword()));
    }
}

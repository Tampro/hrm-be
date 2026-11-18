package vn.specom.hrm.iam.me;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.security.CurrentUser;
import vn.specom.hrm.security.HrmPrincipal;

import java.util.List;
import java.util.TreeSet;

@Tag(name = "Me")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    public record MeDto(String userId, String username, String companyId, boolean superAdmin,
                        List<String> roles, List<String> permissions) {
    }

    @Operation(summary = "Current user, company, roles and permissions",
            description = "The FE hides menus and buttons by `permissions`; the server checks them again on every call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Signed in but no HRM profile (AUTH_006)")})
    @GetMapping
    public ApiMessageDto<MeDto> me() {
        HrmPrincipal p = requireProfile();
        // Ids are strings on the wire (docs/analysis/04 §4).
        return ApiMessageDto.ok(new MeDto(
                p.userId() == null ? null : p.userId().toString(),
                p.username(),
                p.companyId() == null ? null : p.companyId().toString(),
                p.superAdmin(),
                List.copyOf(new TreeSet<>(p.roles())),
                List.copyOf(new TreeSet<>(p.permissions()))));
    }

    @Operation(summary = "Resolved authorities, for other HRM services",
            description = "Called by RemoteAuthorityResolver with the caller's own token; not meant for the FE.")
    @GetMapping("/authorities")
    public ApiMessageDto<HrmPrincipal> authorities() {
        return ApiMessageDto.ok(requireProfile());
    }

    private static HrmPrincipal requireProfile() {
        HrmPrincipal p = CurrentUser.get();
        if (!p.superAdmin() && p.userId() == null) {
            throw ApiException.forbidden(EIamError.AUTH_006);
        }
        return p;
    }
}

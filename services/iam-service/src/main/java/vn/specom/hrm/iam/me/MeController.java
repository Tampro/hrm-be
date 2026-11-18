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

    private final vn.specom.hrm.iam.repository.CompanyRepository companies;
    private final vn.specom.hrm.iam.saas.SubscriptionService subscriptions;

    /** Limits: null = none. */
    public record CompanyContextDto(String companyId, String name, Integer maxUsers, Integer maxEmployees,
                                    boolean blocked) {
    }

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

    @Operation(summary = "My company: name and the limits of its effective plan, for other HRM services",
            description = "employee-service checks maxEmployees before creating an employee and prints the name on letters. "
                    + "Super admin: companyId names the company.")
    @GetMapping("/company")
    public ApiMessageDto<CompanyContextDto> company(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long companyId) {
        requireProfile();
        Long id = vn.specom.hrm.security.TenantGuard.resolveCompany(companyId, EIamError.IAM_003);
        var company = companies.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EIamError.IAM_001));
        var e = subscriptions.effective(company);
        String cid = company.getId().toString();
        return ApiMessageDto.ok(e.plan() == null ? new CompanyContextDto(cid, company.getName(), null, null, e.blocked())
                : new CompanyContextDto(cid, company.getName(), e.plan().getMaxUsers(), e.plan().getMaxEmployees(),
                e.blocked()));
    }

    private static HrmPrincipal requireProfile() {
        HrmPrincipal p = CurrentUser.get();
        if (!p.superAdmin() && p.userId() == null) {
            throw ApiException.forbidden(EIamError.AUTH_006);
        }
        return p;
    }
}

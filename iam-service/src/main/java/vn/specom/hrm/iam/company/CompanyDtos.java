package vn.specom.hrm.iam.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.user.UserDtos.CreatedUserDto;

import java.time.Instant;

public final class CompanyDtos {
    private CompanyDtos() {
    }

    public record CompanyDto(String id, String code, String name, ECompanyStatus status, Instant createdDate,
                             Instant updatedDate) {
        static CompanyDto of(Company c) {
            return new CompanyDto(c.getId().toString(), c.getCode(), c.getName(), c.getStatus(), c.getCreatedDate(),
                    c.getUpdatedDate());
        }
    }

    public record CompanyCreatedDto(CompanyDto company, CreatedUserDto admin) {
    }

    public record AdminForm(
            @Schema(description = "Login name of the first company administrator", example = "acme.admin",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
            @Schema(description = "Email", example = "admin@acme.example", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @jakarta.validation.constraints.Email String email,
            @Schema(description = "Full name", example = "Acme Admin", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 255) String fullName) {
    }

    public record CompanyCreateForm(
            @Schema(description = "Unique code", example = "ACME", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+") String code,
            @Schema(description = "Company name", example = "Acme Co., Ltd", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 255) String name,
            @Schema(description = "First administrator; gets the COMPANY_ADMIN role",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Valid AdminForm admin) {
    }

    public record CompanyStatusForm(
            @Schema(description = "ACTIVE or SUSPENDED", example = "SUSPENDED",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull ECompanyStatus status) {
    }
}

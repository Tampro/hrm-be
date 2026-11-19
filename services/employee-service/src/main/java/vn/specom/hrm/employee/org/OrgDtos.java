package vn.specom.hrm.employee.org;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrgDtos {
    private OrgDtos() {
    }

    public record DesignationDto(String id, String companyId, String departmentId, String name) {
        static DesignationDto of(Designation d) {
            return new DesignationDto(d.getId().toString(), d.getCompanyId().toString(),
                    d.getDepartmentId().toString(), d.getName());
        }
    }

    /** system = shared by every company, read-only; defaultSlot = pre-filled in the Add Employee documents step. */
    public record DocumentTypeDto(String id, String name, boolean system, boolean defaultSlot) {
        static DocumentTypeDto of(DocumentType t) {
            return new DocumentTypeDto(t.getId().toString(), t.getName(), t.getCompanyId() == null, t.isDefaultSlot());
        }
    }

    public record NameForm(
            @Schema(description = "Name, unique within its parent", example = "Head office",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 150) String name) {
    }

    public record DesignationForm(
            @Schema(description = "Department id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long departmentId,
            @Schema(description = "Name, unique within the department", example = "IT Manager",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 150) String name) {
    }
}

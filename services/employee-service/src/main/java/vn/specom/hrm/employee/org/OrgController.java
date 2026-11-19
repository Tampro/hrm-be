package vn.specom.hrm.employee.org;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.employee.org.OrgDtos.DocumentTypeDto;
import vn.specom.hrm.employee.org.OrgDtos.NameForm;

import java.util.List;

@Tag(name = "Organization")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/org")
@SecurityRequirement(name = "bearerAuth")
public class OrgController {
    private static final String COMPANY = "Super admin only: the company to act on. Ignored for company users.";

    private final OrgService orgs;

    @Operation(summary = "Document types: the system ones and the company's own",
            description = "defaultSlot types are pre-filled in the documents step of Add Employee.")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.hasAny('ORG_VIEW', 'EMPLOYEE_VIEW')")
    @GetMapping("/document-types")
    public ApiMessageDto<List<DocumentTypeDto>> documentTypes(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId) {
        return ApiMessageDto.ok(orgs.documentTypes(companyId));
    }

    @Operation(summary = "Create a document type of the company")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Name taken (EMP_011), no company (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PostMapping("/document-types")
    public ApiMessageDto<DocumentTypeDto> createDocumentType(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
            @Valid @RequestBody NameForm form) {
        return ApiMessageDto.ok(orgs.createDocumentType(companyId, form.name()));
    }

    @Operation(summary = "Rename a document type of the company (full replacement)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken (EMP_011), system type (EMP_021)"),
            @ApiResponse(responseCode = "404", description = "No such document type in your company (EMP_005)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PutMapping("/document-types/{id}")
    public ApiMessageDto<DocumentTypeDto> renameDocumentType(@PathVariable Long id, @Valid @RequestBody NameForm form) {
        return ApiMessageDto.ok(orgs.renameDocumentType(id, form.name()));
    }

    @Operation(summary = "Delete a document type of the company", description = "Refused while a document uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Still in use (EMP_010), system type (EMP_021)"),
            @ApiResponse(responseCode = "404", description = "No such document type in your company (EMP_005)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @DeleteMapping("/document-types/{id}")
    public ApiMessageDto<Void> deleteDocumentType(@PathVariable Long id) {
        orgs.deleteDocumentType(id);
        return ApiMessageDto.ok(null);
    }
}

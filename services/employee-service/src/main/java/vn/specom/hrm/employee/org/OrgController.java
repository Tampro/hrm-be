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
import vn.specom.hrm.employee.org.OrgDtos.BranchDto;
import vn.specom.hrm.employee.org.OrgDtos.DepartmentDto;
import vn.specom.hrm.employee.org.OrgDtos.DepartmentForm;
import vn.specom.hrm.employee.org.OrgDtos.DesignationDto;
import vn.specom.hrm.employee.org.OrgDtos.DesignationForm;
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

    @Operation(summary = "Branches of the company")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('ORG_VIEW')")
    @GetMapping("/branches")
    public ApiMessageDto<List<BranchDto>> branches(@Parameter(description = COMPANY) @RequestParam(required = false) Long companyId) {
        return ApiMessageDto.ok(orgs.branches(companyId));
    }

    @Operation(summary = "Create a branch")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Name taken (EMP_011), no company (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PostMapping("/branches")
    public ApiMessageDto<BranchDto> createBranch(@Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
                                                 @Valid @RequestBody NameForm form) {
        return ApiMessageDto.ok(orgs.createBranch(companyId, form.name()));
    }

    @Operation(summary = "Rename a branch (full replacement)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken (EMP_011)"),
            @ApiResponse(responseCode = "404", description = "No such branch in your company (EMP_002)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PutMapping("/branches/{id}")
    public ApiMessageDto<BranchDto> renameBranch(@PathVariable Long id, @Valid @RequestBody NameForm form) {
        return ApiMessageDto.ok(orgs.renameBranch(id, form.name()));
    }

    @Operation(summary = "Delete a branch", description = "Refused while a department or an employee uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Still in use (EMP_010)"),
            @ApiResponse(responseCode = "404", description = "No such branch in your company (EMP_002)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @DeleteMapping("/branches/{id}")
    public ApiMessageDto<Void> deleteBranch(@PathVariable Long id) {
        orgs.deleteBranch(id);
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Departments of the company, optionally of one branch")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('ORG_VIEW')")
    @GetMapping("/departments")
    public ApiMessageDto<List<DepartmentDto>> departments(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId) {
        return ApiMessageDto.ok(orgs.departments(companyId, branchId));
    }

    @Operation(summary = "Create a department in a branch")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken in the branch (EMP_011)"),
            @ApiResponse(responseCode = "404", description = "No such branch in your company (EMP_002)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PostMapping("/departments")
    public ApiMessageDto<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentForm form) {
        return ApiMessageDto.ok(orgs.createDepartment(form.branchId(), form.name()));
    }

    @Operation(summary = "Update a department (full replacement)",
            description = "Moving it to another branch is refused while a designation or an employee uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken (EMP_011), still in use (EMP_010)"),
            @ApiResponse(responseCode = "404", description = "No such department (EMP_003) or branch (EMP_002)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PutMapping("/departments/{id}")
    public ApiMessageDto<DepartmentDto> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentForm form) {
        return ApiMessageDto.ok(orgs.updateDepartment(id, form.branchId(), form.name()));
    }

    @Operation(summary = "Delete a department", description = "Refused while a designation or an employee uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Still in use (EMP_010)"),
            @ApiResponse(responseCode = "404", description = "No such department in your company (EMP_003)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @DeleteMapping("/departments/{id}")
    public ApiMessageDto<Void> deleteDepartment(@PathVariable Long id) {
        orgs.deleteDepartment(id);
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Designations of the company, optionally of one department")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('ORG_VIEW')")
    @GetMapping("/designations")
    public ApiMessageDto<List<DesignationDto>> designations(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long departmentId) {
        return ApiMessageDto.ok(orgs.designations(companyId, departmentId));
    }

    @Operation(summary = "Create a designation in a department")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken in the department (EMP_011)"),
            @ApiResponse(responseCode = "404", description = "No such department in your company (EMP_003)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PostMapping("/designations")
    public ApiMessageDto<DesignationDto> createDesignation(@Valid @RequestBody DesignationForm form) {
        return ApiMessageDto.ok(orgs.createDesignation(form.departmentId(), form.name()));
    }

    @Operation(summary = "Update a designation (full replacement)",
            description = "Moving it to another department is refused while an employee uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Name taken (EMP_011), still in use (EMP_010)"),
            @ApiResponse(responseCode = "404", description = "No such designation (EMP_004) or department (EMP_003)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @PutMapping("/designations/{id}")
    public ApiMessageDto<DesignationDto> updateDesignation(@PathVariable Long id, @Valid @RequestBody DesignationForm form) {
        return ApiMessageDto.ok(orgs.updateDesignation(id, form.departmentId(), form.name()));
    }

    @Operation(summary = "Delete a designation", description = "Refused while an employee uses it.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Still in use (EMP_010)"),
            @ApiResponse(responseCode = "404", description = "No such designation in your company (EMP_004)")})
    @PreAuthorize("@PermissionChecker.has('ORG_MANAGE')")
    @DeleteMapping("/designations/{id}")
    public ApiMessageDto<Void> deleteDesignation(@PathVariable Long id) {
        orgs.deleteDesignation(id);
        return ApiMessageDto.ok(null);
    }

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

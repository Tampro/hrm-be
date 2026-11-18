package vn.specom.hrm.employee.employee;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import vn.specom.hrm.common.ResponseListDto;
import vn.specom.hrm.employee.employee.EmployeeDtos.DocumentDto;
import vn.specom.hrm.employee.employee.EmployeeDtos.DocumentForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.EmployeeCreateForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.EmployeeDetail;
import vn.specom.hrm.employee.employee.EmployeeDtos.EmployeeListItem;
import vn.specom.hrm.employee.employee.EmployeeDtos.Filter;
import vn.specom.hrm.employee.employee.EmployeeDtos.LoginForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.PasswordForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.ProfileForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.StatusCounts;
import vn.specom.hrm.employee.employee.EmployeeDtos.StatusForm;
import vn.specom.hrm.employee.employee.EmployeeDtos.VerifyForm;

import java.util.List;

@Tag(name = "Employees")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    static final String COMPANY = "Super admin only: the company to act on. Ignored for company users.";
    static final String KEYWORD = "Matches full name, email, employee ID or employee code";

    private final EmployeeService service;

    @Operation(summary = "Employees of the company",
            description = "Filters combine; sort by employeeNo, fullName, dateOfJoining, createdDate. "
                    + "profileImageUrl is a time-limited link.")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_VIEW')")
    @GetMapping
    public ApiMessageDto<ResponseListDto<EmployeeListItem>> list(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
            @Parameter(description = KEYWORD) @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) EEmployeeStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Filter f = new Filter(keyword, branchId, departmentId, designationId, status);
        return ApiMessageDto.ok(ResponseListDto.of(service.search(companyId, f, pageable)));
    }

    @Operation(summary = "Counts of the status tabs (All, Active, Inactive, Probation, Terminated)",
            description = "Same filters as the list, except status.")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Super admin without companyId (EMP_019)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_VIEW')")
    @GetMapping("/status-counts")
    public ApiMessageDto<StatusCounts> statusCounts(
            @Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
            @Parameter(description = KEYWORD) @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId) {
        return ApiMessageDto.ok(service.statusCounts(companyId, new Filter(keyword, branchId, departmentId, designationId, null)));
    }

    @Operation(summary = "Employee profile with documents",
            description = "banking is null without EMPLOYEE_BANKING_VIEW.")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_VIEW')")
    @GetMapping("/{id}")
    public ApiMessageDto<EmployeeDetail> get(@PathVariable Long id) {
        return ApiMessageDto.ok(service.get(id));
    }

    @Operation(summary = "Add an employee with a login account",
            description = "Employee ID (EMP000001…) is issued by the system. The account is created in IAM with the "
                    + "email as username and role EMPLOYEE; the password (given or generated) is temporary and "
                    + "emailed. Upload the profile image and documents to the media library first and pass their ids.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Email used (EMP_007), code used (EMP_008), plan limit "
                    + "(EMP_009), branch/department/designation (EMP_002/003/004/023), document type (EMP_005), file "
                    + "(EMP_012, EMP_018, EMP_024), no company (EMP_019); relayed from IAM: username taken (IAM_010), "
                    + "EMPLOYEE role missing (IAM_011), Keycloak/password policy (IAM_014), user limit (SAAS_011)"),
            @ApiResponse(responseCode = "403", description = "Caller lacks IAM_USER_CREATE in IAM (COMMON_004)"),
            @ApiResponse(responseCode = "503", description = "IAM (EMP_014) or media (EMP_015) unavailable")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_CREATE')")
    @PostMapping
    public ApiMessageDto<EmployeeDetail> create(@Parameter(description = COMPANY) @RequestParam(required = false) Long companyId,
                                                @Valid @RequestBody EmployeeCreateForm form) {
        return ApiMessageDto.ok(service.create(companyId, form));
    }

    @Operation(summary = "Update the profile (full replacement)",
            description = "Every field of the profile is replaced; omitted = cleared. Email and password are the "
                    + "login and are not changed here. banking is applied only with EMPLOYEE_BANKING_VIEW.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Code used (EMP_008), branch/department/designation "
                    + "(EMP_002/003/004/023), profile image (EMP_012, EMP_018)"),
            @ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}")
    public ApiMessageDto<EmployeeDetail> update(@PathVariable Long id, @Valid @RequestBody ProfileForm form) {
        return ApiMessageDto.ok(service.update(id, form));
    }

    @Operation(summary = "Change the employee status (Active, Inactive, Probation, Terminated)")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}/status")
    public ApiMessageDto<EmployeeDetail> status(@PathVariable Long id, @Valid @RequestBody StatusForm form) {
        return ApiMessageDto.ok(service.changeStatus(id, form.status()));
    }

    @Operation(summary = "Turn the login on or off",
            description = "Off locks the IAM account: Keycloak disables it and ends its sessions.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "No account (EMP_013); relayed: own account (IAM_013)"),
            @ApiResponse(responseCode = "403", description = "Caller lacks IAM_USER_UPDATE in IAM (COMMON_004)"),
            @ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}/login")
    public ApiMessageDto<EmployeeDetail> login(@PathVariable Long id, @Valid @RequestBody LoginForm form) {
        return ApiMessageDto.ok(service.setLogin(id, form.enabled()));
    }

    @Operation(summary = "Set a new password for the employee's account",
            description = "Permanent password; the realm password policy applies.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Confirmation differs (EMP_020), no account (EMP_013); "
                    + "relayed: password policy (AUTH_007), own account (IAM_013)"),
            @ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}/password")
    public ApiMessageDto<Void> password(@PathVariable Long id, @Valid @RequestBody PasswordForm form) {
        service.changePassword(id, form.newPassword(), form.confirmPassword());
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Delete an employee",
            description = "The login is locked first, then the employee is removed from every list.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Relayed: own account (IAM_013)"),
            @ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_DELETE')")
    @DeleteMapping("/{id}")
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Documents of the employee", description = "viewUrl/downloadUrl are time-limited links.")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_VIEW')")
    @GetMapping("/{id}/documents")
    public ApiMessageDto<List<DocumentDto>> documents(@PathVariable Long id) {
        return ApiMessageDto.ok(service.documents(id));
    }

    @Operation(summary = "Attach a document (a media file of the company, max 5 MB)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Document type (EMP_005), file (EMP_012, EMP_024)"),
            @ApiResponse(responseCode = "404", description = "No such employee in your company (EMP_001)")})
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PostMapping("/{id}/documents")
    public ApiMessageDto<DocumentDto> addDocument(@PathVariable Long id, @Valid @RequestBody DocumentForm form) {
        return ApiMessageDto.ok(service.addDocument(id, form));
    }

    @Operation(summary = "Mark a document verified or not")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such employee (EMP_001) or document (EMP_006)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}/documents/{documentId}/verify")
    public ApiMessageDto<DocumentDto> verify(@PathVariable Long id, @PathVariable Long documentId,
                                             @Valid @RequestBody VerifyForm form) {
        return ApiMessageDto.ok(service.verifyDocument(id, documentId, form.verified()));
    }

    @Operation(summary = "Remove a document", description = "The file stays in the media library.")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such employee (EMP_001) or document (EMP_006)"))
    @PreAuthorize("@PermissionChecker.has('EMPLOYEE_UPDATE')")
    @DeleteMapping("/{id}/documents/{documentId}")
    public ApiMessageDto<Void> deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        service.deleteDocument(id, documentId);
        return ApiMessageDto.ok(null);
    }
}

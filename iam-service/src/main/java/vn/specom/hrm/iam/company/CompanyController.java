package vn.specom.hrm.iam.company;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.common.ResponseListDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreateForm;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreatedDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyStatusForm;

@Tag(name = "Companies (super admin)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/iam/companies")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@PermissionChecker.isSuperAdmin()")
public class CompanyController {
    private final CompanyService companyService;

    @Operation(summary = "List companies")
    @GetMapping
    public ApiMessageDto<ResponseListDto<CompanyDto>> list(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiMessageDto.ok(ResponseListDto.of(companyService.list(pageable)));
    }

    @Operation(summary = "Company detail")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such company (IAM_001)"))
    @GetMapping("/{id}")
    public ApiMessageDto<CompanyDto> get(@PathVariable Long id) {
        return ApiMessageDto.ok(companyService.get(id));
    }

    @Operation(summary = "Create a company with its default roles and first administrator",
            description = "The administrator's temporary password is returned once in this response.")
    @ApiResponses(@ApiResponse(responseCode = "400",
            description = "Code taken (IAM_002), username taken (IAM_010), Keycloak error (IAM_014)"))
    @PostMapping
    public ApiMessageDto<CompanyCreatedDto> create(@Valid @RequestBody CompanyCreateForm form) {
        return ApiMessageDto.ok(companyService.create(form));
    }

    @Operation(summary = "Suspend or reactivate a company")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such company (IAM_001)"))
    @PutMapping("/{id}/status")
    public ApiMessageDto<CompanyDto> status(@PathVariable Long id, @Valid @RequestBody CompanyStatusForm form) {
        return ApiMessageDto.ok(companyService.changeStatus(id, form.status()));
    }
}

package vn.specom.hrm.iam.company;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreateForm;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreatedDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyDto;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EDefaultRole;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.model.Role;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.RoleRepository;
import vn.specom.hrm.iam.role.RoleService;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;
import vn.specom.hrm.iam.user.UserDtos.CreatedUserDto;
import vn.specom.hrm.iam.user.UserDtos.UserCreateForm;
import vn.specom.hrm.iam.user.UserService;

import java.util.Set;

/** Tenant lifecycle; super admin only. */
@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companies;
    private final RoleRepository roles;
    private final RoleService roleService;
    private final UserService userService;
    private final LocalAuthorityResolver authorityCache;

    @Transactional(readOnly = true)
    public Page<CompanyDto> list(Pageable pageable) {
        return companies.findByDeletedFalse(pageable).map(CompanyDto::of);
    }

    @Transactional(readOnly = true)
    public CompanyDto get(Long id) {
        return CompanyDto.of(require(id));
    }

    /** Company + default roles + first administrator in one transaction (the Keycloak user is created last). */
    @Transactional
    public CompanyCreatedDto create(CompanyCreateForm form) {
        if (companies.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(EIamError.IAM_002);
        }
        Company c = new Company();
        c.setCode(form.code());
        c.setName(form.name());
        c.setStatus(ECompanyStatus.ACTIVE);
        c = companies.save(c);
        roleService.createDefaultRoles(c.getId());
        Role admin = roles.findByCompanyIdAndCodeAndDeletedFalse(c.getId(), EDefaultRole.COMPANY_ADMIN.name())
                .orElseThrow();
        CreatedUserDto adminUser = userService.createInCompany(c.getId(), new UserCreateForm(
                form.admin().username(), form.admin().email(), form.admin().fullName(), Set.of(admin.getId())));
        return new CompanyCreatedDto(CompanyDto.of(c), adminUser);
    }

    /** SUSPENDED: logins fail and every token of the company is rejected within the 30 s authority cache. */
    @Transactional
    public CompanyDto changeStatus(Long id, ECompanyStatus status) {
        Company c = require(id);
        c.setStatus(status);
        authorityCache.evictAll();
        return CompanyDto.of(c);
    }

    private Company require(Long id) {
        return companies.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(EIamError.IAM_001));
    }
}

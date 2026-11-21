package vn.specom.hrm.document.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.document.common.EDocumentError;
import vn.specom.hrm.document.common.EStatus;
import vn.specom.hrm.document.common.Texts;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.security.TenantGuard;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Document Categories page (docs/analysis/06 §9.4.7): name unique in the company (DOC_003). The Documents column and
 * the in-use check on delete come with the HR documents.
 */
@Service
@RequiredArgsConstructor
public class DocumentCategoryService {
    private final DocumentCategoryRepository categories;

    public record CategoryForm(
            @Schema(example = "HR Policies", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 150)
            String name,
            @Size(max = 1000) String description,
            @Schema(description = "#RRGGBB; omitted = #3B82F6") @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
            @Schema(description = "Front-end icon key; omitted = folder", example = "shield")
            @Pattern(regexp = "^[a-z][a-z0-9-]{0,29}$") String icon,
            @Schema(description = "Yes, Mandatory / No, Optional; omitted = false on create, kept on edit")
            Boolean mandatory,
            @Schema(description = "Omitted = ACTIVE on create, kept on edit") EStatus status) {
    }

    public record CategoryDto(String id, String name, String description, String color, String icon,
                              boolean mandatory, EStatus status, Instant createdDate) {
    }

    public record Option(String id, String name, String color, String icon, boolean mandatory) {
    }

    @Transactional(readOnly = true)
    public Page<CategoryDto> search(Long requestedCompanyId, String keyword, EStatus status, Boolean mandatory,
                                    Pageable pageable) {
        return categories.search(Texts.company(requestedCompanyId), status, mandatory, Texts.kw(keyword), pageable)
                .map(DocumentCategoryService::dto);
    }

    /** The Category dropdown of the document form: active categories. */
    @Transactional(readOnly = true)
    public List<Option> options(Long requestedCompanyId) {
        return categories.findByCompanyIdAndStatusAndDeletedFalseOrderByNameAsc(Texts.company(requestedCompanyId),
                        EStatus.ACTIVE).stream()
                .map(c -> new Option(c.getId().toString(), c.getName(), c.getColor(), c.getIcon(), c.isMandatory()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long id) {
        return dto(require(id));
    }

    @Transactional
    public CategoryDto create(Long requestedCompanyId, CategoryForm form) {
        DocumentCategory c = new DocumentCategory();
        c.setCompanyId(Texts.company(requestedCompanyId));
        apply(c, form);
        return dto(categories.save(c));
    }

    /** Full replacement; mandatory and status omitted = kept. */
    @Transactional
    public CategoryDto update(Long id, CategoryForm form) {
        DocumentCategory c = require(id);
        apply(c, form);
        return dto(c);
    }

    /** Deactivate / activate. */
    @Transactional
    public CategoryDto changeStatus(Long id, EStatus status) {
        DocumentCategory c = require(id);
        c.setStatus(status);
        return dto(c);
    }

    @Transactional
    public void delete(Long id) {
        require(id).setDeleted(true);
    }

    private void apply(DocumentCategory c, CategoryForm f) {
        String name = f.name().trim();
        if (categories.existsByCompanyIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(c.getCompanyId(), name,
                c.getId() == null ? -1L : c.getId())) {
            throw ApiException.badRequest(EDocumentError.DOC_003);
        }
        c.setName(name);
        c.setDescription(Texts.blankToNull(f.description()));
        if (f.color() != null) {
            c.setColor(f.color().toUpperCase(Locale.ROOT));
        }
        if (f.icon() != null) {
            c.setIcon(f.icon());
        }
        if (f.mandatory() != null) {
            c.setMandatory(f.mandatory());
        }
        if (f.status() != null) {
            c.setStatus(f.status());
        }
    }

    static CategoryDto dto(DocumentCategory c) {
        return new CategoryDto(c.getId().toString(), c.getName(), c.getDescription(), c.getColor(), c.getIcon(),
                c.isMandatory(), c.getStatus(), c.getCreatedDate());
    }

    public DocumentCategory require(Long id) {
        DocumentCategory c = categories.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EDocumentError.DOC_002));
        TenantGuard.assertSameCompany(c.getCompanyId(), EDocumentError.DOC_002);
        return c;
    }
}

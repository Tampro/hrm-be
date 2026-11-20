package vn.specom.hrm.meeting.type;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.meeting.common.EMeetingError;
import vn.specom.hrm.meeting.common.EStatus;
import vn.specom.hrm.meeting.common.Texts;
import vn.specom.hrm.security.TenantGuard;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Meeting Types page (docs/analysis/06 §9.1.3): name unique in the company (MTG_003). */
@Service
@RequiredArgsConstructor
public class MeetingTypeService {
    private final MeetingTypeRepository types;

    public record TypeForm(
            @Schema(example = "Team Meeting", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 150)
            String name,
            @Size(max = 1000) String description,
            @Schema(description = "#RRGGBB; omitted = #3B82F6", example = "#22C55E")
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
            @Schema(description = "Minutes; omitted = 60", example = "30") @Min(5) @Max(1440)
            Integer defaultDurationMinutes,
            @Schema(description = "Omitted = ACTIVE on create, kept on edit") EStatus status) {
    }

    public record TypeDto(String id, String name, String description, String color, int defaultDurationMinutes,
                          EStatus status, Instant createdDate) {
    }

    public record Option(String id, String name, String color, int defaultDurationMinutes) {
    }

    @Transactional(readOnly = true)
    public Page<TypeDto> search(Long requestedCompanyId, String keyword, EStatus status, Pageable pageable) {
        return types.search(Texts.company(requestedCompanyId), status, Texts.kw(keyword), pageable)
                .map(MeetingTypeService::dto);
    }

    /** The Meeting Type dropdown of the meeting form: active types by name. */
    @Transactional(readOnly = true)
    public List<Option> options(Long requestedCompanyId) {
        return types.findByCompanyIdAndStatusAndDeletedFalseOrderByNameAsc(Texts.company(requestedCompanyId),
                        EStatus.ACTIVE).stream()
                .map(t -> new Option(t.getId().toString(), t.getName(), t.getColor(), t.getDefaultDurationMinutes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TypeDto get(Long id) {
        return dto(require(id));
    }

    @Transactional
    public TypeDto create(Long requestedCompanyId, TypeForm form) {
        MeetingType t = new MeetingType();
        t.setCompanyId(Texts.company(requestedCompanyId));
        apply(t, form);
        return dto(types.save(t));
    }

    /** Full replacement; status omitted = kept. */
    @Transactional
    public TypeDto update(Long id, TypeForm form) {
        MeetingType t = require(id);
        apply(t, form);
        return dto(t);
    }

    /** Deactivate / activate. */
    @Transactional
    public TypeDto changeStatus(Long id, EStatus status) {
        MeetingType t = require(id);
        t.setStatus(status);
        return dto(t);
    }

    @Transactional
    public void delete(Long id) {
        require(id).setDeleted(true);
    }

    private void apply(MeetingType t, TypeForm f) {
        String name = f.name().trim();
        if (types.existsByCompanyIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(t.getCompanyId(), name,
                t.getId() == null ? -1L : t.getId())) {
            throw ApiException.badRequest(EMeetingError.MTG_003);
        }
        t.setName(name);
        t.setDescription(Texts.blankToNull(f.description()));
        if (f.color() != null) {
            t.setColor(f.color().toUpperCase(Locale.ROOT));
        }
        if (f.defaultDurationMinutes() != null) {
            t.setDefaultDurationMinutes(f.defaultDurationMinutes());
        }
        if (f.status() != null) {
            t.setStatus(f.status());
        }
    }

    static TypeDto dto(MeetingType t) {
        return new TypeDto(t.getId().toString(), t.getName(), t.getDescription(), t.getColor(),
                t.getDefaultDurationMinutes(), t.getStatus(), t.getCreatedDate());
    }

    public MeetingType require(Long id) {
        MeetingType t = types.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EMeetingError.MTG_002));
        TenantGuard.assertSameCompany(t.getCompanyId(), EMeetingError.MTG_002);
        return t;
    }
}

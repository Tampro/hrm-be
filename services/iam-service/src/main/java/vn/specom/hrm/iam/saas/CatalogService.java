package vn.specom.hrm.iam.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.model.Coupon;
import vn.specom.hrm.iam.model.Plan;
import vn.specom.hrm.iam.repository.CouponRepository;
import vn.specom.hrm.iam.repository.PlanRepository;
import vn.specom.hrm.iam.saas.SaasDtos.CouponDto;
import vn.specom.hrm.iam.saas.SaasDtos.CouponForm;
import vn.specom.hrm.iam.saas.SaasDtos.PlanDto;
import vn.specom.hrm.iam.saas.SaasDtos.PlanForm;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;

import java.math.BigDecimal;
import java.util.List;

/** Plans and coupons, managed by the super admin. */
@Service
@RequiredArgsConstructor
public class CatalogService {
    private final PlanRepository plans;
    private final CouponRepository coupons;
    private final LocalAuthorityResolver authorityCache;

    @Transactional(readOnly = true)
    public List<PlanDto> allPlans() {
        return plans.findByDeletedFalseOrderByMonthlyPriceAsc().stream().map(PlanDto::of).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanDto> offeredPlans() {
        return plans.findByEnabledTrueAndDeletedFalseOrderByMonthlyPriceAsc().stream().map(PlanDto::of).toList();
    }

    @Transactional
    public PlanDto createPlan(PlanForm form) {
        if (plans.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(ESaasError.SAAS_002);
        }
        Plan p = new Plan();
        apply(p, form);
        return PlanDto.of(plans.save(p));
    }

    /** Full replacement. Limit changes apply to every company on the plan (within the 30 s authority cache). */
    @Transactional
    public PlanDto updatePlan(Long id, PlanForm form) {
        Plan p = requirePlan(id);
        if (!p.getCode().equals(form.code()) && plans.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(ESaasError.SAAS_002);
        }
        // Unsetting the default is allowed (no default = expired companies are blocked); a default plan is always
        // enabled (apply() forces it).
        apply(p, form);
        authorityCache.evictAll();
        return PlanDto.of(p);
    }

    /** Companies on a deleted plan fall back to the default plan (or EXPIRED) at their next request. */
    @Transactional
    public void deletePlan(Long id) {
        Plan p = requirePlan(id);
        if (p.isDefaultPlan()) {
            throw ApiException.badRequest(ESaasError.SAAS_013);
        }
        p.setDeleted(true);
        authorityCache.evictAll();
    }

    public Plan requirePlan(Long id) {
        return plans.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(ESaasError.SAAS_001));
    }

    private void apply(Plan p, PlanForm f) {
        if (f.defaultPlan()) {
            // Only one default: the previous one stops being it.
            plans.findFirstByDefaultPlanTrueAndDeletedFalse()
                    .filter(d -> !d.getId().equals(p.getId()))
                    .ifPresent(d -> d.setDefaultPlan(false));
        }
        p.setCode(f.code());
        p.setName(f.name());
        p.setDescription(f.description());
        p.setMonthlyPrice(f.monthlyPrice());
        p.setYearlyPrice(f.yearlyPrice());
        p.setMaxUsers(f.maxUsers());
        p.setMaxEmployees(f.maxEmployees());
        p.setStorageLimitGb(f.storageLimitGb());
        p.setTrialDays(f.trialDays());
        p.setDefaultPlan(f.defaultPlan());
        p.setEnabled(f.enabled() || f.defaultPlan());
    }

    // ------------------------------------------------------------------ coupons

    @Transactional(readOnly = true)
    public List<CouponDto> allCoupons() {
        return coupons.findByDeletedFalseOrderByIdDesc().stream().map(CouponDto::of).toList();
    }

    @Transactional
    public CouponDto createCoupon(CouponForm form) {
        if (coupons.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(ESaasError.SAAS_005);
        }
        Coupon c = new Coupon();
        apply(c, form);
        return CouponDto.of(coupons.save(c));
    }

    @Transactional
    public CouponDto updateCoupon(Long id, CouponForm form) {
        Coupon c = requireCoupon(id);
        if (!c.getCode().equals(form.code()) && coupons.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(ESaasError.SAAS_005);
        }
        apply(c, form);
        return CouponDto.of(c);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        requireCoupon(id).setDeleted(true);
    }

    private Coupon requireCoupon(Long id) {
        return coupons.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(ESaasError.SAAS_004));
    }

    private static void apply(Coupon c, CouponForm f) {
        if (f.type() == ECouponType.PERCENTAGE && f.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest(ESaasError.SAAS_012);
        }
        c.setCode(f.code());
        c.setName(f.name());
        c.setType(f.type());
        c.setDiscountValue(f.discountValue());
        c.setMinimumSpend(f.minimumSpend());
        c.setUseLimitTotal(f.useLimitTotal());
        c.setUseLimitPerCompany(f.useLimitPerCompany());
        c.setExpiryDate(f.expiryDate());
        c.setEnabled(f.enabled());
    }
}

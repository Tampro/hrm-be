package vn.specom.hrm.iam.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.model.Plan;
import vn.specom.hrm.iam.model.PlanOrder;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.PlanOrderRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.iam.saas.SaasDtos.BankTransferDto;
import vn.specom.hrm.iam.saas.SaasDtos.OrderDto;
import vn.specom.hrm.iam.saas.SaasDtos.OrderForm;
import vn.specom.hrm.iam.saas.SaasDtos.QuoteDto;
import vn.specom.hrm.iam.saas.SaasDtos.SubscriptionDto;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;
import vn.specom.hrm.security.CurrentUser;
import vn.specom.hrm.security.TenantGuard;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Plan orders paid by bank transfer: the company orders (price frozen, coupon applied), transfers with the order
 * number as content, the super admin approves (plan activated / extended) or rejects.
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PlanOrderRepository orders;
    private final CompanyRepository companies;
    private final UserAccountRepository users;
    private final CatalogService catalog;
    private final PricingService pricing;
    private final SubscriptionService subscriptions;
    private final BankTransferProperties bank;
    private final LocalAuthorityResolver authorityCache;
    private final Clock clock;

    // ------------------------------------------------------------------ company side

    @Transactional(readOnly = true)
    public SubscriptionDto mySubscription(Long requestedCompanyId) {
        Company c = company(TenantGuard.resolveCompany(requestedCompanyId, EIamError.IAM_003));
        var e = subscriptions.effective(c);
        return new SubscriptionDto(e.status(), SaasDtos.PlanDto.of(e.plan()), e.expiresAt(), e.fallback(),
                users.countByCompanyIdAndDeletedFalse(c.getId()));
    }

    @Transactional(readOnly = true)
    public QuoteDto quote(OrderForm form) {
        Long companyId = TenantGuard.resolveCompany(null, EIamError.IAM_003);
        var q = pricing.quote(offered(form.planId()), form.billingCycle(), form.couponCode(), companyId);
        return new QuoteDto(q.originalPrice(), q.discountAmount(), q.finalPrice(),
                q.coupon() == null ? null : q.coupon().getCode());
    }

    @Transactional
    public OrderDto create(OrderForm form) {
        Long companyId = TenantGuard.resolveCompany(null, EIamError.IAM_003);
        // One open order at a time keeps the approver from activating two periods for one transfer.
        if (orders.existsByCompanyIdAndStatus(companyId, EOrderStatus.PENDING)) {
            throw ApiException.badRequest(ESaasError.SAAS_010);
        }
        Plan plan = offered(form.planId());
        var q = pricing.quote(plan, form.billingCycle(), form.couponCode(), companyId);
        PlanOrder o = new PlanOrder();
        o.setOrderNumber(newOrderNumber());
        o.setCompanyId(companyId);
        o.setPlanId(plan.getId());
        o.setBillingCycle(form.billingCycle());
        o.setCouponId(q.coupon() == null ? null : q.coupon().getId());
        o.setCouponCode(q.coupon() == null ? null : q.coupon().getCode());
        o.setOriginalPrice(q.originalPrice());
        o.setDiscountAmount(q.discountAmount());
        o.setFinalPrice(q.finalPrice());
        o.setPaymentMethod(EPaymentMethod.BANK_TRANSFER);
        o.setPaymentReference(form.paymentReference());
        o.setStatus(EOrderStatus.PENDING);
        return dto(orders.save(o));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> myOrders(Long requestedCompanyId, Pageable pageable) {
        Long companyId = TenantGuard.resolveCompany(requestedCompanyId, EIamError.IAM_003);
        return orders.findByCompanyId(companyId, pageable).map(this::dto);
    }

    @Transactional
    public OrderDto cancel(Long id) {
        PlanOrder o = require(id);
        decide(o, EOrderStatus.CANCELLED, null);
        return dto(o);
    }

    // ------------------------------------------------------------------ super admin side

    @Transactional(readOnly = true)
    public Page<OrderDto> all(EOrderStatus status, Pageable pageable) {
        return (status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable)).map(this::dto);
    }

    /** Activates or extends the ordered plan from the price frozen in the order. */
    @Transactional
    public OrderDto approve(Long id, String notes) {
        PlanOrder o = require(id);
        decide(o, EOrderStatus.APPROVED, notes);
        Company c = company(o.getCompanyId());
        subscriptions.activate(c, catalog.requirePlan(o.getPlanId()), o.getBillingCycle());
        authorityCache.evictAll();
        return dto(o);
    }

    @Transactional
    public OrderDto reject(Long id, String notes) {
        PlanOrder o = require(id);
        decide(o, EOrderStatus.REJECTED, notes);
        return dto(o);
    }

    @Transactional
    public SubscriptionDto assign(Long companyId, Long planId, Instant expiresAt) {
        Company c = company(companyId);
        subscriptions.assign(c, catalog.requirePlan(planId), expiresAt);
        authorityCache.evictAll();
        var e = subscriptions.effective(c);
        return new SubscriptionDto(e.status(), SaasDtos.PlanDto.of(e.plan()), e.expiresAt(), e.fallback(),
                users.countByCompanyIdAndDeletedFalse(c.getId()));
    }

    // ------------------------------------------------------------------ helpers

    private void decide(PlanOrder o, EOrderStatus to, String notes) {
        if (o.getStatus() != EOrderStatus.PENDING) {
            throw ApiException.badRequest(ESaasError.SAAS_009);
        }
        o.setStatus(to);
        o.setProcessedAt(clock.instant());
        o.setProcessedBy(CurrentUser.get().keycloakId());
        if (notes != null) {
            o.setNotes(notes);
        }
    }

    private PlanOrder require(Long id) {
        PlanOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound(ESaasError.SAAS_008));
        TenantGuard.assertSameCompany(o.getCompanyId(), ESaasError.SAAS_008);
        return o;
    }

    private Plan offered(Long planId) {
        Plan p = catalog.requirePlan(planId);
        if (!p.isEnabled()) {
            throw ApiException.badRequest(ESaasError.SAAS_014);
        }
        return p;
    }

    private Company company(Long id) {
        return companies.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(EIamError.IAM_001));
    }

    private String newOrderNumber() {
        // ORD + UTC date + 6 random digits; unique key in the table catches the rare collision.
        return "ORD" + DAY.format(clock.instant()) + String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private OrderDto dto(PlanOrder o) {
        BankTransferDto transfer = o.getStatus() == EOrderStatus.PENDING
                ? new BankTransferDto(bank.bankName(), bank.accountNumber(), bank.accountName(), o.getOrderNumber())
                : null;
        return OrderDto.of(o, transfer);
    }
}

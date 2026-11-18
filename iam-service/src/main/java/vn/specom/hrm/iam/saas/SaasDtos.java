package vn.specom.hrm.iam.saas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.specom.hrm.iam.model.Coupon;
import vn.specom.hrm.iam.model.Plan;
import vn.specom.hrm.iam.model.PlanOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class SaasDtos {
    private SaasDtos() {
    }

    static String str(Long id) {
        return id == null ? null : id.toString();
    }

    // ------------------------------------------------------------------ plans

    public record PlanDto(String id, String code, String name, String description, BigDecimal monthlyPrice,
                          BigDecimal yearlyPrice, Integer maxUsers, Integer maxEmployees, BigDecimal storageLimitGb,
                          int trialDays, boolean defaultPlan, boolean enabled) {
        public static PlanDto of(Plan p) {
            return p == null ? null : new PlanDto(SaasDtos.str(p.getId()), p.getCode(), p.getName(), p.getDescription(),
                    p.getMonthlyPrice(), p.getYearlyPrice(), p.getMaxUsers(), p.getMaxEmployees(),
                    p.getStorageLimitGb(), p.getTrialDays(), p.isDefaultPlan(), p.isEnabled());
        }
    }

    public record PlanForm(
            @Schema(description = "Unique code, UPPER_SNAKE_CASE", example = "STARTER", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String code,
            @Schema(description = "Display name", example = "Starter", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "Optional; cleared when omitted on update", example = "Up to 20 accounts")
            @Size(max = 255) String description,
            @Schema(description = "Price per month", example = "490000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0") BigDecimal monthlyPrice,
            @Schema(description = "Price per year; omit = no yearly billing", example = "4900000")
            @DecimalMin("0") BigDecimal yearlyPrice,
            @Schema(description = "Max accounts; omit = unlimited", example = "20") @Min(1) Integer maxUsers,
            @Schema(description = "Max employees (stored, enforced later); omit = unlimited", example = "50")
            @Min(1) Integer maxEmployees,
            @Schema(description = "Storage in GB (stored, enforced later); omit = unlimited", example = "5")
            @DecimalMin("0") BigDecimal storageLimitGb,
            @Schema(description = "Trial days when given as the default plan to a new company", example = "14")
            @Min(0) int trialDays,
            @Schema(description = "The plan new companies get and expired ones fall back to; setting it unsets the previous default",
                    example = "false") boolean defaultPlan,
            @Schema(description = "Offered to companies", example = "true") boolean enabled) {
    }

    // ------------------------------------------------------------------ coupons

    public record CouponDto(String id, String code, String name, ECouponType type, BigDecimal discountValue,
                            BigDecimal minimumSpend, Integer useLimitTotal, Integer useLimitPerCompany,
                            LocalDate expiryDate, boolean enabled) {
        public static CouponDto of(Coupon c) {
            return new CouponDto(SaasDtos.str(c.getId()), c.getCode(), c.getName(), c.getType(), c.getDiscountValue(),
                    c.getMinimumSpend(), c.getUseLimitTotal(), c.getUseLimitPerCompany(), c.getExpiryDate(),
                    c.isEnabled());
        }
    }

    public record CouponForm(
            @Schema(description = "Code companies type", example = "WELCOME10", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_-]+") String code,
            @Schema(description = "Display name", example = "Welcome 10%", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "PERCENTAGE or FLAT", example = "PERCENTAGE", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull ECouponType type,
            @Schema(description = "Percent (0–100) or amount", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0") BigDecimal discountValue,
            @Schema(description = "Minimum order price; omit = none", example = "400000") @DecimalMin("0") BigDecimal minimumSpend,
            @Schema(description = "Total uses; omit = unlimited", example = "100") @Min(1) Integer useLimitTotal,
            @Schema(description = "Uses per company; omit = unlimited", example = "1") @Min(1) Integer useLimitPerCompany,
            @Schema(description = "Last valid day (UTC); omit = no expiry", example = "2026-12-31") LocalDate expiryDate,
            @Schema(description = "Usable", example = "true") boolean enabled) {
    }

    // ------------------------------------------------------------------ subscription & orders

    public record SubscriptionDto(ESubscriptionStatus status, PlanDto plan, Instant expiresAt,
                                  @Schema(description = "true when the paid/trial plan expired and the default plan applies")
                                  boolean fallback, long usedUsers) {
    }

    public record SubscriptionAssignForm(
            @Schema(description = "Plan id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long planId,
            @Schema(description = "End of the period (UTC); omit = no expiry", example = "2027-01-31T00:00:00Z") Instant expiresAt) {
    }

    public record OrderForm(
            @Schema(description = "Plan id", example = "2", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long planId,
            @Schema(description = "MONTHLY or YEARLY", example = "MONTHLY", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull EBillingCycle billingCycle,
            @Schema(description = "Optional coupon code", example = "WELCOME10") @Size(max = 50) String couponCode,
            @Schema(description = "Bank transfer reference, if already paid", example = "FT2611180001")
            @Size(max = 100) String paymentReference) {
    }

    public record QuoteDto(BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal finalPrice, String couponCode) {
    }

    public record BankTransferDto(String bankName, String accountNumber, String accountName, String transferContent) {
    }

    public record OrderDto(String id, String orderNumber, String companyId, String planId, EBillingCycle billingCycle,
                           String couponCode, BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal finalPrice,
                           EPaymentMethod paymentMethod, String paymentReference, EOrderStatus status,
                           Instant createdDate, Instant processedAt, String notes, BankTransferDto bankTransfer) {
        public static OrderDto of(PlanOrder o, BankTransferDto bank) {
            return new OrderDto(SaasDtos.str(o.getId()), o.getOrderNumber(), SaasDtos.str(o.getCompanyId()), SaasDtos.str(o.getPlanId()),
                    o.getBillingCycle(), o.getCouponCode(), o.getOriginalPrice(), o.getDiscountAmount(),
                    o.getFinalPrice(), o.getPaymentMethod(), o.getPaymentReference(), o.getStatus(),
                    o.getCreatedDate(), o.getProcessedAt(), o.getNotes(), bank);
        }
    }

    public record OrderDecisionForm(
            @Schema(description = "Note shown to the company (reason when rejecting)", example = "Transfer not received")
            @Size(max = 500) String notes) {
    }
}

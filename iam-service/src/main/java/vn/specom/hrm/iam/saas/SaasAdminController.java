package vn.specom.hrm.iam.saas;

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
import vn.specom.hrm.iam.saas.SaasDtos.CouponDto;
import vn.specom.hrm.iam.saas.SaasDtos.CouponForm;
import vn.specom.hrm.iam.saas.SaasDtos.OrderDecisionForm;
import vn.specom.hrm.iam.saas.SaasDtos.OrderDto;
import vn.specom.hrm.iam.saas.SaasDtos.PlanDto;
import vn.specom.hrm.iam.saas.SaasDtos.PlanForm;
import vn.specom.hrm.iam.saas.SaasDtos.SubscriptionAssignForm;
import vn.specom.hrm.iam.saas.SaasDtos.SubscriptionDto;

import java.util.List;

@Tag(name = "SaaS — administration (super admin)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/saas/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@PermissionChecker.isSuperAdmin()")
public class SaasAdminController {
    private final CatalogService catalog;
    private final OrderService orderService;

    @Operation(summary = "All plans (enabled or not)")
    @GetMapping("/plans")
    public ApiMessageDto<List<PlanDto>> plans() {
        return ApiMessageDto.ok(catalog.allPlans());
    }

    @Operation(summary = "Create a plan", description = "defaultPlan = true moves the default flag to this plan.")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Code taken (SAAS_002)"))
    @PostMapping("/plans")
    public ApiMessageDto<PlanDto> createPlan(@Valid @RequestBody PlanForm form) {
        return ApiMessageDto.ok(catalog.createPlan(form));
    }

    @Operation(summary = "Update a plan (full replacement)",
            description = "PUT replaces every field: omitted limits become unlimited. New limits apply to every "
                    + "company on the plan.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Code taken (SAAS_002)"),
            @ApiResponse(responseCode = "404", description = "No such plan (SAAS_001)")})
    @PutMapping("/plans/{id}")
    public ApiMessageDto<PlanDto> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanForm form) {
        return ApiMessageDto.ok(catalog.updatePlan(id, form));
    }

    @Operation(summary = "Delete a plan (soft)", description = "Companies on it fall back to the default plan or EXPIRED.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Default plan (SAAS_013)"),
            @ApiResponse(responseCode = "404", description = "No such plan (SAAS_001)")})
    @DeleteMapping("/plans/{id}")
    public ApiMessageDto<Void> deletePlan(@PathVariable Long id) {
        catalog.deletePlan(id);
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "All coupons")
    @GetMapping("/coupons")
    public ApiMessageDto<List<CouponDto>> coupons() {
        return ApiMessageDto.ok(catalog.allCoupons());
    }

    @Operation(summary = "Create a coupon")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Code taken (SAAS_005), percentage > 100 (SAAS_012)"))
    @PostMapping("/coupons")
    public ApiMessageDto<CouponDto> createCoupon(@Valid @RequestBody CouponForm form) {
        return ApiMessageDto.ok(catalog.createCoupon(form));
    }

    @Operation(summary = "Update a coupon (full replacement)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Code taken (SAAS_005), percentage > 100 (SAAS_012)"),
            @ApiResponse(responseCode = "404", description = "No such coupon (SAAS_004)")})
    @PutMapping("/coupons/{id}")
    public ApiMessageDto<CouponDto> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponForm form) {
        return ApiMessageDto.ok(catalog.updateCoupon(id, form));
    }

    @Operation(summary = "Delete a coupon (soft)")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such coupon (SAAS_004)"))
    @DeleteMapping("/coupons/{id}")
    public ApiMessageDto<Void> deleteCoupon(@PathVariable Long id) {
        catalog.deleteCoupon(id);
        return ApiMessageDto.ok(null);
    }

    @Operation(summary = "Orders of every company")
    @GetMapping("/orders")
    public ApiMessageDto<ResponseListDto<OrderDto>> orders(
            @RequestParam(required = false) EOrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiMessageDto.ok(ResponseListDto.of(orderService.all(status, pageable)));
    }

    @Operation(summary = "Approve a paid order", description = "Activates the plan for 1 month / 1 year, or extends "
            + "it when the company renews the plan it is on.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Not pending (SAAS_009)"),
            @ApiResponse(responseCode = "404", description = "No such order (SAAS_008), plan deleted (SAAS_001)")})
    @PostMapping("/orders/{id}/approve")
    public ApiMessageDto<OrderDto> approve(@PathVariable Long id, @Valid @RequestBody(required = false) OrderDecisionForm form) {
        return ApiMessageDto.ok(orderService.approve(id, form == null ? null : form.notes()));
    }

    @Operation(summary = "Reject an order")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Not pending (SAAS_009)"),
            @ApiResponse(responseCode = "404", description = "No such order (SAAS_008)")})
    @PostMapping("/orders/{id}/reject")
    public ApiMessageDto<OrderDto> reject(@PathVariable Long id, @Valid @RequestBody(required = false) OrderDecisionForm form) {
        return ApiMessageDto.ok(orderService.reject(id, form == null ? null : form.notes()));
    }

    @Operation(summary = "Set a company's plan directly (upgrade, gift, correction)")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No such company (IAM_001) or plan (SAAS_001)"))
    @PutMapping("/companies/{companyId}/subscription")
    public ApiMessageDto<SubscriptionDto> assign(@PathVariable Long companyId,
                                                 @Valid @RequestBody SubscriptionAssignForm form) {
        return ApiMessageDto.ok(orderService.assign(companyId, form.planId(), form.expiresAt()));
    }
}

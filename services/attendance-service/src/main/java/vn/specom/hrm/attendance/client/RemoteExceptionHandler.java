package vn.specom.hrm.attendance.client;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.common.TraceIdFilter;
import vn.specom.hrm.attendance.common.EAttendanceError;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.exception.RestExceptionHandler;

/** Runs before the common handler, whose catch-all would turn these 400s into a 500. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@lombok.RequiredArgsConstructor
public class RemoteExceptionHandler {
    private final RestExceptionHandler common;

    /** An import file over the multipart limit is the client's mistake. */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiMessageDto<Object>> tooLarge() {
        return common.handleApi(ApiException.badRequest(EAttendanceError.ATT_016, "5 MB"));
    }

    /**
     * Invalid or missing query parameters (year=13, month=abc, no year) are the client's mistake: 400 COMMON_001.
     * TODO: the common RestExceptionHandler answers these with 500 in every service — fix it there (hrm-common).
     */
    @ExceptionHandler({jakarta.validation.ConstraintViolationException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<ApiMessageDto<Object>> badParameter() {
        return common.handleApi(ApiException.badRequest(vn.specom.hrm.exception.ECommonError.COMMON_001));
    }

    @ExceptionHandler(RemoteApiException.class)
    public ResponseEntity<ApiMessageDto<Object>> handle(RemoteApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiMessageDto.builder()
                .result(false).code(ex.getCode()).message(ex.getRemoteMessage())
                .traceId(MDC.get(TraceIdFilter.MDC_KEY)).build());
    }
}

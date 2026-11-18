package vn.specom.hrm.employee.client;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.common.TraceIdFilter;
import vn.specom.hrm.employee.common.EEmployeeError;
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
        return common.handleApi(ApiException.badRequest(EEmployeeError.EMP_026, "5 MB"));
    }

    @ExceptionHandler(RemoteApiException.class)
    public ResponseEntity<ApiMessageDto<Object>> handle(RemoteApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiMessageDto.builder()
                .result(false).code(ex.getCode()).message(ex.getRemoteMessage())
                .traceId(MDC.get(TraceIdFilter.MDC_KEY)).build());
    }
}

package vn.specom.hrm.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.specom.hrm.common.ApiMessageDto;
import vn.specom.hrm.common.TraceIdFilter;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiMessageDto<Object>> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getErrorCode(), null, ex.getArgs());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageDto<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, ECommonError.COMMON_001, fields);
    }

    // @PreAuthorize failures are thrown inside the handler, so they arrive here, not at the AccessDeniedHandler.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiMessageDto<Object>> handleDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ECommonError.COMMON_004, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageDto<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ECommonError.COMMON_002, null);
    }

    public String resolve(ErrorCode error, Object... args) {
        return messageSource.getMessage(error.getCode(), args, error.getMessage(), LocaleContextHolder.getLocale());
    }

    private ResponseEntity<ApiMessageDto<Object>> build(HttpStatus status, ErrorCode error, Object data, Object... args) {
        ApiMessageDto<Object> body = ApiMessageDto.builder()
                .result(false)
                .code(error.getCode())
                .message(resolve(error, args))
                .data(data)
                .traceId(MDC.get(TraceIdFilter.MDC_KEY))
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

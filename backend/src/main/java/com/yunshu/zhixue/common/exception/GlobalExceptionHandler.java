package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常映射（04 §2.7）：业务异常 → 对应业务码；校验异常 → 10000 + fieldErrors；
 * 未捕获异常 → 10006，日志保留堆栈但响应不泄露内部细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.httpStatus())
                .body(ApiResponse.fail(ec.code(), ex.getMessage()));
    }

    /** @Valid 请求体校验失败：一次返回全部字段错误。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldErrorItem(fe.getField(), reasonOf(fe)))
                .toList();
        return badRequest(errors);
    }

    /** @Validated 方法参数（含 @RequestParam / @PathVariable）校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleParamValidation(ConstraintViolationException ex) {
        List<ApiResponse.FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return badRequest(errors);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(Exception ex) {
        return badRequest(List.of(new ApiResponse.FieldErrorItem("request", "请求参数缺失或类型不正确")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ErrorCode ec = ErrorCode.BAD_REQUEST_BODY;
        return ResponseEntity.status(ec.httpStatus()).body(ApiResponse.fail(ec.code(), ec.message()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        ErrorCode ec = ErrorCode.FILE_INVALID;
        return ResponseEntity.status(ec.httpStatus()).body(ApiResponse.fail(ec.code(), "文件大小超出限制"));
    }

    /** 未匹配到任何接口（尚未实现的 API）：按 404 返回，避免被兜底处理成 10006。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        ErrorCode ec = ErrorCode.NOT_FOUND;
        return ResponseEntity.status(ec.httpStatus()).body(ApiResponse.fail(ec.code(), ec.message()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode ec = ErrorCode.FORBIDDEN;
        return ResponseEntity.status(ec.httpStatus()).body(ApiResponse.fail(ec.code(), ec.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("未捕获异常", ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(List<ApiResponse.FieldErrorItem> errors) {
        ErrorCode ec = ErrorCode.PARAM_INVALID;
        return ResponseEntity.status(ec.httpStatus())
                .body(ApiResponse.fail(ec.code(), ec.message(), errors));
    }

    private ApiResponse.FieldErrorItem toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new ApiResponse.FieldErrorItem(field, violation.getMessage());
    }

    private String reasonOf(FieldError fe) {
        return fe.getDefaultMessage() == null ? "参数不合法" : fe.getDefaultMessage();
    }
}

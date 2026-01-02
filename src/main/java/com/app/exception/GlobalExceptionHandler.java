package com.app.exception;

import com.app.dto.common.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Custom business exception (اگر استفاده می‌کنی)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    // ✅ مهم: چون در سرویس‌ها هنوز IllegalArgumentException زیاد داری
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "درخواست نامعتبر است."
                : ex.getMessage();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("BAD_REQUEST", msg));
    }

    // ✅ @Valid روی DTOها
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        "VALIDATION_ERROR",
                        "اطلاعات ورودی نامعتبر است.",
                        details
                ));
    }

    // ✅ Validation روی path/query params
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        "VALIDATION_ERROR",
                        "خطا در اعتبارسنجی اطلاعات.",
                        details
                ));
    }

    // ✅ خطاهای دیتابیس: FK / unique / not null / ...
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {

        // پیام عمومی و قابل‌فهم برای کاربر
        // (جزئیات SQL را به کاربر نمایش نمی‌دهیم)
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "DB_CONSTRAINT",
                        "امکان ثبت/ویرایش/حذف وجود ندارد. احتمالاً داده تکراری است یا رکورد وابستگی دارد."
                ));
    }

    // ✅ اگر جایی EntityNotFoundException انداختی
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "رکورد مورد نظر یافت نشد."
                : ex.getMessage();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", msg));
    }

    // ✅ fallback برای خطاهای غیرمنتظره
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ex.printStackTrace(); // فقط لاگ

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "INTERNAL_ERROR",
                        "خطای غیرمنتظره‌ای رخ داده است. لطفاً دوباره تلاش کنید."
                ));
    }
}

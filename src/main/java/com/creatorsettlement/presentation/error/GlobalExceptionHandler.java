package com.creatorsettlement.presentation.error;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, DomainErrorMessage> MESSAGE_INDEX =
            Arrays.stream(DomainErrorMessage.values())
                    .collect(Collectors.toUnmodifiableMap(DomainErrorMessage::message, Function.identity()));

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        DomainErrorMessage matched = MESSAGE_INDEX.get(msg);
        if (matched == null) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(400, "BAD_REQUEST", msg));
        }
        HttpStatus status = HttpStatus.valueOf(matched.httpStatus());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), matched.name(), matched.message()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("validation failed");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "VALIDATION", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(500, "INTERNAL", "예기치 못한 오류가 발생했습니다"));
    }
}

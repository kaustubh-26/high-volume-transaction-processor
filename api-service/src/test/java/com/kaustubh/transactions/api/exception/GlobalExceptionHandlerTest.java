package com.kaustubh.transactions.api.exception;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Tag("unit")
    @Test
    void handleValidation_returnsFieldErrors_whenBindingResultPresent() throws Exception {
        Method method = TestController.class.getDeclaredMethod("sample", TestRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BindingResult bindingResult = new BeanPropertyBindingResult(new TestRequest(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be positive"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> body = response.getBody();
        assertThat(body)
                .isNotNull()
                .containsEntry("error", "Validation failed");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) body.get("fieldErrors");

        assertThat(fieldErrors).containsEntry("amount", "must be positive");
    }

    @Tag("unit")
    @Test
    void handleValidation_usesDefaultMessage_whenFieldErrorMessageNull() throws Exception {
        Method method = TestController.class.getDeclaredMethod("sample", TestRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BindingResult bindingResult = new BeanPropertyBindingResult(new TestRequest(), "request");
        bindingResult.addError(new FieldError("request", "amount", null));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) body.get("fieldErrors");

        assertThat(fieldErrors).containsEntry("amount", "invalid value");
    }

    static class TestController {
        void sample(TestRequest request) {
            // Intentionally empty: the test only reflects on this signature to build a MethodParameter.
        }
    }

    static class TestRequest {
    }
}

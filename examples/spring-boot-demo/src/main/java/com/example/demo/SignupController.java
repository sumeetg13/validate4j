package com.example.demo;

import io.github.sumeetg13.validate4j.core.Validate4j;
import io.github.sumeetg13.validate4j.core.ValidationResult;
import io.github.sumeetg13.validate4j.spring.ValidateWith;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SignupController {

    private final Validate4j validate4j;

    public SignupController(Validate4j validate4j) {
        this.validate4j = validate4j;
    }

    /** Declarative: the body is validated before this method runs; failures become a 400 ProblemDetail. */
    @PostMapping("/signup")
    public Map<String, Object> signup(@ValidateWith("signup") @RequestBody Map<String, Object> body) {
        return Map.of("status", "created", "name", body.get("name"));
    }

    /** Programmatic: inspect the result yourself. */
    @PostMapping("/signup/check")
    public Map<String, Object> check(@RequestBody Map<String, Object> body) {
        ValidationResult result = validate4j.validate("signup", body);
        return Map.of("valid", result.isValid(), "errors", result.errors());
    }
}

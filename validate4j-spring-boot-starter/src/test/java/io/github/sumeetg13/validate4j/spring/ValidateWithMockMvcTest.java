package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ValidateWithMockMvcTest.App.class)
@AutoConfigureMockMvc
class ValidateWithMockMvcTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        ValidatorFactory uppercase() {
            return new UppercaseValidatorFactory();
        }

        @Bean
        SignupController controller() {
            return new SignupController();
        }
    }

    record SignupRequest(String name, Integer age, String phone, String code) {
    }

    @RestController
    static class SignupController {
        @PostMapping("/map")
        Map<String, Object> map(@ValidateWith("signup") @RequestBody Map<String, Object> body) {
            return Map.of("ok", true, "name", body.get("name"));
        }

        @PostMapping("/record")
        Map<String, Object> record(@ValidateWith("signup") @RequestBody SignupRequest body) {
            return Map.of("ok", true, "name", body.name());
        }

        @PostMapping("/plain")
        Map<String, Object> plain(@RequestBody Map<String, Object> body) {
            return Map.of("ok", true);
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void validMapBodyPasses() throws Exception {
        mvc.perform(post("/map").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"age\":36,\"phone\":\"9876543210\",\"code\":\"XY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"));
    }

    @Test
    void invalidMapBodyReturnsProblemDetail() throws Exception {
        mvc.perform(post("/map").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"age\":17,\"phone\":\"123\",\"code\":\"xy\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.form").value("signup"))
                .andExpect(jsonPath("$.detail").value("4 field(s) failed validation"))
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("string.minLength"))
                .andExpect(jsonPath("$.errors[1].code").value("number.min"))
                .andExpect(jsonPath("$.errors[2].code").value("phone.digits"))
                .andExpect(jsonPath("$.errors[3].code").value("uppercase"));
    }

    @Test
    void recordBodyIsConvertedAndValidated() throws Exception {
        mvc.perform(post("/record").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"age\":36}"))
                .andExpect(status().isOk());
        mvc.perform(post("/record").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":36}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("required"));
    }

    @Test
    void unannotatedEndpointIsUntouched() throws Exception {
        mvc.perform(post("/plain").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }
}

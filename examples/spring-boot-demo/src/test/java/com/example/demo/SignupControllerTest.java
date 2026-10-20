package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SignupControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void acceptsValidSignup() throws Exception {
        mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada Lovelace\",\"age\":36,\"phone\":\"+91 98765 43210\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    void rejectsInvalidSignupWithProblemDetail() throws Exception {
        mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A1\",\"age\":17,\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.form").value("signup"))
                .andExpect(jsonPath("$.errors[0].message").value("Name must be 2-50 letters"))
                .andExpect(jsonPath("$.errors[1].code").value("number.min"))
                .andExpect(jsonPath("$.errors[2].code").value("required"))
                .andExpect(jsonPath("$.errors[3].code").value("email.format"));
    }

    @Test
    void programmaticEndpointReportsWithoutThrowing() throws Exception {
        mvc.perform(post("/signup/check").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }
}

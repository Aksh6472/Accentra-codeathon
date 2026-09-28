package com.accentra.leavemanagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the full application on H2 with the demo seed data and talks to it over MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    static final String PASSWORD = "Demo@1234";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    ResultActions getAs(String token, String url, Object... vars) throws Exception {
        return mvc.perform(auth(get(url, vars), token));
    }

    ResultActions postAs(String token, String url, Object body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder request = auth(post(url, vars), token).contentType(MediaType.APPLICATION_JSON);
        return mvc.perform(body == null ? request : request.content(json(body)));
    }

    ResultActions putAs(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(auth(put(url, vars), token).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    long leaveTypeId(String token, String code) throws Exception {
        for (JsonNode policy : read(getAs(token, "/api/policies"))) {
            if (policy.get("leaveTypeCode").asText().equals(code)) {
                return policy.get("leaveTypeId").asLong();
            }
        }
        throw new IllegalStateException("No leave type " + code);
    }

    String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}

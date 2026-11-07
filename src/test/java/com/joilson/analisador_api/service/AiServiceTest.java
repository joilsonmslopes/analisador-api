package com.joilson.analisador_api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.joilson.analisador_api.enums.SentimentEnum;

class AiServiceTest {
    private AiService service;
    private MockRestServiceServer server;
    private static final String URL = "https://gemini.invalid/generate?key=test-key";

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build();
        // Intercept the existing constructor without changing production injection or using a network server.
        try (var factory = mockStatic(RestClient.class)) {
            factory.when(RestClient::create).thenReturn(client);
            service = new AiService();
        }
        ReflectionTestUtils.setField(service, "apiUrl", "https://gemini.invalid/generate");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
    }

    @ParameterizedTest
    @CsvSource({"POSITIVO,POSITIVO", "Negativo,NEGATIVO", "NEUTRO,NEUTRO", "unexpected,NEUTRO"})
    void mapsProviderLabelsAndSendsTheCurrentPrompt(String label, SentimentEnum expected) {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.endsWith("Avaliação: Bom")))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + label + "\"}]}}]}", MediaType.APPLICATION_JSON));
        assertEquals(expected, service.analyzeSentiment("Bom")); server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"candidates\":[]}", "{\"candidates\":[{\"content\":null}]}",
            "{\"candidates\":[{\"content\":{\"parts\":[]}}]}", "not-json"})
    void absentOrMalformedProviderResponsesCurrentlyBecomeNeutral(String body) {
        server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertEquals(SentimentEnum.NEUTRO, service.analyzeSentiment("Bom")); server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void providerHttpFailuresCurrentlyBecomeNeutral(int status) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertEquals(SentimentEnum.NEUTRO, service.analyzeSentiment("Bom")); server.verify();
    }

    @Test
    void missingUrlCurrentlyThrowsBeforeTheFallback() {
        ReflectionTestUtils.setField(service, "apiUrl", null);
        assertThrows(NullPointerException.class, () -> service.analyzeSentiment("Bom")); server.verify();
    }
}

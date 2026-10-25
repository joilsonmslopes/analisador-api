package com.joilson.analisador_api.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.joilson.analisador_api.dto.gemini.GeminiResponse;

@Service 
public class AiService {
    private final RestClient restClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public AiService() {
        this.restClient = RestClient.create();
    }

    public String analyzeSentiment(String ratingText) {
        String prompt = "Analise o sentimento da seguinte avaliação de um produto e responda APENAS com uma destas três palavras: POSITIVO, NEGATIVO ou NEUTRO. Avaliação: " + ratingText;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        String safeUrl = Objects.requireNonNull(apiUrl, "A URL do Gemini não foi configurada no propertie");

        URI uri = UriComponentsBuilder.fromHttpUrl(safeUrl)
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();
        
        Map<String, Object> safeRequestBody = Objects.requireNonNull(requestBody, "O Objeto requestBody não está preenchido corretamente.");

        try {
            GeminiResponse response = restClient.post()
                                    .uri(uri)
                                    .body(safeRequestBody)
                                    .retrieve()
                                    .body(GeminiResponse.class);

            if (Objects.isNull(response) || Objects.isNull(response.candidates()) || response.candidates().isEmpty()) {
                return "NEUTRO";
            }
            
            return response.candidates().get(0).content().parts().get(0).text().trim();
        } catch (Exception e) {
            System.err.println("Erro ao chamar a API do gemini" + e.getMessage());
            return "NEUTRO";
        }
    }
}


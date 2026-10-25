package com.joilson.analisador_api.dto.gemini;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {
    
}

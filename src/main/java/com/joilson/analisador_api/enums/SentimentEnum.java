package com.joilson.analisador_api.enums;

public enum SentimentEnum {
    POSITIVO("Positivo"),
    NEGATIVO("Negativo"),
    NEUTRO("Neutro");

    private String label;

    private SentimentEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SentimentEnum fromValue(String value) {
        if (value.isBlank() || value.isEmpty()) {
            return SentimentEnum.NEUTRO;
        }
        
        var sanitizedText = value.replaceAll("[^a-zA-Z]", "").toUpperCase(); 

        try {
            return SentimentEnum.valueOf(sanitizedText);
        } catch (IllegalArgumentException e) {
            return SentimentEnum.NEUTRO;
        }
    }
}

package com.joilson.analisador_api.dto;

import com.joilson.analisador_api.enums.SentimentEnum;

import lombok.Data;

@Data 
public class RatingDto {
    private String originalText;
    private SentimentEnum sentiment;
}

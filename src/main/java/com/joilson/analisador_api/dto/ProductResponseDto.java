package com.joilson.analisador_api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data 
public class ProductResponseDto {
    private Long id;
    private String name;
    private BigDecimal currentPrice;
    private String originalLink;
    private List<RatingDto> ratings;
}

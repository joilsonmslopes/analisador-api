package com.joilson.analisador_api.dto.scrape;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ScrapedProductDto {
    private String name;
    private BigDecimal currentPrice;
    private String originalLink;
    private List<String> ratings;
}

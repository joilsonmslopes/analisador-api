package com.joilson.analisador_api.mapper;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.dto.ProductResponseDto;
import com.joilson.analisador_api.dto.RatingDto;
import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.enums.SentimentEnum;
import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.model.Rating;

@Component 
public class ProductMapper {
    public Product toEntity(ScrapedProductDto dto) {
        Product entity = new Product();
        if (dto == null) {
            return null;
        }

        entity.setName(dto.getName());
        entity.setCurrentPrice(dto.getCurrentPrice());
        entity.setOriginalLink(dto.getOriginalLink());

        return entity;
    }

    public ProductResponseDto toResponseDto(Product entity) {
        if (entity == null) {
            return null;
        }

        var dto = new ProductResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCurrentPrice(entity.getCurrentPrice());
        dto.setOriginalLink(entity.getOriginalLink());

        if (entity.getRatings() != null) {
            dto.setRatings(
                entity.getRatings().stream()
                    .map(this::toRatingDto)
                    .toList()
            );
        }

        return dto;
    }

    private RatingDto toRatingDto(Rating entity) {
        if (entity == null) {
            return null;
        }

        var dto = new RatingDto();
        dto.setOriginalText(entity.getOriginalText());
        
        if (entity.getSentiment() != null) {
            dto.setSentiment(SentimentEnum.fromValue(entity.getSentiment()));
        }

        return dto;
    }

}

package com.joilson.analisador_api.mapper;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.model.Product;

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


}

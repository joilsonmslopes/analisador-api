package com.joilson.analisador_api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.enums.SentimentEnum;
import com.joilson.analisador_api.mapper.ProductMapper;
import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.model.Rating;
import com.joilson.analisador_api.repository.ProductRepository;
import com.joilson.analisador_api.service.scraper.ScraperService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final ScraperService scraperService;

    private final AiService aiService;

    private final ProductMapper productMapper;

    public Product createFromUrl(String url) {
        ScrapedProductDto scrapedProductDto = scraperService.scrapeMercadoLivre(url);

        Product product = productMapper.toEntity(scrapedProductDto);
        List<Rating> ratings = new ArrayList<>();

        scrapedProductDto.getRatings().forEach(rating -> {
            Rating ratingEntity = new Rating();
            SentimentEnum analyzedSentiment = aiService.analyzeSentiment(rating);

            ratingEntity.setOriginalText(rating);
            ratingEntity.setProduct(product);
            ratingEntity.setSentiment(analyzedSentiment.getLabel());

            ratings.add(ratingEntity);
        });

        product.setRatings(ratings);

        return productRepository.save(product);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
}

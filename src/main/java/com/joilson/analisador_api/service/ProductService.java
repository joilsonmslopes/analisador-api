package com.joilson.analisador_api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import com.joilson.analisador_api.dto.RatingDto;
import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
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
        ScrapedProductDto scrapedProductDto = scraperService.scrape(url);

        Product product = productMapper.toEntity(scrapedProductDto);
        List<Rating> ratings = new ArrayList<>();

        var ratingsDto = getSentimentByGemini(scrapedProductDto.getRatings());

        for (RatingDto ratingDto : ratingsDto) {
            Rating rating = new Rating();
            rating.setOriginalText(ratingDto.getOriginalText());
            rating.setProduct(product);
            rating.setSentiment(ratingDto.getSentiment().getLabel());

            ratings.add(rating);
        }

        product.setRatings(ratings);

        return productRepository.save(product);
    }

    private List<RatingDto> getSentimentByGemini(List<String> prompts) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RatingDto>> futures = prompts.stream()
                    .map(rating -> executor.submit(() -> {
                        var sentiment = aiService.analyzeSentiment(rating);

                        RatingDto ratingDto = new RatingDto();

                        ratingDto.setOriginalText(rating);
                        ratingDto.setSentiment(sentiment);

                        return ratingDto;
                    }))
                    .toList();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            System.err.println("Erro ao consultar Gemini: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

}

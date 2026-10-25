package com.joilson.analisador_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.repository.ProductRepository;
import com.joilson.analisador_api.service.scraper.ScraperService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ScraperService scraperService;

    public Product createFromUrl(String url) {
        Product scrapedProduct = scraperService.scrapeMercadoLivre(url);

        return productRepository.save(scrapedProduct);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
}

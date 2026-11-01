package com.joilson.analisador_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joilson.analisador_api.dto.ProductResponseDto;
import com.joilson.analisador_api.enums.ProductSortOption;
import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/products")
@RequiredArgsConstructor 
public class ProductController {
    
    @Autowired 
    private ProductService productService;

    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "NAME_ASC") ProductSortOption sort
    ) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, sort));
    }

    @PostMapping("/scrape")
    public Product scrapeAndSave(@RequestParam String url) {
        return productService.createFromUrl(url);
    }
}

package com.joilson.analisador_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joilson.analisador_api.model.Product;

@Repository 
public interface ProductRepository extends JpaRepository<Product, Long>{
    
}

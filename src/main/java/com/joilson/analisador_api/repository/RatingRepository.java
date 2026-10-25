package com.joilson.analisador_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joilson.analisador_api.model.Rating;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
}

package com.joilson.analisador_api.enums;

import org.springframework.data.domain.Sort;

import io.micrometer.common.lang.NonNull;

public enum ProductSortOption {
    NAME_ASC(Sort.by(Sort.Direction.ASC, "name")),
    NAME_DESC(Sort.by(Sort.Direction.DESC, "name")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "currentPrice")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "currentPrice"));

    private final Sort sort;

    ProductSortOption(Sort sort) {
        this.sort = sort;
    }

    @NonNull 
    public Sort getSort() {
        return sort;
    }
}

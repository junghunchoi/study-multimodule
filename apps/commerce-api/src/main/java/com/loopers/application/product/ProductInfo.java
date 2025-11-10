package com.loopers.application.product;

import com.loopers.domain.product.Product;

public record ProductInfo(Long id, String name, Long price, Integer stock) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock()
        );
    }
}

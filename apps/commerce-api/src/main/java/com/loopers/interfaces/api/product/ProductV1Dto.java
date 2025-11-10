package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;

import java.util.List;
import java.util.stream.Collectors;

public class ProductV1Dto {

    public record ProductResponse(Long id, String name, Long price, Integer stock) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.price(),
                info.stock()
            );
        }
    }

    public record ProductListResponse(List<ProductResponse> products) {
        public static ProductListResponse from(List<ProductInfo> productInfos) {
            List<ProductResponse> products = productInfos.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
            return new ProductListResponse(products);
        }
    }

    public record CreateProductRequest(String name, Long price, Integer stock) {}
}

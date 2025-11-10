package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;

    public ProductInfo getProduct(Long productId) {
        Product product = productService.getProduct(productId);
        return ProductInfo.from(product);
    }

    public List<ProductInfo> getAllProducts() {
        return productService.getAllProducts().stream()
            .map(ProductInfo::from)
            .collect(Collectors.toList());
    }

    public ProductInfo createProduct(String name, Long price, Integer stock) {
        Product product = productService.createProduct(name, price, stock);
        return ProductInfo.from(product);
    }
}

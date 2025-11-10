package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> find(Long id);
    Optional<Product> findWithLock(Long id);
    List<Product> findAll();
    Product save(Product product);
}

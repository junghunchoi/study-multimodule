package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

/**
 * 주문 항목 엔티티
 */
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long price;

    public OrderItem(Product product, Integer quantity) {
        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 비어있을 수 없습니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }

        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice();
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Long getPrice() {
        return price;
    }

    /**
     * 총 가격 계산
     * @return 수량 * 단가
     */
    public Long getTotalPrice() {
        return this.price * this.quantity;
    }

    /**
     * 주문 설정 (양방향 관계 설정용)
     * @param order 주문
     */
    void setOrder(Order order) {
        this.order = order;
    }
}

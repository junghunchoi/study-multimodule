package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    protected Order() {}

    public Order(User user) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 비어있을 수 없습니다.");
        }
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0L;
    }

    public User getUser() {
        return user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getOrderItems() {
        return new ArrayList<>(orderItems);
    }

    /**
     * 주문 항목 추가
     * @param orderItem 주문 항목
     */
    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 비어있을 수 없습니다.");
        }
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
        this.totalAmount += orderItem.getTotalPrice();
    }

    /**
     * 주문 결제 처리
     */
    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "대기 중인 주문만 결제할 수 있습니다.");
        }
        if (this.orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.");
        }

        // 사용자 포인트 차감
        this.user.usePoint(this.totalAmount);
        this.status = OrderStatus.PAID;
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문입니다.");
        }
        if (this.status == OrderStatus.PAID) {
            // 결제된 주문 취소 시 포인트 환불
            this.user.chargePoint(this.totalAmount);
        }
        this.status = OrderStatus.CANCELLED;
    }
}

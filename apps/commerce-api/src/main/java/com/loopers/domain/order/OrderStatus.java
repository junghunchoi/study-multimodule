package com.loopers.domain.order;

/**
 * 주문 상태
 */
public enum OrderStatus {
    PENDING,     // 대기
    PAID,        // 결제 완료
    CANCELLED    // 취소
}

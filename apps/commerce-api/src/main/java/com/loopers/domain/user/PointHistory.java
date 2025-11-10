package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

/**
 * 포인트 이력 엔티티
 */
@Entity
@Table(name = "point_history")
public class PointHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    protected PointHistory() {}

    public PointHistory(User user, PointTransactionType type, Long amount, Long balanceAfter) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 비어있을 수 없습니다.");
        }
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "거래 유형은 비어있을 수 없습니다.");
        }
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0보다 커야 합니다.");
        }
        if (balanceAfter == null || balanceAfter < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "잔액은 0보다 크거나 같아야 합니다.");
        }

        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public User getUser() {
        return user;
    }

    public PointTransactionType getType() {
        return type;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }
}

package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    private String name;
    private Long point;

    protected User() {}

    public User(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 이름은 비어있을 수 없습니다.");
        }
        this.name = name;
        this.point = 0L;
    }

    public String getName() {
        return name;
    }

    public Long getPoint() {
        return point;
    }

    /**
     * 포인트 충전
     * @param amount 충전할 금액
     */
    public void chargePoint(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    /**
     * 포인트 사용
     * @param amount 사용할 금액
     */
    public void usePoint(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다. 현재 포인트: " + this.point);
        }
        this.point -= amount;
    }
}

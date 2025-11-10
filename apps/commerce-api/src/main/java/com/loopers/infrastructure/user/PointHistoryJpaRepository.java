package com.loopers.infrastructure.user;

import com.loopers.domain.user.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserId(Long userId);
}

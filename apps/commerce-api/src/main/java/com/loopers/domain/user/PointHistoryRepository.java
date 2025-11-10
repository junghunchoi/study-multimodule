package com.loopers.domain.user;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long userId);
}

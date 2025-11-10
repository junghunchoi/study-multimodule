package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.find(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public User createUser(String name) {
        User user = new User(name);
        return userRepository.save(user);
    }

    @Transactional
    public void chargePoint(Long userId, Long amount) {
        User user = getUser(userId);
        user.chargePoint(amount);

        // 포인트 이력 저장
        PointHistory history = new PointHistory(
            user,
            PointTransactionType.CHARGE,
            amount,
            user.getPoint()
        );
        pointHistoryRepository.save(history);
    }

    @Transactional
    public void usePoint(Long userId, Long amount) {
        User user = getUser(userId);
        user.usePoint(amount);

        // 포인트 이력 저장
        PointHistory history = new PointHistory(
            user,
            PointTransactionType.USE,
            amount,
            user.getPoint()
        );
        pointHistoryRepository.save(history);
    }
}

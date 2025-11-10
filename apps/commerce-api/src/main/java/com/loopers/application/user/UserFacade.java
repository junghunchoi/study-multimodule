package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo getUser(Long userId) {
        User user = userService.getUser(userId);
        return UserInfo.from(user);
    }

    public UserInfo createUser(String name) {
        User user = userService.createUser(name);
        return UserInfo.from(user);
    }

    public void chargePoint(Long userId, Long amount) {
        userService.chargePoint(userId, amount);
    }

    public UserInfo getUserPoint(Long userId) {
        User user = userService.getUser(userId);
        return UserInfo.from(user);
    }
}

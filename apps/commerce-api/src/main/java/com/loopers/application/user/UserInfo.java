package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(Long id, String name, Long point) {
    public static UserInfo from(User user) {
        return new UserInfo(
            user.getId(),
            user.getName(),
            user.getPoint()
        );
    }
}

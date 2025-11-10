package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1Dto {

    public record UserResponse(Long id, String name, Long point) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                info.id(),
                info.name(),
                info.point()
            );
        }
    }

    public record CreateUserRequest(String name) {}

    public record ChargePointRequest(Long amount) {}

    public record PointResponse(Long userId, Long point) {
        public static PointResponse from(UserInfo info) {
            return new PointResponse(
                info.id(),
                info.point()
            );
        }
    }
}

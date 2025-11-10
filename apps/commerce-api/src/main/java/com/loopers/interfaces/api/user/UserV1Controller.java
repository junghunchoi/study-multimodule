package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller {

    private final UserFacade userFacade;

    @GetMapping("/{userId}")
    public ApiResponse<UserV1Dto.UserResponse> getUser(
        @PathVariable(value = "userId") Long userId
    ) {
        UserInfo info = userFacade.getUser(userId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<UserV1Dto.UserResponse> createUser(
        @RequestBody UserV1Dto.CreateUserRequest request
    ) {
        UserInfo info = userFacade.createUser(request.name());
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping("/{userId}/charge")
    public ApiResponse<Void> chargePoint(
        @PathVariable(value = "userId") Long userId,
        @RequestBody UserV1Dto.ChargePointRequest request
    ) {
        userFacade.chargePoint(userId, request.amount());
        return ApiResponse.success(null);
    }

    @GetMapping("/{userId}/point")
    public ApiResponse<UserV1Dto.PointResponse> getUserPoint(
        @PathVariable(value = "userId") Long userId
    ) {
        UserInfo info = userFacade.getUserPoint(userId);
        UserV1Dto.PointResponse response = UserV1Dto.PointResponse.from(info);
        return ApiResponse.success(response);
    }
}

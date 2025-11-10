package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller {

    private final OrderFacade orderFacade;

    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @RequestBody OrderV1Dto.CreateOrderRequest request
    ) {
        Map<Long, Integer> productQuantities = OrderV1Dto.toProductQuantityMap(request.items());
        OrderInfo info = orderFacade.createOrder(request.userId(), productQuantities);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @PathVariable(value = "orderId") Long orderId
    ) {
        OrderInfo info = orderFacade.getOrder(orderId);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<OrderV1Dto.OrderListResponse> getOrdersByUserId(
        @PathVariable(value = "userId") Long userId
    ) {
        List<OrderInfo> orders = orderFacade.getOrdersByUserId(userId);
        OrderV1Dto.OrderListResponse response = OrderV1Dto.OrderListResponse.from(orders);
        return ApiResponse.success(response);
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
        @PathVariable(value = "orderId") Long orderId
    ) {
        orderFacade.payOrder(orderId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
        @PathVariable(value = "orderId") Long orderId
    ) {
        orderFacade.cancelOrder(orderId);
        return ApiResponse.success(null);
    }
}

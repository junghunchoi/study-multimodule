package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private final OrderService orderService;

    public OrderInfo getOrder(Long orderId) {
        Order order = orderService.getOrder(orderId);
        return OrderInfo.from(order);
    }

    public List<OrderInfo> getOrdersByUserId(Long userId) {
        return orderService.getOrdersByUserId(userId).stream()
            .map(OrderInfo::from)
            .collect(Collectors.toList());
    }

    public OrderInfo createOrder(Long userId, Map<Long, Integer> productQuantities) {
        Order order = orderService.createOrder(userId, productQuantities);
        return OrderInfo.from(order);
    }

    public void payOrder(Long orderId) {
        orderService.payOrder(orderId);
    }

    public void cancelOrder(Long orderId) {
        orderService.cancelOrder(orderId);
    }
}

package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.find(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 주문을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> productQuantities) {
        User user = userService.getUser(userId);
        Order order = new Order(user);

        // 주문 항목 추가 및 재고 차감
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productService.getProduct(productId);

            // 재고 차감 (비관적 락 사용)
            productService.decreaseStock(productId, quantity);

            OrderItem orderItem = new OrderItem(product, quantity);
            order.addOrderItem(orderItem);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public void payOrder(Long orderId) {
        Order order = getOrder(orderId);
        order.pay();
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = getOrder(orderId);

        // 주문 취소 시 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            productService.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order.cancel();
    }
}

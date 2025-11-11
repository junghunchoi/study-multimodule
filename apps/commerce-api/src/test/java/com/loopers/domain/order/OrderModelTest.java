package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderModelTest {

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("사용자 ID와 주문 아이템이 유효하면, 정상적으로 생성된다.")
        @Test
        void createsOrder_whenUserIdAndItemsAreValid() {
            // arrange
            Long userId = 1L;
            List<OrderItem> items = List.of(
                new OrderItem(1L, "노트북", 1500000, 2),
                new OrderItem(2L, "마우스", 50000, 1)
            );

            // act
            OrderModel order = new OrderModel(userId, items);

            // assert
            assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getUserId()).isEqualTo(userId),
                () -> assertThat(order.getItems()).hasSize(2),
                () -> assertThat(order.getTotalAmount()).isEqualTo(3050000),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }

        @DisplayName("사용자 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenUserIdIsNull() {
            // arrange
            Long userId = null;
            List<OrderItem> items = List.of(new OrderItem(1L, "노트북", 1500000, 1));

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new OrderModel(userId, items);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("주문 아이템이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenItemsAreEmpty() {
            // arrange
            Long userId = 1L;
            List<OrderItem> items = List.of();

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new OrderModel(userId, items);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("총 주문 금액을 계산할 때, ")
    @Nested
    class CalculateTotalAmount {

        @DisplayName("모든 아이템의 가격을 합산한다.")
        @Test
        void calculatesSumOfAllItems() {
            // arrange
            List<OrderItem> items = List.of(
                new OrderItem(1L, "노트북", 1500000, 2),  // 3,000,000
                new OrderItem(2L, "마우스", 50000, 3),     // 150,000
                new OrderItem(3L, "키보드", 100000, 1)     // 100,000
            );
            OrderModel order = new OrderModel(1L, items);

            // act
            int totalAmount = order.getTotalAmount();

            // assert
            assertThat(totalAmount).isEqualTo(3250000);
        }

        @DisplayName("아이템이 하나만 있으면, 해당 아이템의 금액을 반환한다.")
        @Test
        void returnsSingleItemAmount_whenOnlyOneItem() {
            // arrange
            List<OrderItem> items = List.of(
                new OrderItem(1L, "노트북", 1500000, 1)
            );
            OrderModel order = new OrderModel(1L, items);

            // act
            int totalAmount = order.getTotalAmount();

            // assert
            assertThat(totalAmount).isEqualTo(1500000);
        }
    }

    @DisplayName("주문 상태를 변경할 때, ")
    @Nested
    class ChangeStatus {

        @DisplayName("결제 대기 -> 결제 완료로 변경할 수 있다.")
        @Test
        void changesStatusToPaid_fromPending() {
            // arrange
            OrderModel order = new OrderModel(1L, List.of(
                new OrderItem(1L, "노트북", 1500000, 1)
            ));

            // act
            order.complete();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @DisplayName("결제 대기 -> 취소로 변경할 수 있다.")
        @Test
        void changesStatusToCancelled_fromPending() {
            // arrange
            OrderModel order = new OrderModel(1L, List.of(
                new OrderItem(1L, "노트북", 1500000, 1)
            ));

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("이미 취소된 주문은 완료로 변경할 수 없다.")
        @Test
        void throwsException_whenCompletingCancelledOrder() {
            // arrange
            OrderModel order = new OrderModel(1L, List.of(
                new OrderItem(1L, "노트북", 1500000, 1)
            ));
            order.cancel();

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                order.complete();
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_ORDER_STATUS);
        }
    }

    @DisplayName("주문 아이템을 생성할 때, ")
    @Nested
    class CreateOrderItem {

        @DisplayName("모든 필드가 유효하면, 정상적으로 생성된다.")
        @Test
        void createsOrderItem_whenAllFieldsAreValid() {
            // arrange
            Long productId = 1L;
            String productName = "노트북";
            int price = 1500000;
            int quantity = 2;

            // act
            OrderItem item = new OrderItem(productId, productName, price, quantity);

            // assert
            assertAll(
                () -> assertThat(item.getProductId()).isEqualTo(productId),
                () -> assertThat(item.getProductName()).isEqualTo(productName),
                () -> assertThat(item.getPrice()).isEqualTo(price),
                () -> assertThat(item.getQuantity()).isEqualTo(quantity),
                () -> assertThat(item.getTotalPrice()).isEqualTo(3000000)
            );
        }

        @DisplayName("상품 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenProductIdIsNull() {
            // arrange
            Long productId = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new OrderItem(productId, "노트북", 1500000, 1);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("수량이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenQuantityIsZeroOrNegative() {
            // arrange
            int quantity = 0;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new OrderItem(1L, "노트북", 1500000, quantity);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenPriceIsZeroOrNegative() {
            // arrange
            int price = -1000;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new OrderItem(1L, "노트북", price, 1);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}

package com.loopers.application.order;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.UserPointModel;
import com.loopers.domain.user.UserPointRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.testcontainers.mysql.MySqlTestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private ProductModel product;
    private UserPointModel userPoint;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        product = new ProductModel("노트북", 1500000, 10);
        productRepository.save(product);

        userPoint = new UserPointModel(1L);
        userPoint.charge(10000000); // 충분한 포인트 충전
        userPointRepository.save(userPoint);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class CreateOrder {

        @DisplayName("유효한 요청이면, 정상적으로 주문이 생성된다.")
        @Test
        void createsOrder_whenRequestIsValid() {
            // arrange
            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );

            // act
            OrderInfo result = orderService.createOrder(request);

            // assert
            assertAll(
                () -> assertThat(result.getOrderId()).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(1L),
                () -> assertThat(result.getTotalAmount()).isEqualTo(3000000),
                () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }

        @DisplayName("주문 생성 시 재고가 차감된다.")
        @Test
        void decreasesStock_whenOrderIsCreated() {
            // arrange
            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 3))
            );
            int initialStock = product.getStock();

            // act
            orderService.createOrder(request);

            // assert
            ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 3);
        }

        @DisplayName("주문 생성 시 포인트가 차감된다.")
        @Test
        void decreasesUserPoint_whenOrderIsCreated() {
            // arrange
            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );
            int initialBalance = userPoint.getBalance();
            int expectedAmount = 3000000;

            // act
            orderService.createOrder(request);

            // assert
            UserPointModel updatedUserPoint = userPointRepository.findById(userPoint.getId()).orElseThrow();
            assertThat(updatedUserPoint.getBalance()).isEqualTo(initialBalance - expectedAmount);
        }

        @DisplayName("재고가 부족하면, INSUFFICIENT_STOCK 예외가 발생한다.")
        @Test
        void throwsInsufficientStockException_whenStockIsInsufficient() {
            // arrange
            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 100))
            );

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                orderService.createOrder(request);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_STOCK);
        }

        @DisplayName("포인트가 부족하면, INSUFFICIENT_BALANCE 예외가 발생한다.")
        @Test
        void throwsInsufficientBalanceException_whenBalanceIsInsufficient() {
            // arrange
            userPoint.use(9999000); // 포인트를 거의 다 사용
            userPointRepository.save(userPoint);

            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                orderService.createOrder(request);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_BALANCE);
        }

        @DisplayName("존재하지 않는 상품으로 주문하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenProductDoesNotExist() {
            // arrange
            OrderCreateRequest request = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(999L, 1))
            );

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                orderService.createOrder(request);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("동시에 주문을 생성할 때, ")
    @Nested
    class ConcurrentOrderCreation {

        @DisplayName("비관적 락을 사용하여 재고 차감 정합성이 보장된다.")
        @Test
        void ensuresStockConsistency_withPessimisticLock() throws InterruptedException {
            // arrange
            int threadCount = 10;
            int orderQuantity = 1;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executorService.execute(() -> {
                    try {
                        OrderCreateRequest request = new OrderCreateRequest(
                            1L,
                            List.of(new OrderItemRequest(product.getId(), orderQuantity))
                        );
                        orderService.createOrder(request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // assert
            ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10), // 초기 재고 10개
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(updatedProduct.getStock()).isEqualTo(0)
            );
        }

        @DisplayName("재고보다 많은 동시 주문 시, 일부는 실패한다.")
        @Test
        void someOrdersFail_whenConcurrentOrdersExceedStock() throws InterruptedException {
            // arrange
            int threadCount = 15; // 재고 10개보다 많은 요청
            int orderQuantity = 1;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executorService.execute(() -> {
                    try {
                        OrderCreateRequest request = new OrderCreateRequest(
                            1L,
                            List.of(new OrderItemRequest(product.getId(), orderQuantity))
                        );
                        orderService.createOrder(request);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.INSUFFICIENT_STOCK) {
                            failCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // assert
            ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(failCount.get()).isEqualTo(5),
                () -> assertThat(updatedProduct.getStock()).isEqualTo(0)
            );
        }
    }

    @DisplayName("주문을 취소할 때, ")
    @Nested
    class CancelOrder {

        @DisplayName("유효한 주문이면, 정상적으로 취소된다.")
        @Test
        void cancelsOrder_whenOrderIsValid() {
            // arrange
            OrderCreateRequest createRequest = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );
            OrderInfo order = orderService.createOrder(createRequest);

            // act
            orderService.cancelOrder(order.getOrderId());

            // assert
            OrderModel cancelledOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("주문 취소 시 재고가 복구된다.")
        @Test
        void restoresStock_whenOrderIsCancelled() {
            // arrange
            OrderCreateRequest createRequest = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 3))
            );
            OrderInfo order = orderService.createOrder(createRequest);
            int stockAfterOrder = productRepository.findById(product.getId()).orElseThrow().getStock();

            // act
            orderService.cancelOrder(order.getOrderId());

            // assert
            ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updatedProduct.getStock()).isEqualTo(stockAfterOrder + 3);
        }

        @DisplayName("주문 취소 시 포인트가 환불된다.")
        @Test
        void refundsPoint_whenOrderIsCancelled() {
            // arrange
            OrderCreateRequest createRequest = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );
            OrderInfo order = orderService.createOrder(createRequest);
            int balanceAfterOrder = userPointRepository.findById(userPoint.getId()).orElseThrow().getBalance();

            // act
            orderService.cancelOrder(order.getOrderId());

            // assert
            UserPointModel updatedUserPoint = userPointRepository.findById(userPoint.getId()).orElseThrow();
            assertThat(updatedUserPoint.getBalance()).isEqualTo(balanceAfterOrder + order.getTotalAmount());
        }

        @DisplayName("이미 취소된 주문은 다시 취소할 수 없다.")
        @Test
        void throwsException_whenOrderIsAlreadyCancelled() {
            // arrange
            OrderCreateRequest createRequest = new OrderCreateRequest(
                1L,
                List.of(new OrderItemRequest(product.getId(), 2))
            );
            OrderInfo order = orderService.createOrder(createRequest);
            orderService.cancelOrder(order.getOrderId());

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                orderService.cancelOrder(order.getOrderId());
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_ORDER_STATUS);
        }
    }
}

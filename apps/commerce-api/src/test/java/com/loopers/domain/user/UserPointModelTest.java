package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPointModelTest {

    @DisplayName("사용자 포인트를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("사용자 ID가 유효하면, 초기 포인트 0으로 생성된다.")
        @Test
        void createsUserPoint_withZeroInitialBalance() {
            // arrange
            Long userId = 1L;

            // act
            UserPointModel userPoint = new UserPointModel(userId);

            // assert
            assertAll(
                () -> assertThat(userPoint.getId()).isNotNull(),
                () -> assertThat(userPoint.getUserId()).isEqualTo(userId),
                () -> assertThat(userPoint.getBalance()).isEqualTo(0)
            );
        }

        @DisplayName("사용자 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenUserIdIsNull() {
            // arrange
            Long userId = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserPointModel(userId);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("포인트를 충전할 때, ")
    @Nested
    class Charge {

        @DisplayName("유효한 금액이면, 정상적으로 충전된다.")
        @Test
        void chargesPoint_whenAmountIsValid() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            int amount = 10000;

            // act
            userPoint.charge(amount);

            // assert
            assertThat(userPoint.getBalance()).isEqualTo(10000);
        }

        @DisplayName("여러 번 충전하면, 누적된다.")
        @Test
        void accumulatesBalance_whenChargedMultipleTimes() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);

            // act
            userPoint.charge(10000);
            userPoint.charge(5000);
            userPoint.charge(3000);

            // assert
            assertThat(userPoint.getBalance()).isEqualTo(18000);
        }

        @DisplayName("충전 금액이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAmountIsZeroOrNegative() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            int amount = 0;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userPoint.charge(amount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("충전 금액이 최대 한도를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAmountExceedsMaxLimit() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            int amount = 1_000_001; // 최대 100만원 초과

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userPoint.charge(amount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("포인트를 사용할 때, ")
    @Nested
    class Use {

        @DisplayName("잔액이 충분하면, 정상적으로 차감된다.")
        @Test
        void usesPoint_whenBalanceIsSufficient() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            userPoint.charge(10000);
            int amount = 3000;

            // act
            userPoint.use(amount);

            // assert
            assertThat(userPoint.getBalance()).isEqualTo(7000);
        }

        @DisplayName("잔액이 부족하면, INSUFFICIENT_BALANCE 예외가 발생한다.")
        @Test
        void throwsInsufficientBalanceException_whenBalanceIsInsufficient() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            userPoint.charge(5000);
            int amount = 10000;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userPoint.use(amount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_BALANCE);
        }

        @DisplayName("사용 금액이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAmountIsZeroOrNegative() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            userPoint.charge(10000);
            int amount = -1000;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userPoint.use(amount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정확히 잔액만큼 사용하면, 잔액이 0이 된다.")
        @Test
        void balanceBecomesZero_whenUsedExactAmount() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            userPoint.charge(10000);

            // act
            userPoint.use(10000);

            // assert
            assertThat(userPoint.getBalance()).isEqualTo(0);
        }
    }

    @DisplayName("포인트를 환불할 때, ")
    @Nested
    class Refund {

        @DisplayName("유효한 금액이면, 정상적으로 환불된다.")
        @Test
        void refundsPoint_whenAmountIsValid() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            userPoint.charge(10000);
            userPoint.use(3000);

            // act
            userPoint.refund(3000);

            // assert
            assertThat(userPoint.getBalance()).isEqualTo(10000);
        }

        @DisplayName("환불 금액이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAmountIsZeroOrNegative() {
            // arrange
            UserPointModel userPoint = new UserPointModel(1L);
            int amount = 0;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userPoint.refund(amount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}

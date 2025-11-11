package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductModelTest {

    @DisplayName("상품을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("상품명, 가격, 재고가 모두 유효하면, 정상적으로 생성된다.")
        @Test
        void createsProduct_whenAllFieldsAreValid() {
            // arrange
            String name = "노트북";
            int price = 1500000;
            int stock = 100;

            // act
            ProductModel product = new ProductModel(name, price, stock);

            // assert
            assertAll(
                () -> assertThat(product.getId()).isNotNull(),
                () -> assertThat(product.getName()).isEqualTo(name),
                () -> assertThat(product.getPrice()).isEqualTo(price),
                () -> assertThat(product.getStock()).isEqualTo(stock)
            );
        }

        @DisplayName("상품명이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenNameIsEmpty() {
            // arrange
            String name = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new ProductModel(name, 1000, 10);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenPriceIsZeroOrNegative() {
            // arrange
            int price = 0;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new ProductModel("상품", price, 10);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 음수면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenStockIsNegative() {
            // arrange
            int stock = -1;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new ProductModel("상품", 1000, stock);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class DecreaseStock {

        @DisplayName("재고가 충분하면, 정상적으로 차감된다.")
        @Test
        void decreasesStock_whenStockIsSufficient() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 100);
            int quantity = 10;

            // act
            product.decreaseStock(quantity);

            // assert
            assertThat(product.getStock()).isEqualTo(90);
        }

        @DisplayName("재고가 부족하면, INSUFFICIENT_STOCK 예외가 발생한다.")
        @Test
        void throwsInsufficientStockException_whenStockIsInsufficient() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 5);
            int quantity = 10;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                product.decreaseStock(quantity);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_STOCK);
        }

        @DisplayName("차감 수량이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenQuantityIsZeroOrNegative() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 100);
            int quantity = 0;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                product.decreaseStock(quantity);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정확히 남은 재고만큼 차감하면, 재고가 0이 된다.")
        @Test
        void stockBecomesZero_whenDecreasedByExactAmount() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 10);
            int quantity = 10;

            // act
            product.decreaseStock(quantity);

            // assert
            assertThat(product.getStock()).isEqualTo(0);
        }
    }

    @DisplayName("재고를 증가시킬 때, ")
    @Nested
    class IncreaseStock {

        @DisplayName("유효한 수량이면, 정상적으로 증가한다.")
        @Test
        void increasesStock_whenQuantityIsValid() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 10);
            int quantity = 20;

            // act
            product.increaseStock(quantity);

            // assert
            assertThat(product.getStock()).isEqualTo(30);
        }

        @DisplayName("증가 수량이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenQuantityIsZeroOrNegative() {
            // arrange
            ProductModel product = new ProductModel("노트북", 1500000, 10);
            int quantity = -5;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                product.increaseStock(quantity);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}

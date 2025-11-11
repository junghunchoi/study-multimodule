-- ============================================================
-- Sample Data for Commerce API
-- Generated based on domain entities
-- ============================================================

-- Set timezone to Asia/Seoul
SET time_zone = '+09:00';

-- ============================================================
-- 1. Example Data (example table)
-- ============================================================
INSERT INTO example (name, description, created_at, updated_at, deleted_at) VALUES
('Example 1', '첫 번째 예제 데이터입니다.', NOW(), NOW(), NULL),
('Example 2', '두 번째 예제 데이터입니다.', NOW(), NOW(), NULL),
('Example 3', '세 번째 예제 데이터입니다.', NOW(), NOW(), NULL),
('Example 4', '네 번째 예제 데이터입니다.', NOW(), NOW(), NULL),
('Deleted Example', '삭제된 예제 데이터입니다.', NOW(), NOW(), NOW());

-- ============================================================
-- 2. User Data (users table)
-- ============================================================
INSERT INTO users (name, point, created_at, updated_at, deleted_at) VALUES
('김철수', 100000, NOW(), NOW(), NULL),
('이영희', 50000, NOW(), NOW(), NULL),
('박민수', 200000, NOW(), NOW(), NULL),
('최지은', 75000, NOW(), NOW(), NULL),
('정우성', 150000, NOW(), NOW(), NULL),
('한지민', 0, NOW(), NOW(), NULL),
('이병헌', 300000, NOW(), NOW(), NULL),
('송혜교', 125000, NOW(), NOW(), NULL),
('공유', 80000, NOW(), NOW(), NULL),
('전지현', 95000, NOW(), NOW(), NULL);

-- ============================================================
-- 3. Point History Data (point_history table)
-- ============================================================
-- 김철수 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(1, 'CHARGE', 50000, 50000, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NULL),
(1, 'CHARGE', 100000, 150000, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
(1, 'USE', 50000, 100000, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL);

-- 이영희 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(2, 'CHARGE', 100000, 100000, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL),
(2, 'USE', 50000, 50000, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL);

-- 박민수 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(3, 'CHARGE', 200000, 200000, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), NULL),
(3, 'CHARGE', 100000, 300000, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NULL),
(3, 'USE', 100000, 200000, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL);

-- 최지은 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(4, 'CHARGE', 100000, 100000, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL),
(4, 'USE', 25000, 75000, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

-- 정우성 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(5, 'CHARGE', 150000, 150000, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL);

-- 이병헌 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(7, 'CHARGE', 500000, 500000, DATE_SUB(NOW(), INTERVAL 50 DAY), DATE_SUB(NOW(), INTERVAL 50 DAY), NULL),
(7, 'USE', 200000, 300000, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL);

-- 송혜교 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(8, 'CHARGE', 125000, 125000, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NULL);

-- 공유 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(9, 'CHARGE', 100000, 100000, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NULL),
(9, 'USE', 20000, 80000, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL);

-- 전지현 포인트 이력
INSERT INTO point_history (user_id, type, amount, balance_after, created_at, updated_at, deleted_at) VALUES
(10, 'CHARGE', 150000, 150000, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NULL),
(10, 'USE', 55000, 95000, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NULL);

-- ============================================================
-- 4. Product Data (products table)
-- ============================================================
INSERT INTO products (name, price, stock, version, created_at, updated_at, deleted_at) VALUES
-- 전자제품
('삼성 갤럭시 S24', 1200000, 50, 0, NOW(), NOW(), NULL),
('애플 아이폰 15 Pro', 1500000, 30, 0, NOW(), NOW(), NULL),
('LG 그램 노트북', 1800000, 20, 0, NOW(), NOW(), NULL),
('삼성 갤럭시 탭 S9', 800000, 40, 0, NOW(), NOW(), NULL),
('애플 에어팟 Pro 2', 350000, 100, 0, NOW(), NOW(), NULL),

-- 의류
('나이키 에어맥스 운동화', 180000, 150, 0, NOW(), NOW(), NULL),
('아디다스 기본 티셔츠', 45000, 200, 0, NOW(), NOW(), NULL),
('리바이스 청바지', 120000, 80, 0, NOW(), NOW(), NULL),
('노스페이스 패딩', 450000, 60, 0, NOW(), NOW(), NULL),
('뉴발란스 996', 150000, 90, 0, NOW(), NOW(), NULL),

-- 가전제품
('다이슨 무선청소기 V15', 950000, 25, 0, NOW(), NOW(), NULL),
('삼성 비스포크 냉장고', 2500000, 15, 0, NOW(), NOW(), NULL),
('LG 트롬 세탁기', 1300000, 18, 0, NOW(), NOW(), NULL),
('쿠쿠 전기압력밥솥', 250000, 50, 0, NOW(), NOW(), NULL),
('브리타 정수기', 80000, 70, 0, NOW(), NOW(), NULL),

-- 생활용품
('템퍼 베개', 65000, 120, 0, NOW(), NOW(), NULL),
('에어랩 이불', 120000, 80, 0, NOW(), NOW(), NULL),
('모던하우스 책상', 350000, 30, 0, NOW(), NOW(), NULL),
('시디즈 의자', 580000, 25, 0, NOW(), NOW(), NULL),
('필립스 스탠드', 95000, 60, 0, NOW(), NOW(), NULL),

-- 식품
('스타벅스 원두 1kg', 35000, 200, 0, NOW(), NOW(), NULL),
('동원참치 200g x 10캔', 28000, 300, 0, NOW(), NOW(), NULL),
('오뚜기 진라면 멀티팩', 12000, 500, 0, NOW(), NOW(), NULL),
('백설 요리당 1.8L', 8000, 250, 0, NOW(), NOW(), NULL),
('풀무원 두부 300g x 5개', 7500, 400, 0, NOW(), NOW(), NULL),

-- 재고 부족 상품
('한정판 스니커즈', 450000, 3, 0, NOW(), NOW(), NULL),
('품절 임박 게임 콘솔', 680000, 5, 0, NOW(), NOW(), NULL),

-- 삭제된 상품
('단종된 구형 모델', 100000, 0, 0, NOW(), NOW(), NOW());

-- ============================================================
-- 5. Order Data (orders table)
-- ============================================================
INSERT INTO orders (user_id, status, total_amount, created_at, updated_at, deleted_at) VALUES
-- 김철수의 주문들
(1, 'PAID', 350000, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),
(1, 'PAID', 180000, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
(1, 'PENDING', 120000, NOW(), NOW(), NULL),

-- 이영희의 주문들
(2, 'PAID', 1500000, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL),
(2, 'CANCELLED', 250000, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NULL),

-- 박민수의 주문들
(3, 'PAID', 1800000, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
(3, 'PAID', 950000, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NULL),
(3, 'PAID', 450000, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), NULL),

-- 최지은의 주문들
(4, 'PAID', 65000, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),

-- 정우성의 주문들
(5, 'PENDING', 800000, NOW(), NOW(), NULL),

-- 이병헌의 주문들
(7, 'PAID', 2500000, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL),
(7, 'PAID', 1300000, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NULL),

-- 공유의 주문들
(9, 'PAID', 95000, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),

-- 전지현의 주문들
(10, 'PAID', 700000, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NULL),
(10, 'CANCELLED', 350000, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL);

-- ============================================================
-- 6. Order Item Data (order_items table)
-- ============================================================
-- 주문 1번 (김철수 - PAID 350000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(1, 5, 1, 350000, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL);

-- 주문 2번 (김철수 - PAID 180000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(2, 6, 1, 180000, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

-- 주문 3번 (김철수 - PENDING 120000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(3, 8, 1, 120000, NOW(), NOW(), NULL);

-- 주문 4번 (이영희 - PAID 1500000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(4, 2, 1, 1500000, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL);

-- 주문 5번 (이영희 - CANCELLED 250000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(5, 14, 1, 250000, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NULL);

-- 주문 6번 (박민수 - PAID 1800000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(6, 3, 1, 1800000, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL);

-- 주문 7번 (박민수 - PAID 950000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(7, 11, 1, 950000, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NULL);

-- 주문 8번 (박민수 - PAID 450000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(8, 9, 1, 450000, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), NULL);

-- 주문 9번 (최지은 - PAID 65000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(9, 16, 1, 65000, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

-- 주문 10번 (정우성 - PENDING 800000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(10, 4, 1, 800000, NOW(), NOW(), NULL);

-- 주문 11번 (이병헌 - PAID 2500000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(11, 12, 1, 2500000, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL);

-- 주문 12번 (이병헌 - PAID 1300000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(12, 13, 1, 1300000, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NULL);

-- 주문 13번 (공유 - PAID 95000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(13, 20, 1, 95000, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL);

-- 주문 14번 (전지현 - PAID 700000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(14, 5, 2, 350000, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NULL);

-- 주문 15번 (전지현 - CANCELLED 350000원)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at, updated_at, deleted_at) VALUES
(15, 5, 1, 350000, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL);

-- ============================================================
-- Useful Query Examples
-- ============================================================

-- 1. 사용자별 총 주문 금액 조회
-- SELECT u.name, SUM(o.total_amount) as total_ordered
-- FROM users u
-- LEFT JOIN orders o ON u.id = o.user_id
-- WHERE o.status = 'PAID'
-- GROUP BY u.id, u.name
-- ORDER BY total_ordered DESC;

-- 2. 상품별 판매량 조회
-- SELECT p.name, SUM(oi.quantity) as total_sold
-- FROM products p
-- JOIN order_items oi ON p.id = oi.product_id
-- JOIN orders o ON oi.order_id = o.id
-- WHERE o.status = 'PAID'
-- GROUP BY p.id, p.name
-- ORDER BY total_sold DESC;

-- 3. 사용자별 포인트 거래 내역
-- SELECT u.name, ph.type, ph.amount, ph.balance_after, ph.created_at
-- FROM point_history ph
-- JOIN users u ON ph.user_id = u.id
-- ORDER BY u.name, ph.created_at DESC;

-- 4. 재고 부족 상품 조회 (재고 10개 이하)
-- SELECT name, price, stock
-- FROM products
-- WHERE stock <= 10 AND deleted_at IS NULL
-- ORDER BY stock ASC;

-- 5. 최근 7일간 주문 통계
-- SELECT
--     DATE(created_at) as order_date,
--     COUNT(*) as order_count,
--     SUM(total_amount) as daily_revenue
-- FROM orders
-- WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
--     AND status = 'PAID'
-- GROUP BY DATE(created_at)
-- ORDER BY order_date DESC;

-- 6. 주문 취소율 조회
-- SELECT
--     COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_count,
--     COUNT(*) as total_count,
--     ROUND(COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) * 100.0 / COUNT(*), 2) as cancel_rate
-- FROM orders;

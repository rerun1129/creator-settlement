-- ============================================================
-- 정산 시나리오 검증용 시드 (Creator + Course 7건)
--
-- 재실행 가능: 격리 범위만 DELETE 후 INSERT.
-- ============================================================

-- 1) 격리 범위 정리 (재실행 대비)
DELETE FROM creator WHERE creator_id BETWEEN 1001 AND 1003;
DELETE FROM course WHERE course_id BETWEEN 10001 AND 10004;
DELETE FROM cancellation_record WHERE cancellation_record_id BETWEEN 150001 AND 150003;
DELETE FROM sales_record WHERE sales_record_id BETWEEN 3000001 AND 3000007;

-- 2) Creator 3건
INSERT INTO creator (creator_id, name) VALUES
    ( 1001, '김강사'),
    ( 1002, '이강사'),
    ( 1003, '박강사');

-- 3) Course 4건
INSERT INTO course (course_id, creator_id, title) VALUES
    (10001, 1001, 'Spring Boot 입문'),
    (10002, 1001, 'JPA 실전'),
    (10003, 1002, 'Kotlin 기초'),
    (10004, 1003, 'MSA 설계');

-- 4) SalesRecord 7건 (paidAt: +09:00 제거 후 KST 그대로)
--    sale-1 → course 10001(creator 1001) / 50,000  / 2025-03-05
--    sale-2 → course 10001(creator 1001) / 50,000  / 2025-03-15
--    sale-3 → course 10002(creator 1001) / 80,000  / 2025-03-20  (cancel-1 전액 환불 대상)
--    sale-4 → course 10002(creator 1001) / 80,000  / 2025-03-22  (cancel-2 부분 환불 대상)
--    sale-5 → course 10003(creator 1002) / 60,000  / 2025-01-31  (월 경계: 1월 결제 / cancel-3 2월 취소)
--    sale-6 → course 10003(creator 1002) / 60,000  / 2025-03-10
--    sale-7 → course 10004(creator 1003) / 120,000 / 2025-02-14  (creator-3 빈 월 조회 검증용)
INSERT INTO sales_record (sales_record_id, course_id, student_id, payment_amount, paid_at) VALUES
    (3000001, 10001, 300001,  50000, '2025-03-05 10:00:00'),
    (3000002, 10001, 300002,  50000, '2025-03-15 14:30:00'),
    (3000003, 10002, 300003,  80000, '2025-03-20 09:00:00'),
    (3000004, 10002, 300004,  80000, '2025-03-22 11:00:00'),
    (3000005, 10003, 300005,  60000, '2025-01-31 23:30:00'),
    (3000006, 10003, 300006,  60000, '2025-03-10 16:00:00'),
    (3000007, 10004, 300007, 120000, '2025-02-14 10:00:00');

-- 5) CancellationRecord 3건
--    sales_record_id는 student_id로 역조회 (sales_record AUTO_INCREMENT 시작 번호 가정 불필요)
--    cancel-1 → sale-3 전액 80,000 / 2025-03-23  (creator-1 3월 환불 합계 110,000의 80,000)
--    cancel-2 → sale-4 부분 30,000 / 2025-03-25  (creator-1 3월 환불 합계 110,000의 30,000)
--    cancel-3 → sale-5 전액 60,000 / 2025-02-01  (월 경계: 1월 결제 / 2월 취소)
INSERT INTO cancellation_record (cancellation_record_id, sales_record_id, refund_amount, cancelled_at)
SELECT 150001, sales_record_id, 80000, '2025-03-23 10:00:00' FROM sales_record WHERE student_id = 300003;

INSERT INTO cancellation_record (cancellation_record_id, sales_record_id, refund_amount, cancelled_at)
SELECT 150002, sales_record_id, 30000, '2025-03-25 10:00:00' FROM sales_record WHERE student_id = 300004;

INSERT INTO cancellation_record (cancellation_record_id, sales_record_id, refund_amount, cancelled_at)
SELECT 150003, sales_record_id, 60000, '2025-02-01 09:00:00' FROM sales_record WHERE student_id = 300005;
CREATE TABLE IF NOT EXISTS course (
    id          BIGINT       NOT NULL PRIMARY KEY,
    creator_id  BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_course_creator ON course (creator_id);

CREATE TABLE IF NOT EXISTS sales_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT        NOT NULL,
    student_id      BIGINT        NOT NULL,
    payment_amount  DECIMAL(19,2) NOT NULL,
    paid_at         DATETIME(6)   NOT NULL,
    CONSTRAINT fk_sales_course FOREIGN KEY (course_id) REFERENCES course (id)
);
CREATE INDEX IF NOT EXISTS idx_sales_paid_at ON sales_record (paid_at);
CREATE INDEX IF NOT EXISTS idx_sales_course  ON sales_record (course_id);

CREATE TABLE IF NOT EXISTS cancellation_record (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sales_record_id  BIGINT        NOT NULL,
    refund_amount    DECIMAL(19,2) NOT NULL,
    cancelled_at     DATETIME(6)   NOT NULL,
    CONSTRAINT fk_cancel_sales FOREIGN KEY (sales_record_id) REFERENCES sales_record (id)
);
CREATE INDEX IF NOT EXISTS idx_cancel_sales ON cancellation_record (sales_record_id);

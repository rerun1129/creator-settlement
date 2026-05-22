CREATE TABLE IF NOT EXISTS creator (
    creator_id BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS course (
    course_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES creator (creator_id)
);
CREATE INDEX idx_course_creator ON course (creator_id);

CREATE TABLE IF NOT EXISTS sales_record (
    sales_record_id BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT        NOT NULL,
    student_id      BIGINT        NOT NULL,
    payment_amount  DECIMAL(19,2) NOT NULL,
    paid_at         DATETIME(6)   NOT NULL,
    CONSTRAINT fk_sales_course FOREIGN KEY (course_id) REFERENCES course (course_id)
);
CREATE INDEX idx_sales_paid_at ON sales_record (paid_at);
CREATE INDEX idx_sales_course  ON sales_record (course_id);

CREATE TABLE IF NOT EXISTS cancellation_record (
    cancellation_record_id BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sales_record_id        BIGINT        NOT NULL,
    refund_amount          DECIMAL(19,2) NOT NULL,
    cancelled_at           DATETIME(6)   NOT NULL,
    CONSTRAINT fk_cancel_sales FOREIGN KEY (sales_record_id) REFERENCES sales_record (sales_record_id)
);
CREATE INDEX idx_cancel_sales ON cancellation_record (sales_record_id);
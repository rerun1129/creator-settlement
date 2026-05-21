CREATE TABLE IF NOT EXISTS course (
    id          BIGINT       NOT NULL PRIMARY KEY,
    creator_id  BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_course_creator ON course (creator_id);

ALTER TABLE settlement
    ADD COLUMN paid_at DATETIME(6) NULL AFTER confirmed_at;

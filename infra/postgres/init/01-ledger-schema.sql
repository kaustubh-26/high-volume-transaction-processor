CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ledger_transaction_id
    ON ledger_entries (transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ledger_account_idempotency
    ON ledger_entries (account_id, idempotency_key);

CREATE INDEX IF NOT EXISTS ix_ledger_entries_processed_at
    ON ledger_entries (processed_at);


CREATE TABLE IF NOT EXISTS reconciliation_runs (
    id BIGSERIAL PRIMARY KEY,
    run_at TIMESTAMP NOT NULL,
    window_start TIMESTAMP NOT NULL,
    audit_count BIGINT NOT NULL,
    ledger_count BIGINT NOT NULL,
    missing_in_ledger BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes TEXT
);

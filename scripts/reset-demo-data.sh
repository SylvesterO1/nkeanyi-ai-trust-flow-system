#!/usr/bin/env bash
set -euo pipefail

echo "============================================================"
echo "Resetting NAITFS Demo Data"
echo "============================================================"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-naitfs-postgres}"
POSTGRES_DB="${POSTGRES_DB:-naitfs_db}"
POSTGRES_USER="${POSTGRES_USER:-naitfs_user}"

echo "Postgres container: $POSTGRES_CONTAINER"
echo "Database:           $POSTGRES_DB"
echo "User:               $POSTGRES_USER"

run_sql() {
  docker exec -i "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1
}

echo
echo "Checking Postgres connection..."
docker exec "$POSTGRES_CONTAINER" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null
echo "Postgres is ready."

echo
echo "Deleting MVP11/demo records safely..."

run_sql <<'SQL'
DO $$
DECLARE
  payment_where text := '';
  evidence_where text := '';
BEGIN
  -- Build WHERE clause for payments only using columns that actually exist.
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'tenant_id'
  ) THEN
    payment_where := payment_where || 'tenant_id LIKE ''tenant-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'tenantid'
  ) THEN
    payment_where := payment_where || 'tenantid LIKE ''tenant-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'correlation_id'
  ) THEN
    payment_where := payment_where || 'correlation_id LIKE ''corr-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'correlationid'
  ) THEN
    payment_where := payment_where || 'correlationid LIKE ''corr-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'idempotency_key'
  ) THEN
    payment_where := payment_where || 'idempotency_key LIKE ''idem-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'idempotencykey'
  ) THEN
    payment_where := payment_where || 'idempotencykey LIKE ''idem-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'customer_id'
  ) THEN
    payment_where := payment_where || 'customer_id LIKE ''customer-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'customerid'
  ) THEN
    payment_where := payment_where || 'customerid LIKE ''customer-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'payment_reference'
  ) THEN
    payment_where := payment_where || 'payment_reference LIKE ''MVP11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'payments'
      AND column_name = 'paymentreference'
  ) THEN
    payment_where := payment_where || 'paymentreference LIKE ''MVP11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'payments'
  ) AND payment_where <> '' THEN
    payment_where := regexp_replace(payment_where, ' OR $', '');

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'payment_idempotency_keys') THEN
      EXECUTE 'DELETE FROM payment_idempotency_keys WHERE payment_id IN (SELECT payment_id FROM payments WHERE ' || payment_where || ') OR idempotency_key LIKE ''idem-mvp11-%''';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'payment_status_events') THEN
      EXECUTE 'DELETE FROM payment_status_events WHERE payment_id IN (SELECT payment_id FROM payments WHERE ' || payment_where || ')';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'payment_risk_decisions') THEN
      EXECUTE 'DELETE FROM payment_risk_decisions WHERE payment_id IN (SELECT payment_id FROM payments WHERE ' || payment_where || ')';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ledger_entries') THEN
      EXECUTE 'DELETE FROM ledger_entries WHERE payment_id IN (SELECT payment_id FROM payments WHERE ' || payment_where || ')';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'outbox_events') THEN
      EXECUTE 'DELETE FROM outbox_events WHERE aggregate_id IN (SELECT payment_id FROM payments WHERE ' || payment_where || ') OR payload LIKE ''%tenant-mvp11-%'' OR payload LIKE ''%corr-mvp11-%''';
    END IF;

    EXECUTE 'DELETE FROM payments WHERE ' || payment_where;
    RAISE NOTICE 'Deleted MVP11/demo records from payments.';
  ELSE
    RAISE NOTICE 'Skipped payments cleanup because no known MVP11 columns were found.';
  END IF;

  -- Build WHERE clause for compliance_evidence only using columns that actually exist.
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'compliance_evidence'
      AND column_name = 'tenant_id'
  ) THEN
    evidence_where := evidence_where || 'tenant_id LIKE ''tenant-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'compliance_evidence'
      AND column_name = 'tenantid'
  ) THEN
    evidence_where := evidence_where || 'tenantid LIKE ''tenant-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'compliance_evidence'
      AND column_name = 'correlation_id'
  ) THEN
    evidence_where := evidence_where || 'correlation_id LIKE ''corr-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'compliance_evidence'
      AND column_name = 'correlationid'
  ) THEN
    evidence_where := evidence_where || 'correlationid LIKE ''corr-mvp11-%'' OR ';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'compliance_evidence'
  ) AND evidence_where <> '' THEN
    evidence_where := regexp_replace(evidence_where, ' OR $', '');
    EXECUTE 'DELETE FROM compliance_evidence WHERE ' || evidence_where;
    RAISE NOTICE 'Deleted MVP11/demo records from compliance_evidence.';
  ELSE
    RAISE NOTICE 'Skipped compliance_evidence cleanup because no known MVP11 columns were found.';
  END IF;
END $$;
SQL

echo
echo "Demo data reset complete."
echo
echo "No Docker volumes were deleted."
echo "No infrastructure containers were removed."
echo "This script is safe to rerun."

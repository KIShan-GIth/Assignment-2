# Simplify Money — Ledger Sync Service

Backend ledger ingestion, parser fix, and reconciliation engine built for the Simplify Money technical assessment. This service parses bank SMS and emails, normalizes transactions, classifies spends/transfers, deduplicates raw notifications, and synchronizes state between SQL and a document store.

---

## 🚀 Quickstart

### 1. Boot Document Store Infrastructure
Launch the local MongoDB instance using Docker Compose:
```bash
docker compose up -d

## Incident INC-2026-09-11 Response

1. WHAT BROKE: Amounts regex required exact 2 decimal places and matched first global pattern, scanning past integer debits (e.g., 'Rs.5') into 'Avl Bal' (e.g., 'Rs.92,213.10').
2. HOW FOUND: Isolated message 'm-00004-9c11ae' in corpus-a where a Rs.5 UPI debit for WATER CAN recorded as a Rs.92,213.10 spend.
3. WHO AFFECTED: 14 transactions across accounts where debit amounts were reported without decimal places alongside inline available balances.
4. WHY IT CANNOT RECUR: Updated regex to support optional decimals, strip balance clauses prior to parsing, and locked regression test in AmountsTest.
5. STATUS: Verified green across full test suite; no frozen contract files modified.
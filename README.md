# LedgerSync - MongoDB Migration

A full-stack financial transaction ingestion and reporting engine backed by MongoDB.

## Architecture & Tech Stack
* **Language/Framework**: Java 21, Gradle
* **Database**: MongoDB 7 (running via Docker)
* **Storage Layer**: `MongoDocumentStore` (implementing custom query methods for account filtering, category totals, and message ID lookups).

## Prerequisites
* Docker & Docker Compose
* Java 21 JDK
* Gradle

## Getting Started

1. **Start MongoDB Container**:
   Spin up the container using Docker Compose (exposes port `27017` with root authentication):
   ```powershell
   docker compose up -d

Run Ingestion & Reports:

PowerShell
./gradlew run --args="ingest fixtures/corpus-a.jsonl"
./gradlew run --args="report output-folder"

Document Model & Performance Metrics (at 100,000 Transactions)MongoDB Document SchemaEach document in the transactions collection is structured for high-performance retrieval:JSON{
"sourceMessageIds": ["m-00087-1a2b3c"],
"accountLast4": "4821",
"occurredAt": "2026-07-04T20:24:00+05:30",
"direction": "debit",
"amount": 2499.50,
"category": "SPEND",
"merchant": "AMAZON PAY"
}
Examined vs. Returned MetricsQuery MethodTotal Docs ExaminedDocs

Query Method	Total Docs Examined	Docs Returned (nReturned)	Optimization Strategy		
forAccountMonth	~30	~30	Composite index { accountLast4: 1, occurredAt: -1 }		
categoryTotals	~500	~500	Index on { accountLast4: 1 }		
byMessageId	1	1	Multi-key index on sourceMessageIds	

Decision Log
1. MongoDB over DynamoDB: Chosen for fast local Docker deployment, flexible BSON support, and intuitive aggregation APIs.
2. Currency Precision: Enforced strict .setScale(2, RoundingMode.HALF_UP) during BSON-to-Domain mapping to prevent precision drift against frozen contract constraints.
3. Compound Index Strategy: Built composite indexes on account and timestamp to ensure efficient chronological sorting.
4. Message ID Arrays: Stored sourceMessageIds as arrays to enable direct inverse lookups.
5. Idempotent Backfill: Designed backfill logic to be safely re-runnable without duplicate key violations.
6. Robust Consistency Checking: Implemented field-level validation instead of simple row counts to catch targeted mutations.
7. Parser Error Isolation: Skipped malformed messages cleanly to maintain pipeline stability.
8. Docker Automation: Bundled the database configuration directly into docker-compose.yml for single-command setup.

AI Disclosure

1.Tools Used: Claude, Cursor, and Gemini for structure and debugging.

2.Concrete Correction: Initial AI suggestions mapped document doubles to BigDecimal directly without setting scale, causing runtime errors against the frozen NormalizedTxn constructor. Manually enforced .setScale(2, RoundingMode.HALF_UP) to resolve the validation failure.

Incident INC-2026-09-11 Response

1. WHAT BROKE: Amounts regex required exact 2 decimal places and matched first global pattern, scanning past integer debits (e.g., 'Rs.5') into 'Avl Bal' (e.g., 'Rs.92,213.10').
2. HOW FOUND: Isolated message 'm-00004-9c11ae' in corpus-a where a Rs.5 UPI debit for WATER CAN recorded as a Rs.92,213.10 spend.
3. WHO AFFECTED: 14 transactions across accounts where debit amounts were reported without decimal places alongside inline available balances.
4. WHY IT CANNOT RECUR: Updated regex to support optional decimals, strip balance clauses prior to parsing, and locked regression test in AmountsTest.
5. STATUS: Verified green across full test suite; no frozen contract files modified.
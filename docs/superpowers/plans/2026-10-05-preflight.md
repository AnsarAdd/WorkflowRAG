# Document preflight implementation plan

**Goal:** Implement the approved preview/confirm flow and position-independent incremental processing.
**Architecture:** Dedicated preflight module: controller, service interfaces/impl, models, repository/postgres. Shared contract validator for source ingestion. Persistent immutable preview snapshot, transactional confirmation followed by existing workflow execution.
**Tech Stack:** Existing Java 21, Spring Boot, JdbcClient, PostgreSQL, Liquibase; no new runtime dependencies.
**Spec:** ../specs/2026-10-05-preflight-design.md

## Constraints
Local codex/finch only; preserve source style and evolved retrieval behavior. UI deferred. Tests use temporary DB only. No global vector dedup across sources. No semantic equality assumptions.

## Task 1: Stable text units and reuse
- [x] Write a regression for insertion/reordering changing chunk indices while matching embedding fingerprints; demonstrate failure.
- [x] Modify ParagraphTextChunker and ChunkStage version to paragraph-local boundaries and prose whitespace normalization; preserve fenced code layout.
- [x] Modify PostgresChunkEmbeddingRepository.reuseFromActiveVersion to match fingerprint across ACTIVE chunks of the same document, deterministic LIMIT 1; retain model fingerprint.
- [x] Verify insertion, movement, local edits, revision mismatch, code whitespace and limits.

## Task 2: Contract and preview
- [x] Write HTTP integration scenarios for POST /api/v1/ingestions/previews/source, multipart /previews/upload, GET /previews/{id}, POST /previews/{id}/confirm; assert immutable snapshot and no job before confirm.
- [x] Add DocumentContractValidator and configuration limits. Reuse validation in direct source ingestion; enforce bounded read in file adapter.
- [x] Add preview repository/migration, storing document identity, format, raw bytes/hash, baseline version, forceReindex, expiry, report JSON and confirmed Job ID.
- [x] Add text change report from normalized paragraph multisets. Similarity service samples bounded representative sections across document, launches lexical and semantic calls in parallel over registered sources; partial failure is visible.
- [x] Add preview/confirm service; lock preview and document in transaction, reject stale baselines, associate job atomically. Run expensive workflow after commit. Return same job on repeated confirm.
- [x] Verify validation, unchanged short circuit, expiry, stale baseline, similarity failure, same job id, source path identity and real multipart input.

## Task 3: Integration, review, delivery
- [x] Run Maven verify; investigate failures before fixes.
- [x] Review complete diff for integrity, concurrency, API style and snapshot correctness.
- [x] Document contract, endpoints, incremental limitations and cleanup in README.
- [x] Commit intended files and verify clean branch; no merge/push.

## Decisions
Existing voice approval covers this design; no repeated approval gate. Keep configured-source identity for both file and source previews. Search failure is warning and permits confirmation; invalid contract does not. Similarity samples are advisory. Database is single-instance as already documented.

## Verification outcome
41 tests passed with Maven offline verify, including real PostgreSQL/pgvector, multipart HTTP, concurrent confirmation, snapshot preservation, stale and expired previews, contract limits, model outage, and paragraph insertion/reordering. Read-only code review found no blockers. Advisory sampling can be crowded by self hits before top-10 filtering; retention-bounded confirmation and this limit are documented in README. No UI or filesystem watcher added.

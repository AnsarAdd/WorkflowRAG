-- Upgrade the initial vector-only schema; also safe for the previously edited 006.
ALTER TABLE retrieval_chunk ADD COLUMN IF NOT EXISTS search_vector TSVECTOR;

UPDATE retrieval_chunk r
SET search_vector = setweight(to_tsvector('simple', COALESCE(s.title, '')), 'A')
                 || setweight(to_tsvector('simple', c.content), 'B')
FROM document_chunk c
JOIN document_section s ON s.id = c.section_id
WHERE r.chunk_id = c.id AND r.search_vector IS NULL;

ALTER TABLE retrieval_chunk ALTER COLUMN search_vector SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_retrieval_chunk_search_vector
    ON retrieval_chunk USING GIN (search_vector);
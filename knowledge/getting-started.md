# WorkflowRAG

WorkflowRAG stores document versions and indexes knowledge for search.

## Document lifecycle

A document is parsed into sections, split into chunks, embedded and indexed.
Only a fully processed version becomes ACTIVE. Failed updates preserve the previous active version.

## Search

SIMPLE uses semantic search. HYBRID combines semantic and lexical search with reciprocal rank fusion.
ADVANCED additionally requires an enabled reranker. Context retrieval can expand a matching chunk to its section.
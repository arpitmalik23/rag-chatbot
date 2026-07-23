# RAG Chatbot — Backend

Spring Boot service that powers the RAG chatbot: PDF ingestion, chunking,
Gemini embeddings, Qdrant vector search, and Gemini generation, with
Upstash Redis caching session/doc metadata and chat history.

## Requirements

- Java 17+
- Maven 3.9+
- A running Qdrant instance (local via Docker, or Qdrant Cloud)
- A Gemini API key
- An Upstash Redis database (or any Redis instance reachable over TLS)

## Setup

1. Copy `.env.example` to `.env` and fill in the values, or export the
   same variables in your shell.
2. Run Qdrant locally if you don't have a hosted instance:
   ```bash
   docker run -p 6333:6333 qdrant/qdrant
   ```
3. Start the backend:
   ```bash
   mvn spring-boot:run
   ```

The API listens on `http://localhost:8080` by default, with CORS opened
to `http://localhost:5173` (the Vite dev server) — override with
`CORS_ALLOWED_ORIGINS`.

## Pipeline

**Upload (`POST /api/upload`)**
1. `PdfService` extracts text per page with PDFBox, then splits it into
   overlapping chunks (500 words / 50-word overlap — see `PdfService`
   for the tokenizer caveat).
2. `EmbeddingService` embeds each chunk via Gemini
   (`text-embedding-004`).
3. `VectorStoreService` upserts the chunks (text + page + session id
   payload) into Qdrant.
4. `RedisService` caches the document's metadata against the session id.

**Chat (`POST /api/chat`)**
1. The question is embedded via Gemini.
2. `VectorStoreService` searches Qdrant, filtered to the current
   `sessionId`, for the top-k closest chunks.
3. `LlmService` builds a grounded prompt from those chunks and calls
   Gemini `generateContent`.
4. The Q&A turn is cached in Redis; the answer and cited chunks are
   returned to the client.

## Endpoints

| Method | Path                | Body / Params                          | Returns |
|--------|---------------------|-----------------------------------------|---------|
| POST   | `/api/upload`       | multipart `file`, `sessionId`           | `{ docId, filename, chunkCount, sessionId }` |
| POST   | `/api/chat`         | JSON `{ question, sessionId }`          | `{ answer, sources: [{ text, page, score }] }` |
| GET    | `/api/chat/history`  | query `sessionId`                       | `{ sessionId, turns: [{ question, answer, timestamp }] }` |

## Project structure

```
src/main/java/com/ragbot/
├── RagChatbotApplication.java
├── controller/
│   ├── UploadController.java     # POST /api/upload
│   └── ChatController.java       # POST /api/chat, GET /api/chat/history
├── service/
│   ├── PdfService.java           # PDFBox extraction + chunking
│   ├── EmbeddingService.java     # Gemini embeddings API calls
│   ├── VectorStoreService.java   # Qdrant REST wrapper
│   ├── LlmService.java           # Gemini generateContent calls
│   └── RedisService.java         # Upstash Redis client
├── model/
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── SourceChunk.java
│   ├── UploadResponse.java
│   ├── ChatTurn.java
│   └── DocChunk.java
├── config/
│   └── AppConfig.java            # CORS, WebClient, Jedis pool
└── exception/
    └── GlobalExceptionHandler.java
```

## Notes / things to swap in for production

- **Chunking** is word-based, not a real tokenizer — close enough for
  ~500-token chunks with English text, but swap in a proper tokenizer
  (e.g. `jtokkit`) if you need exact counts.
- **Embedding calls** run sequentially per chunk in `embedBatch`; for
  large PDFs, batch them with Gemini's `batchEmbedContents` endpoint or
  parallelize with a bounded executor.
- **Qdrant collection creation** in `VectorStoreService.ensureCollection()`
  swallows errors on purpose (it 4xxs harmlessly once the collection
  already exists) — check logs if chunks aren't showing up.
- **Vector size** (`gemini.embedding-dimensions`) must match whatever
  Gemini's embedding model actually returns — 768 for
  `text-embedding-004` at the time of writing; verify against current
  Gemini docs before deploying.

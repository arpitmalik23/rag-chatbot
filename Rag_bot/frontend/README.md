# RAG Chatbot — Frontend

React + Vite client for the RAG chatbot. Talks to a Spring Boot backend
exposing `/api/upload` and `/api/chat`.

## Setup

```bash
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies any `/api/*`
request to `http://localhost:8080` (see `vite.config.js`). Adjust the
proxy target or set `VITE_API_BASE_URL` in a `.env` file if your backend
runs elsewhere — see `.env.example`.

## Structure

```
src/
├── App.jsx                  # layout: sidebar (upload + pipeline) + chat
├── components/
│   ├── PdfUpload.jsx         # drag-and-drop PDF upload with progress
│   ├── ChatWindow.jsx        # message list + input, calls /api/chat
│   └── MessageBubble.jsx     # renders a message + retrieved source excerpts
└── api/
    └── client.js             # fetch wrappers, session id management
```

## Backend contract this frontend expects

**POST `/api/upload`** — `multipart/form-data` with fields `file` (PDF)
and `sessionId` (string). Response:

```json
{ "docId": "abc123", "filename": "handbook.pdf", "chunkCount": 42 }
```

**POST `/api/chat`** — JSON body `{ "question": "...", "sessionId": "..." }`.
Response:

```json
{
  "answer": "...",
  "sources": [
    { "text": "excerpt from the matched chunk", "page": 4, "score": 0.87 }
  ]
}
```

`sources` is optional — if the backend omits it, the UI simply won't show
the highlighted excerpt cards under that answer.

**GET `/api/chat/history?sessionId=...`** *(optional, not yet wired into
the UI)* — for restoring cached Redis chat history on reload.

## Session handling

A random `sessionId` (UUID) is generated on first load and persisted in
`localStorage` so uploads and chat turns are tied together across a
browser session. "New session" in the header clears it and starts fresh.

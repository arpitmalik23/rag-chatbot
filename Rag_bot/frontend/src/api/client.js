const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const SESSION_KEY = 'ragbot_session_id'

export function getSessionId() {
  let id = localStorage.getItem(SESSION_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(SESSION_KEY, id)
  }
  return id
}

export function resetSession() {
  const id = crypto.randomUUID()
  localStorage.setItem(SESSION_KEY, id)
  return id
}

async function handleResponse(res) {
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      message = body.message || body.error || message
    } catch {
      // response wasn't JSON, keep default message
    }
    throw new Error(message)
  }
  return res.json()
}

/**
 * Upload a PDF for ingestion.
 * Backend: POST /api/upload  (multipart/form-data)
 * Expected response shape: { docId, filename, chunkCount, sessionId }
 */
export async function uploadPdf(file, onProgress) {
  const sessionId = getSessionId()
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sessionId', sessionId)

  // Use XHR instead of fetch so we can report upload progress
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${BASE_URL}/upload`)

    xhr.upload.onprogress = (event) => {
      if (onProgress && event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }

    xhr.onload = () => {
      try {
        const data = JSON.parse(xhr.responseText)
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(data)
        } else {
          reject(new Error(data.message || `Upload failed (${xhr.status})`))
        }
      } catch (err) {
        reject(new Error('Upload failed: invalid server response'))
      }
    }

    xhr.onerror = () => reject(new Error('Network error during upload'))
    xhr.send(formData)
  })
}

/**
 * Ask a question against the ingested document(s).
 * Backend: POST /api/chat  { question, sessionId }
 * Expected response shape: { answer, sources: [{ text, page, score }] }
 */
export async function sendMessage(question) {
  const sessionId = getSessionId()
  const res = await fetch(`${BASE_URL}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question, sessionId }),
  })
  return handleResponse(res)
}

/**
 * Fetch cached chat history for the current session (optional endpoint).
 * Backend: GET /api/chat/history?sessionId=...
 */
export async function fetchHistory() {
  const sessionId = getSessionId()
  const res = await fetch(`${BASE_URL}/chat/history?sessionId=${sessionId}`)
  return handleResponse(res)
}

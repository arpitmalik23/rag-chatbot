import { useRef, useState } from 'react'
import { uploadPdf } from '../api/client'

export default function PdfUpload({ onUploaded, doc }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [progress, setProgress] = useState(0)
  const [status, setStatus] = useState('idle') // idle | uploading | error
  const [error, setError] = useState('')

  async function handleFile(file) {
    if (!file) return
    if (file.type !== 'application/pdf') {
      setError('Only PDF files are supported.')
      setStatus('error')
      return
    }
    setError('')
    setStatus('uploading')
    setProgress(0)
    try {
      const result = await uploadPdf(file, setProgress)
      setStatus('idle')
      onUploaded({
        filename: result.filename || file.name,
        chunkCount: result.chunkCount,
        docId: result.docId,
      })
    } catch (err) {
      setStatus('error')
      setError(err.message || 'Upload failed.')
    }
  }

  function onDrop(e) {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files?.[0]
    handleFile(file)
  }

  return (
    <div className="upload-panel">
      <div
        className={`dropzone ${dragging ? 'dropzone--active' : ''} ${status === 'uploading' ? 'dropzone--busy' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click() }}
      >
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf"
          hidden
          onChange={(e) => handleFile(e.target.files?.[0])}
        />

        {status === 'uploading' ? (
          <div className="dropzone__progress">
            <div className="progress-bar">
              <div className="progress-bar__fill" style={{ width: `${progress}%` }} />
            </div>
            <span className="mono">indexing… {progress}%</span>
          </div>
        ) : doc ? (
          <div className="dropzone__loaded">
            <span className="doc-mark" aria-hidden="true">◆</span>
            <div>
              <p className="dropzone__filename">{doc.filename}</p>
              <p className="mono dropzone__meta">
                {doc.chunkCount ? `${doc.chunkCount} chunks indexed` : 'ready to query'}
              </p>
            </div>
            <button
              type="button"
              className="link-btn"
              onClick={(e) => { e.stopPropagation(); inputRef.current?.click() }}
            >
              replace
            </button>
          </div>
        ) : (
          <div className="dropzone__empty">
            <span className="doc-mark" aria-hidden="true">◇</span>
            <p><strong>Drop a PDF here</strong> or click to browse</p>
            <p className="mono dropzone__hint">chunked at 500 tokens · 50 overlap</p>
          </div>
        )}
      </div>
      {error && <p className="upload-error">{error}</p>}
    </div>
  )
}

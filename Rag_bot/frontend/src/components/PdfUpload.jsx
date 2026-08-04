import { useRef, useState } from 'react'
import { uploadPdf } from '../api/client'

export default function PdfUpload({ docs, onUploaded }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [progress, setProgress] = useState(0)
  const [status, setStatus] = useState('idle')
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
      onUploaded({ filename: result.filename || file.name, chunkCount: result.chunkCount })
    } catch (err) {
      setStatus('error')
      setError(err.message || 'Upload failed.')
    }
  }

  function onDrop(e) {
    e.preventDefault()
    setDragging(false)
    handleFile(e.dataTransfer.files?.[0])
  }

  return (
    <div className="upload-box">
      <h2 className="upload-box__title">Upload your documents here</h2>
      <p className="upload-box__subtitle">PDF only — ask questions about it once indexed</p>

      <div
        className={`dropzone ${dragging ? 'dropzone--active' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click() }}
      >
        <input ref={inputRef} type="file" accept="application/pdf" hidden onChange={(e) => handleFile(e.target.files?.[0])} />

        {status === 'uploading' ? (
          <div className="dropzone__progress">
            <div className="progress-bar"><div className="progress-bar__fill" style={{ width: `${progress}%` }} /></div>
            <span className="mono">indexing… {progress}%</span>
          </div>
        ) : (
          <div className="dropzone__empty">
            <span className="doc-mark">＋</span>
            <p><strong>Drop a PDF here</strong> or click to browse</p>
          </div>
        )}
      </div>

      {error && <p className="upload-error">{error}</p>}

      {docs.length > 0 && (
        <ul className="doc-list">
          {docs.map((d, i) => (
            <li key={i} className="doc-list__item">
              <span className="doc-mark doc-mark--sm">✓</span>
              <div>
                <p className="doc-list__name">{d.filename}</p>
                <p className="mono doc-list__meta">{d.chunkCount} chunks</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
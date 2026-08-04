import { useState } from 'react'
import ChatWindow from './components/ChatWindow'
import PdfUpload from './components/PdfUpload'
import { resetSession } from './api/client'

export default function App() {
  const [docs, setDocs] = useState([])

  function handleNewSession() {
    resetSession()
    setDocs([])
    window.location.reload()
  }

  return (
    <div className="app-shell">
      <aside className="app-side">
        <PdfUpload docs={docs} onUploaded={(d) => setDocs(prev => [...prev, d])} />
      </aside>

      <div className="app">
        <header className="app-header">
          <button className="ghost-btn" onClick={handleNewSession}>New session</button>
        </header>
        <ChatWindow docReady={docs.length > 0} />
      </div>
    </div>
  )
}
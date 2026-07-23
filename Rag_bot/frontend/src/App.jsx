import { useState } from 'react'
import PdfUpload from './components/PdfUpload'
import ChatWindow from './components/ChatWindow'
import { resetSession } from './api/client'

export default function App() {
  const [doc, setDoc] = useState(null)

  function handleNewSession() {
    resetSession()
    setDoc(null)
    window.location.reload()
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <span className="brand__mark">◆</span>
          <div>
            <h1>DocQuery</h1>
            <p className="mono brand__sub">retrieval-augmented chat</p>
          </div>
        </div>
        <button className="ghost-btn mono" onClick={handleNewSession}>new session</button>
      </header>

      <main className="app-main">
        <aside className="app-side">
          <h2 className="section-label mono">01 · source document</h2>
          <PdfUpload doc={doc} onUploaded={setDoc} />

          <h2 className="section-label mono">02 · how it works</h2>
          <ol className="pipeline">
            <li>PDF is split into overlapping chunks</li>
            <li>Each chunk is embedded and stored in Qdrant</li>
            <li>Your question is embedded and matched to the closest chunks</li>
            <li>Those chunks + your question are sent to the model</li>
          </ol>
        </aside>

        <section className="app-chat">
          <ChatWindow docReady={!!doc} />
        </section>
      </main>
    </div>
  )
}

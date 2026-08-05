import { useState } from 'react'
import ChatWindow from './components/ChatWindow'
import PdfUpload from './components/PdfUpload'
import Login from './components/Login'
import { getToken, getUsername, logout } from './api/auth'

export default function App() {
  const [authed, setAuthed] = useState(!!getToken())
  const [docs, setDocs] = useState([])

  if (!authed) {
    return <Login onSuccess={() => setAuthed(true)} />
  }

  function handleLogout() {
    logout()
    setDocs([])
    setAuthed(false)
  }

  return (
    <div className="app-shell">
      <aside className="app-side">
        <PdfUpload docs={docs} onUploaded={(d) => setDocs(prev => [...prev, d])} />
      </aside>
      <div className="app">
        <header className="app-header">
          <span className="mono" style={{ color: 'var(--muted)', fontSize: 12 }}>{getUsername()}</span>
          <button className="ghost-btn" onClick={handleLogout}>Log out</button>
        </header>
        <ChatWindow docReady={docs.length > 0} />
      </div>
    </div>
  )
}
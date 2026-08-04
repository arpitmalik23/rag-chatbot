import { useEffect, useRef, useState } from 'react'
import MessageBubble from './MessageBubble'
import { sendMessage } from '../api/client'

export default function ChatWindow({ docReady }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const scrollRef = useRef(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages])

  async function handleSend(e) {
    e.preventDefault()
    const question = input.trim()
    if (!question || sending) return

    setInput('')
    setMessages((prev) => [...prev, { role: 'user', text: question }])
    setSending(true)
    setMessages((prev) => [...prev, { role: 'bot', pending: true }])

    try {
      const result = await sendMessage(question)
      setMessages((prev) => {
        const next = [...prev]
        next[next.length - 1] = {
          role: 'bot',
          text: result.answer,
          sources: result.sources,
        }
        return next
      })
    } catch (err) {
      setMessages((prev) => {
        const next = [...prev]
        next[next.length - 1] = {
          role: 'bot',
          text: `Something went wrong: ${err.message}`,
          error: true,
        }
        return next
      })
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="chat-window">
      <div className="chat-scroll" ref={scrollRef}>
        {messages.length === 0 ? (
          <div className="chat-empty">
            <span className="doc-mark doc-mark--lg" aria-hidden="true">◇</span>
            <p>
              {docReady
                ? 'Your document is indexed. Ask anything about it.'
                : 'Upload a PDF to start asking questions.'}
            </p>
          </div>
        ) : (
          messages.map((m, i) => <MessageBubble key={i} {...m} />)
        )}
      </div>

      <form className="chat-input" onSubmit={handleSend}>
        <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder={docReady ? 'Ask a question about the document…' : 'Ask anything, or upload a PDF for document-specific answers…'}
        disabled={sending}
      />
      <button type="submit" disabled={sending || !input.trim()}>
        send
      </button>
      </form>
    </div>
  )
}

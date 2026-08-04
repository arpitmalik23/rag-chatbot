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
        next[next.length - 1] = { role: 'bot', text: result.answer, sources: result.sources }
        return next
      })
    } catch (err) {
      setMessages((prev) => {
        const next = [...prev]
        next[next.length - 1] = { role: 'bot', text: `Something went wrong: ${err.message}` }
        return next
      })
    } finally {
      setSending(false)
    }
  }

  const pillForm = (
    <form className="pill-form" onSubmit={handleSend}>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder={docReady ? 'Ask a question about the document…' : 'Ask anything…'}
        disabled={sending}
      />
    
      <button type="submit" className="send-btn" disabled={sending || !input.trim()}>↑</button>
    </form>
  )

  if (messages.length === 0) {
    return (
      <div className="hero">
        <h1>What's next?</h1>
        {pillForm}
      </div>
    )
  }

  return (
    <div className="chat-window">
      <div className="chat-scroll" ref={scrollRef}>
        {messages.map((m, i) => <MessageBubble key={i} {...m} />)}
      </div>
      <div className="chat-input-dock">{pillForm}</div>
    </div>
  )
}
export default function MessageBubble({ role, text, sources, pending }) {
  const isUser = role === 'user'

  return (
    <div className={`msg-row ${isUser ? 'msg-row--user' : 'msg-row--bot'}`}>
      <div className="msg-avatar mono" aria-hidden="true">{isUser ? 'YOU' : 'DOC'}</div>

      <div className="msg-stack">
        <div className={`msg-bubble ${isUser ? 'msg-bubble--user' : 'msg-bubble--bot'}`}>
          {pending ? (
            <span className="typing" aria-label="Assistant is thinking">
              <span className="typing__dot" />
              <span className="typing__dot" />
              <span className="typing__dot" />
            </span>
          ) : (
            <p>{text}</p>
          )}
        </div>

        {!pending && sources && sources.length > 0 && (
          <div className="excerpts">
            {sources.map((s, i) => (
              <div className="excerpt" key={i}>
                <div className="excerpt__mark" aria-hidden="true" />
                <div className="excerpt__body">
                  <p className="excerpt__text">"{s.text}"</p>
                  <p className="mono excerpt__meta">
                    {s.page ? `p.${s.page}` : `chunk ${i + 1}`}
                    {typeof s.score === 'number' ? ` · ${(s.score * 100).toFixed(0)}% match` : ''}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

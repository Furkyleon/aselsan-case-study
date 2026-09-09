import { useEffect } from 'react'

function Notification({ id, message, onDismiss }) {
  useEffect(() => {
    const timeoutId = window.setTimeout(() => onDismiss(id), 4_000)
    return () => window.clearTimeout(timeoutId)
  }, [id, onDismiss])

  return (
    <div className="notification" role="status">
      <span className="notification-symbol" aria-hidden="true">✓</span>
      <span>{message}</span>
      <button
        type="button"
        aria-label="Bildirimi kapat"
        onClick={() => onDismiss(id)}
      >
        ×
      </button>
    </div>
  )
}

function NotificationStack({ notifications, onDismiss }) {
  return (
    <aside className="notification-stack" aria-label="Bildirimler">
      {notifications.map((notification) => (
        <Notification
          key={notification.id}
          {...notification}
          onDismiss={onDismiss}
        />
      ))}
    </aside>
  )
}

export default NotificationStack

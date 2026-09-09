function formatTimestamp(timestamp) {
  if (!timestamp) {
    return 'Henüz veri alınmadı'
  }

  return new Intl.DateTimeFormat('tr-TR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(timestamp))
}

function DashboardHeader({ error, isLoading, running, timestamp }) {
  const connectionState = error
    ? 'Bağlantı kesildi'
    : isLoading
      ? 'Bağlanıyor'
      : running
        ? 'Simülasyon aktif'
        : 'Sistem hazır'

  return (
    <header className="dashboard-header">
      <div className="brand-lockup">
        <span className="brand-mark" aria-hidden="true">
          QM
        </span>
        <div>
          <p className="eyebrow">OPERATIONS CONSOLE</p>
          <h1>Queue Monitor</h1>
        </div>
      </div>

      <div className="system-state" aria-live="polite">
        <span
          className={`status-dot ${error ? 'is-error' : running ? 'is-live' : ''}`}
          aria-hidden="true"
        />
        <div>
          <strong>{connectionState}</strong>
          <span>Son veri: {formatTimestamp(timestamp)}</span>
        </div>
      </div>
    </header>
  )
}

export default DashboardHeader

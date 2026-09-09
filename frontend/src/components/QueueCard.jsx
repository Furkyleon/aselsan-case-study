const SEGMENT_COUNT = 18

function QueueCard({ queue, running }) {
  const occupancy = Math.min(100, Math.max(0, queue.occupancyPercentage ?? 0))
  const filledSegments = Math.round((occupancy / 100) * SEGMENT_COUNT)

  return (
    <section className="panel queue-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">BUFFER STATUS</p>
          <h2>Queue doluluğu</h2>
        </div>
        <span className={`state-pill ${running ? 'is-live' : ''}`}>
          {running ? 'Canlı' : 'Beklemede'}
        </span>
      </div>

      <div className="queue-overview">
        <div
          className="occupancy-ring"
          style={{ '--occupancy': `${occupancy * 3.6}deg` }}
          aria-label={`Queue doluluk oranı yüzde ${occupancy.toFixed(1)}`}
        >
          <div>
            <strong>{occupancy.toFixed(1)}</strong>
            <span>% dolu</span>
          </div>
        </div>

        <div className="queue-details">
          <div className="queue-count">
            <strong>{queue.size}</strong>
            <span>/ {queue.capacity || '—'} mesaj</span>
          </div>
          <div className="queue-segments" aria-hidden="true">
            {Array.from({ length: SEGMENT_COUNT }, (_, index) => (
              <span
                key={index}
                className={index < filledSegments ? 'is-filled' : ''}
              />
            ))}
          </div>
          <div className="queue-scale">
            <span>Boş</span>
            <span>Kapasite {queue.capacity || '—'}</span>
          </div>
        </div>
      </div>

      <div className="flow-line" aria-label="Sender, queue ve receiver veri akışı">
        <span>Sender</span>
        <i aria-hidden="true" />
        <strong>{queue.size} bekliyor</strong>
        <i aria-hidden="true" />
        <span>Receiver</span>
      </div>
    </section>
  )
}

export default QueueCard

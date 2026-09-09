const SEGMENT_COUNT = 18

function QueueCard({ messageFlow, queue, running }) {
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

      <div className="message-flow" aria-label="Sender, queue ve receiver mesaj akışı">
        <div className="flow-stat flow-sent">
          <span>Sender → Queue</span>
          <strong>{messageFlow.produced}</strong>
          <small>{messageFlow.productionRate.toFixed(1)} mesaj/sn · gönderildi</small>
        </div>
        <div className="flow-queue">
          <span>Queue</span>
          <strong>{queue.size}</strong>
          <small>bekliyor</small>
        </div>
        <div className="flow-stat flow-received">
          <span>Queue → Receiver</span>
          <strong>{messageFlow.consumed}</strong>
          <small>{messageFlow.consumptionRate.toFixed(1)} mesaj/sn · alındı</small>
        </div>
      </div>
    </section>
  )
}

export default QueueCard

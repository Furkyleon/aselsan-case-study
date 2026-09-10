import { useState } from 'react'

const ACTIVITY_LABELS = {
  STARTING: 'Başlatılıyor',
  PRODUCING: 'Mesaj gönderiyor',
  CONSUMING: 'Mesaj alıyor',
  QUEUE_FULL: 'Queue dolu',
  QUEUE_EMPTY: 'Queue boş',
  SLEEPING: 'Bekliyor',
  STOPPED: 'Durduruldu',
  FAILED: 'Hata',
}

function WorkerDetailsPanel({ disabled, pendingAction, workers, onStop }) {
  const [expanded, setExpanded] = useState(false)
  const activeWorkerCount = workers.filter((worker) => (
    worker.running || worker.activityState === 'STARTING'
  )).length

  return (
    <section className={`panel worker-details-panel ${expanded ? 'is-expanded' : ''}`}>
      <button
        type="button"
        className="worker-details-toggle"
        aria-expanded={expanded}
        aria-controls="worker-details-content"
        onClick={() => setExpanded((current) => !current)}
      >
        <span>
          <span className="eyebrow">WORKER REGISTRY</span>
          <strong>Worker detayları</strong>
        </span>
        <span className="worker-details-summary">
          {activeWorkerCount} aktif · {workers.length} kayıt
          <i aria-hidden="true">{expanded ? '−' : '+'}</i>
        </span>
      </button>

      {expanded && (
        <div id="worker-details-content" className="worker-details-content">
          {workers.length === 0 ? (
            <p className="worker-details-empty">
              Simülasyon başlatıldığında worker kayıtları burada görünecek.
            </p>
          ) : (
            <div className="worker-table-wrap">
              <table className="worker-table">
                <thead>
                  <tr>
                    <th scope="col">Worker ID</th>
                    <th scope="col">Tip</th>
                    <th scope="col">JVM state</th>
                    <th scope="col">Aktivite</th>
                    <th scope="col">Priority</th>
                    <th scope="col">Mesaj</th>
                    <th scope="col">Durum</th>
                    <th scope="col"><span className="sr-only">İşlem</span></th>
                  </tr>
                </thead>
                <tbody>
                  {workers.map((worker) => {
                    const isStopping = pendingAction === `stop-${worker.id}`

                    return (
                      <tr key={worker.id}>
                        <td>
                          <code>{worker.id}</code>
                        </td>
                        <td>
                          <span className={`worker-type worker-type-${worker.type.toLowerCase()}`}>
                            {worker.type === 'SENDER' ? 'Sender' : 'Receiver'}
                          </span>
                        </td>
                        <td><code>{worker.jvmState}</code></td>
                        <td>{ACTIVITY_LABELS[worker.activityState] ?? worker.activityState}</td>
                        <td><strong>{worker.priority} / 10</strong></td>
                        <td><strong>{worker.processedMessageCount}</strong></td>
                        <td>
                          <span className={`worker-run-state ${worker.running ? 'is-active' : ''}`}>
                            {worker.running ? 'Aktif' : 'Sonlandı'}
                          </span>
                        </td>
                        <td>
                          <button
                            type="button"
                            className="worker-stop-button"
                            disabled={disabled || !worker.running}
                            aria-busy={isStopping}
                            aria-label={`${worker.id} worker'ını durdur`}
                            onClick={() => onStop(worker.id)}
                          >
                            {isStopping ? 'Durduruluyor…' : 'Durdur'}
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

export default WorkerDetailsPanel

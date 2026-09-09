import { useState } from 'react'

const METRICS = [
  ['runnable', 'Runnable'],
  ['waiting', 'Waiting'],
  ['blocked', 'Blocked'],
  ['terminated', 'Terminated'],
]

function WorkerCard({
  accent,
  disabled,
  pendingAction,
  running,
  status,
  title,
  type,
  onAdd,
  onPriorityChange,
  onStopGroup,
  onStopOne,
}) {
  const [priority, setPriority] = useState(5)
  const activeCount = status.runnable + status.waiting + status.blocked
  const scope = type === 'SENDER' ? 'SENDERS' : 'RECEIVERS'
  const typeLabel = type === 'SENDER' ? 'sender' : 'receiver'

  return (
    <section className={`panel worker-panel accent-${accent}`}>
      <div className="panel-heading worker-heading">
        <div>
          <p className="eyebrow">{type}</p>
          <h2>{title}</h2>
        </div>
        <div className="worker-total">
          <strong>{activeCount}</strong>
          <span>aktif</span>
        </div>
      </div>

      <div className="metric-list">
        {METRICS.map(([key, label]) => {
          const percentage = status.total
            ? Math.round((status[key] / status.total) * 100)
            : 0

          return (
            <div className="metric-row" key={key}>
              <div>
                <span className={`metric-dot metric-${key}`} aria-hidden="true" />
                <span>{label}</span>
              </div>
              <div className="metric-bar" aria-hidden="true">
                <span style={{ width: `${percentage}%` }} />
              </div>
              <strong>{status[key]}</strong>
            </div>
          )
        })}
      </div>

      <div className="worker-actions">
        <div className="stepper" aria-label={`${title} sayısını değiştir`}>
          <button
            type="button"
            aria-label={`Bir ${typeLabel} durdur`}
            disabled={disabled || !running || activeCount === 0}
            onClick={() => onStopOne(type)}
          >
            −
          </button>
          <span>{pendingAction === `add-${type.toLowerCase()}` ? '…' : activeCount}</span>
          <button
            type="button"
            aria-label={`Bir ${typeLabel} ekle`}
            disabled={disabled || !running}
            onClick={() => onAdd({ type, count: 1 })}
          >
            +
          </button>
        </div>

        <button
          type="button"
          className="button button-quiet"
          disabled={disabled || !running || activeCount === 0}
          onClick={() => onStopGroup(scope)}
        >
          Grubu durdur
        </button>
      </div>

      <div className="priority-control">
        <div>
          <label htmlFor={`priority-${type}`}>Thread priority</label>
          <span>{priority} / 10</span>
        </div>
        <input
          id={`priority-${type}`}
          type="range"
          min="1"
          max="10"
          value={priority}
          disabled={disabled || !running || activeCount === 0}
          onChange={(event) => setPriority(Number(event.target.value))}
        />
        <button
          type="button"
          className="text-button"
          disabled={disabled || !running || activeCount === 0}
          onClick={() => onPriorityChange({ type, priority })}
        >
          Uygula
        </button>
      </div>
    </section>
  )
}

export default WorkerCard

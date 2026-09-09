const VIEWBOX_WIDTH = 560
const VIEWBOX_HEIGHT = 152
const CHART_PADDING = 12

function formatRate(value) {
  return `${value.toFixed(1)}/sn`
}

function buildLinePath(history, accessor, maximum) {
  if (!history.length) {
    return ''
  }

  const drawableWidth = VIEWBOX_WIDTH - CHART_PADDING * 2
  const drawableHeight = VIEWBOX_HEIGHT - CHART_PADDING * 2

  return history.map((point, index) => {
    const x = history.length === 1
      ? CHART_PADDING
      : CHART_PADDING + (index / (history.length - 1)) * drawableWidth
    const normalizedValue = Math.min(1, Math.max(0, accessor(point) / maximum))
    const y = VIEWBOX_HEIGHT - CHART_PADDING - normalizedValue * drawableHeight

    return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
  }).join(' ')
}

function MetricChart({ history, label, series, maximum }) {
  const safeMaximum = Math.max(1, maximum)

  return (
    <article className="telemetry-chart">
      <div className="chart-heading">
        <h3>{label}</h3>
        <div className="chart-legend">
          {series.map(({ color, current, name }) => (
            <span key={name}>
              <i style={{ '--legend-color': color }} aria-hidden="true" />
              {name} <strong>{current}</strong>
            </span>
          ))}
        </div>
      </div>

      <svg
        className="metric-chart"
        viewBox={`0 0 ${VIEWBOX_WIDTH} ${VIEWBOX_HEIGHT}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={`${label}, son ${history.length} ölçüm`}
      >
        {[0, 0.5, 1].map((position) => (
          <line
            key={position}
            x1={CHART_PADDING}
            x2={VIEWBOX_WIDTH - CHART_PADDING}
            y1={CHART_PADDING + position * (VIEWBOX_HEIGHT - CHART_PADDING * 2)}
            y2={CHART_PADDING + position * (VIEWBOX_HEIGHT - CHART_PADDING * 2)}
            className="chart-grid-line"
          />
        ))}
        {series.map(({ accessor, color, name }) => (
          <path
            key={name}
            d={buildLinePath(history, accessor, safeMaximum)}
            fill="none"
            stroke={color}
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
          />
        ))}
      </svg>
    </article>
  )
}

function TelemetryPanel({ history }) {
  const latest = history.at(-1) ?? {
    queueOccupancy: 0,
    productionRate: 0,
    consumptionRate: 0,
    senderActive: 0,
    receiverActive: 0,
  }
  const maximumRate = Math.max(
    1,
    ...history.flatMap(({ productionRate, consumptionRate }) => [
      productionRate,
      consumptionRate,
    ]),
  )
  const maximumWorkers = Math.max(
    1,
    ...history.flatMap(({ senderActive, receiverActive }) => [
      senderActive,
      receiverActive,
    ]),
  )

  return (
    <section className="panel telemetry-panel">
      <div className="panel-heading telemetry-heading">
        <div>
          <p className="eyebrow">LIVE TELEMETRY</p>
          <h2>Zaman serisi</h2>
        </div>
        <span className="state-pill">Son {history.length} / 60 ölçüm</span>
      </div>

      <div className="telemetry-grid">
        <MetricChart
          history={history}
          label="Queue doluluğu"
          maximum={100}
          series={[
            {
              name: 'Doluluk',
              color: 'var(--cyan)',
              current: `%${latest.queueOccupancy.toFixed(1)}`,
              accessor: (point) => point.queueOccupancy,
            },
          ]}
        />
        <MetricChart
          history={history}
          label="Mesaj akış hızı"
          maximum={maximumRate}
          series={[
            {
              name: 'Gönderilen',
              color: 'var(--cyan)',
              current: formatRate(latest.productionRate),
              accessor: (point) => point.productionRate,
            },
            {
              name: 'Alınan',
              color: 'var(--amber)',
              current: formatRate(latest.consumptionRate),
              accessor: (point) => point.consumptionRate,
            },
          ]}
        />
        <MetricChart
          history={history}
          label="Aktif worker"
          maximum={maximumWorkers}
          series={[
            {
              name: 'Sender',
              color: 'var(--cyan)',
              current: latest.senderActive,
              accessor: (point) => point.senderActive,
            },
            {
              name: 'Receiver',
              color: 'var(--amber)',
              current: latest.receiverActive,
              accessor: (point) => point.receiverActive,
            },
          ]}
        />
      </div>
    </section>
  )
}

export default TelemetryPanel

import { useState } from 'react'

const INITIAL_CONFIGURATION = {
  senderCount: 3,
  receiverCount: 2,
  queueCapacity: 20,
}

function StartSimulationForm({ disabled, running, onStart, onStopAll }) {
  const [configuration, setConfiguration] = useState(INITIAL_CONFIGURATION)
  const [validationError, setValidationError] = useState('')

  function updateField(event) {
    const { name, value } = event.target
    setConfiguration((current) => ({
      ...current,
      [name]: Number(value),
    }))
    setValidationError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()

    const totalWorkers = configuration.senderCount + configuration.receiverCount

    if (totalWorkers < 1) {
      setValidationError('En az bir sender veya receiver gerekli.')
      return
    }

    if (totalWorkers > 100) {
      setValidationError('Toplam worker sayısı 100’ü geçemez.')
      return
    }

    try {
      await onStart(configuration)
    } catch {
      // API errors are presented by the dashboard error banner.
    }
  }

  return (
    <section className="panel control-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">SIMULATION CONTROL</p>
          <h2>{running ? 'Simülasyonu yönet' : 'Yeni simülasyon'}</h2>
        </div>
      </div>

      {running ? (
        <div className="running-control">
          <div className="pulse-visual" aria-hidden="true">
            <span />
            <span />
            <span />
            <span />
          </div>
          <div>
            <strong>Worker’lar aktif</strong>
            <p>Canlı metrikler her saniye yenileniyor.</p>
          </div>
          <button
            type="button"
            className="button button-danger"
            disabled={disabled}
            onClick={onStopAll}
          >
            Tümünü durdur
          </button>
        </div>
      ) : (
        <form className="start-form" onSubmit={handleSubmit}>
          <div className="form-grid">
            <label>
              <span>Sender</span>
              <input
                type="number"
                name="senderCount"
                min="0"
                max="100"
                value={configuration.senderCount}
                onChange={updateField}
              />
            </label>
            <label>
              <span>Receiver</span>
              <input
                type="number"
                name="receiverCount"
                min="0"
                max="100"
                value={configuration.receiverCount}
                onChange={updateField}
              />
            </label>
          </div>

          <label>
            <span>Queue kapasitesi</span>
            <input
              type="number"
              name="queueCapacity"
              min="1"
              max="1000"
              value={configuration.queueCapacity}
              onChange={updateField}
            />
          </label>

          {validationError && (
            <p className="field-error" role="alert">{validationError}</p>
          )}

          <button type="submit" className="button button-primary" disabled={disabled}>
            {disabled ? 'Başlatılıyor…' : 'Simülasyonu başlat'}
          </button>
        </form>
      )}
    </section>
  )
}

export default StartSimulationForm

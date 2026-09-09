import { useState } from 'react'

const INITIAL_CONFIGURATION = {
  senderCount: 3,
  receiverCount: 2,
  queueCapacity: 20,
}

function firstError(errors, field) {
  return errors?.[field]?.[0] ?? ''
}

function StartSimulationForm({
  disabled,
  pendingAction,
  running,
  serverErrors,
  onClearError,
  onStart,
  onStopAll,
}) {
  const [configuration, setConfiguration] = useState(INITIAL_CONFIGURATION)
  const [validationErrors, setValidationErrors] = useState({})

  const errors = {
    senderCount: validationErrors.senderCount
      ?? firstError(serverErrors, 'senderCount'),
    receiverCount: validationErrors.receiverCount
      ?? firstError(serverErrors, 'receiverCount'),
    queueCapacity: validationErrors.queueCapacity
      ?? firstError(serverErrors, 'queueCapacity'),
    workerTotal: validationErrors.workerTotal,
  }

  function updateField(event) {
    const { name, value } = event.target
    setConfiguration((current) => ({
      ...current,
      [name]: Number(value),
    }))
    setValidationErrors((current) => ({
      ...current,
      [name]: undefined,
      workerTotal: undefined,
    }))
    onClearError()
  }

  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = {}

    if (configuration.senderCount < 0) {
      nextErrors.senderCount = 'Sender sayısı negatif olamaz.'
    }

    if (configuration.receiverCount < 0) {
      nextErrors.receiverCount = 'Receiver sayısı negatif olamaz.'
    }

    if (configuration.queueCapacity < 1 || configuration.queueCapacity > 1000) {
      nextErrors.queueCapacity = 'Queue kapasitesi 1–1000 arasında olmalı.'
    }

    const totalWorkers = configuration.senderCount + configuration.receiverCount
    if (totalWorkers < 1) {
      nextErrors.workerTotal = 'En az bir sender veya receiver gerekli.'
    } else if (totalWorkers > 100) {
      nextErrors.workerTotal = 'Toplam worker sayısı 100’ü geçemez.'
    }

    setValidationErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) {
      return
    }

    await onStart(configuration)
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
            aria-busy={pendingAction === 'stop-all'}
            onClick={onStopAll}
          >
            {pendingAction === 'stop-all' ? 'Durduruluyor…' : 'Tümünü durdur'}
          </button>
        </div>
      ) : (
        <form className="start-form" noValidate onSubmit={handleSubmit}>
          <div className="form-grid">
            <label className={errors.senderCount ? 'has-error' : ''}>
              <span>Sender</span>
              <input
                type="number"
                name="senderCount"
                min="0"
                max="100"
                value={configuration.senderCount}
                aria-invalid={Boolean(errors.senderCount)}
                aria-describedby={errors.senderCount ? 'sender-error' : undefined}
                onChange={updateField}
              />
              {errors.senderCount && (
                <small id="sender-error" className="field-error">
                  {errors.senderCount}
                </small>
              )}
            </label>
            <label className={errors.receiverCount ? 'has-error' : ''}>
              <span>Receiver</span>
              <input
                type="number"
                name="receiverCount"
                min="0"
                max="100"
                value={configuration.receiverCount}
                aria-invalid={Boolean(errors.receiverCount)}
                aria-describedby={errors.receiverCount ? 'receiver-error' : undefined}
                onChange={updateField}
              />
              {errors.receiverCount && (
                <small id="receiver-error" className="field-error">
                  {errors.receiverCount}
                </small>
              )}
            </label>
          </div>

          {errors.workerTotal && (
            <p className="field-error group-error" role="alert">
              {errors.workerTotal}
            </p>
          )}

          <label className={errors.queueCapacity ? 'has-error' : ''}>
            <span>Queue kapasitesi</span>
            <input
              type="number"
              name="queueCapacity"
              min="1"
              max="1000"
              value={configuration.queueCapacity}
              aria-invalid={Boolean(errors.queueCapacity)}
              aria-describedby={errors.queueCapacity ? 'capacity-error' : undefined}
              onChange={updateField}
            />
            {errors.queueCapacity && (
              <small id="capacity-error" className="field-error">
                {errors.queueCapacity}
              </small>
            )}
          </label>

          <button
            type="submit"
            className="button button-primary"
            disabled={disabled}
            aria-busy={pendingAction === 'start'}
          >
            {pendingAction === 'start' ? 'Başlatılıyor…' : 'Simülasyonu başlat'}
          </button>
        </form>
      )}
    </section>
  )
}

export default StartSimulationForm

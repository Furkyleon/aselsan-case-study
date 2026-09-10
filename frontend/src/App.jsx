import { useCallback, useState } from 'react'
import './App.css'
import ConfirmDialog from './components/ConfirmDialog.jsx'
import DashboardHeader from './components/DashboardHeader.jsx'
import NotificationStack from './components/NotificationStack.jsx'
import QueueCard from './components/QueueCard.jsx'
import StartSimulationForm from './components/StartSimulationForm.jsx'
import TelemetryPanel from './components/TelemetryPanel.jsx'
import WorkerCard from './components/WorkerCard.jsx'
import WorkerDetailsPanel from './components/WorkerDetailsPanel.jsx'
import { useSimulation } from './hooks/useSimulation.js'

const EMPTY_WORKER_STATUS = {
  total: 0,
  runnable: 0,
  waiting: 0,
  blocked: 0,
  terminated: 0,
  priority: 5,
}

const EMPTY_QUEUE_STATUS = {
  size: 0,
  capacity: 0,
  occupancyPercentage: 0,
}

function App() {
  const [notifications, setNotifications] = useState([])
  const [isStopDialogOpen, setIsStopDialogOpen] = useState(false)
  const {
    status,
    history,
    error,
    isLoading,
    isMutating,
    pendingAction,
    clearError,
    refresh,
    start,
    addWorkers,
    updateWorkerPriority,
    stopOneWorker,
    stopWorker,
    stop,
  } = useSimulation()

  const running = status?.running ?? false
  const queue = status?.queue ?? EMPTY_QUEUE_STATUS
  const senders = status?.senders ?? EMPTY_WORKER_STATUS
  const receivers = status?.receivers ?? EMPTY_WORKER_STATUS
  const workers = status?.workers ?? []
  const latestHistory = history.at(-1)
  const messageFlow = {
    produced: status?.messages?.produced ?? 0,
    consumed: status?.messages?.consumed ?? 0,
    productionRate: latestHistory?.productionRate ?? 0,
    consumptionRate: latestHistory?.consumptionRate ?? 0,
  }
  const fieldErrors = error?.problem?.errors ?? null
  const connectionError = Boolean(error && !error.status)

  const dismissNotification = useCallback((notificationId) => {
    setNotifications((current) => (
      current.filter(({ id }) => id !== notificationId)
    ))
  }, [])

  const showSuccess = useCallback((message) => {
    setNotifications((current) => [
      ...current,
      { id: crypto.randomUUID(), message },
    ])
  }, [])

  const runWithFeedback = useCallback(async (operation, successMessage) => {
    try {
      await operation()
      showSuccess(successMessage)
      return true
    } catch {
      return false
    }
  }, [showSuccess])

  async function confirmStopAll() {
    const succeeded = await runWithFeedback(
      () => stop('ALL'),
      'Simülasyondaki tüm worker’lar durduruldu.',
    )

    if (succeeded) {
      setIsStopDialogOpen(false)
    }
  }

  return (
    <main className="app-shell">
      <DashboardHeader
        error={connectionError}
        isLoading={isLoading}
        running={running}
        timestamp={status?.timestamp}
      />

      {error && !fieldErrors && (
        <section className="error-banner" role="alert">
          <div>
            <strong>
              {connectionError
                ? 'Backend bağlantısı kurulamadı'
                : 'İşlem tamamlanamadı'}
            </strong>
            <span>{error.message}</span>
          </div>
          <div className="error-actions">
            {connectionError && (
              <button type="button" onClick={() => refresh()}>
                Yeniden dene
              </button>
            )}
            <button
              type="button"
              className="icon-button"
              aria-label="Hata mesajını kapat"
              onClick={clearError}
            >
              ×
            </button>
          </div>
        </section>
      )}

      {isLoading && !status ? (
        <section className="connection-panel" aria-live="polite">
          <span className="spinner" aria-hidden="true" />
          <div>
            <strong>Simülasyon servisine bağlanılıyor</strong>
            <p>Güncel queue ve worker bilgileri alınıyor.</p>
          </div>
        </section>
      ) : (
        <div className="dashboard-grid">
          <QueueCard
            messageFlow={messageFlow}
            queue={queue}
            running={running}
          />

          <StartSimulationForm
            disabled={isMutating}
            pendingAction={pendingAction}
            running={running}
            serverErrors={fieldErrors}
            onClearError={clearError}
            onStart={(configuration) => runWithFeedback(
              () => start(configuration),
              'Simülasyon başlatıldı.',
            )}
            onStopAll={() => setIsStopDialogOpen(true)}
          />

          <WorkerCard
            key={`sender-${senders.priority}`}
            accent="cyan"
            disabled={isMutating}
            pendingAction={pendingAction}
            running={running}
            status={senders}
            title="Sender workers"
            type="SENDER"
            onAdd={(worker) => runWithFeedback(
              () => addWorkers(worker),
              'Bir sender worker eklendi.',
            )}
            onPriorityChange={(workerPriority) => runWithFeedback(
              () => updateWorkerPriority(workerPriority),
              'Sender thread priority güncellendi.',
            )}
            onStopGroup={(scope) => runWithFeedback(
              () => stop(scope),
              'Tüm sender worker’lar durduruldu.',
            )}
            onStopOne={(type) => runWithFeedback(
              () => stopOneWorker(type),
              'Bir sender worker durduruldu.',
            )}
          />

          <WorkerCard
            key={`receiver-${receivers.priority}`}
            accent="amber"
            disabled={isMutating}
            pendingAction={pendingAction}
            running={running}
            status={receivers}
            title="Receiver workers"
            type="RECEIVER"
            onAdd={(worker) => runWithFeedback(
              () => addWorkers(worker),
              'Bir receiver worker eklendi.',
            )}
            onPriorityChange={(workerPriority) => runWithFeedback(
              () => updateWorkerPriority(workerPriority),
              'Receiver thread priority güncellendi.',
            )}
            onStopGroup={(scope) => runWithFeedback(
              () => stop(scope),
              'Tüm receiver worker’lar durduruldu.',
            )}
            onStopOne={(type) => runWithFeedback(
              () => stopOneWorker(type),
              'Bir receiver worker durduruldu.',
            )}
          />

          <WorkerDetailsPanel
            disabled={isMutating}
            pendingAction={pendingAction}
            workers={workers}
            onStop={(workerId) => runWithFeedback(
              () => stopWorker(workerId),
              'Worker durduruldu.',
            )}
          />

          <TelemetryPanel history={history} />
        </div>
      )}

      <NotificationStack
        notifications={notifications}
        onDismiss={dismissNotification}
      />

      {isStopDialogOpen && (
        <ConfirmDialog
          busy={pendingAction === 'stop-all'}
          description="Aktif sender ve receiver worker’lar güvenli biçimde durdurulacak. Bu işlem mevcut queue’yu sonlandırır."
          title="Tüm simülasyonu durdur?"
          onCancel={() => setIsStopDialogOpen(false)}
          onConfirm={confirmStopAll}
        />
      )}
    </main>
  )
}

export default App

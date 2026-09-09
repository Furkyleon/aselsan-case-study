import './App.css'
import DashboardHeader from './components/DashboardHeader.jsx'
import QueueCard from './components/QueueCard.jsx'
import StartSimulationForm from './components/StartSimulationForm.jsx'
import WorkerCard from './components/WorkerCard.jsx'
import { useSimulation } from './hooks/useSimulation.js'

const EMPTY_WORKER_STATUS = {
  total: 0,
  runnable: 0,
  waiting: 0,
  blocked: 0,
  terminated: 0,
}

const EMPTY_QUEUE_STATUS = {
  size: 0,
  capacity: 0,
  occupancyPercentage: 0,
}

function ignoreHandledError(promise) {
  promise.catch(() => {})
}

function App() {
  const {
    status,
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
    stop,
  } = useSimulation()

  const running = status?.running ?? false
  const queue = status?.queue ?? EMPTY_QUEUE_STATUS
  const senders = status?.senders ?? EMPTY_WORKER_STATUS
  const receivers = status?.receivers ?? EMPTY_WORKER_STATUS

  return (
    <main className="app-shell">
      <DashboardHeader
        error={error}
        isLoading={isLoading}
        running={running}
        timestamp={status?.timestamp}
      />

      {error && (
        <section className="error-banner" role="alert">
          <div>
            <strong>Backend bağlantısı kurulamadı</strong>
            <span>{error.message}</span>
          </div>
          <div className="error-actions">
            <button type="button" onClick={() => refresh()}>
              Yeniden dene
            </button>
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
          <QueueCard queue={queue} running={running} />

          <StartSimulationForm
            disabled={isMutating}
            running={running}
            onStart={(configuration) => start(configuration)}
            onStopAll={() => ignoreHandledError(stop('ALL'))}
          />

          <WorkerCard
            accent="cyan"
            disabled={isMutating}
            pendingAction={pendingAction}
            running={running}
            status={senders}
            title="Sender workers"
            type="SENDER"
            onAdd={(worker) => ignoreHandledError(addWorkers(worker))}
            onPriorityChange={(workerPriority) => (
              ignoreHandledError(updateWorkerPriority(workerPriority))
            )}
            onStopGroup={(scope) => ignoreHandledError(stop(scope))}
            onStopOne={(type) => ignoreHandledError(stopOneWorker(type))}
          />

          <WorkerCard
            accent="amber"
            disabled={isMutating}
            pendingAction={pendingAction}
            running={running}
            status={receivers}
            title="Receiver workers"
            type="RECEIVER"
            onAdd={(worker) => ignoreHandledError(addWorkers(worker))}
            onPriorityChange={(workerPriority) => (
              ignoreHandledError(updateWorkerPriority(workerPriority))
            )}
            onStopGroup={(scope) => ignoreHandledError(stop(scope))}
            onStopOne={(type) => ignoreHandledError(stopOneWorker(type))}
          />
        </div>
      )}
    </main>
  )
}

export default App

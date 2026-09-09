import { useCallback, useEffect, useRef, useState } from 'react'
import { simulationApi } from '../api/simulationApi.js'

const DEFAULT_POLLING_INTERVAL = 1_000
const MAX_HISTORY_POINTS = 60

function getActiveWorkerCount(workerStatus = {}) {
  return (workerStatus.runnable ?? 0)
    + (workerStatus.waiting ?? 0)
    + (workerStatus.blocked ?? 0)
}

function calculateRate(currentValue, previousValue, elapsedSeconds) {
  if (previousValue == null || elapsedSeconds <= 0 || currentValue < previousValue) {
    return 0
  }

  return (currentValue - previousValue) / elapsedSeconds
}

function createHistoryPoint(nextStatus, previousPoint) {
  const produced = nextStatus.messages?.produced ?? 0
  const consumed = nextStatus.messages?.consumed ?? 0
  const timestamp = nextStatus.timestamp ?? new Date().toISOString()
  const elapsedSeconds = previousPoint
    ? (new Date(timestamp).getTime() - new Date(previousPoint.timestamp).getTime()) / 1_000
    : 0

  return {
    timestamp,
    running: nextStatus.running ?? false,
    queueOccupancy: nextStatus.queue?.occupancyPercentage ?? 0,
    queueSize: nextStatus.queue?.size ?? 0,
    produced,
    consumed,
    productionRate: calculateRate(
      produced,
      previousPoint?.produced,
      elapsedSeconds,
    ),
    consumptionRate: calculateRate(
      consumed,
      previousPoint?.consumed,
      elapsedSeconds,
    ),
    senderActive: getActiveWorkerCount(nextStatus.senders),
    receiverActive: getActiveWorkerCount(nextStatus.receivers),
  }
}

export function useSimulation({ pollingInterval = DEFAULT_POLLING_INTERVAL } = {}) {
  const [status, setStatus] = useState(null)
  const [history, setHistory] = useState([])
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [pendingAction, setPendingAction] = useState(null)
  const mountedRef = useRef(false)
  const pollingRequestRef = useRef(null)

  const applyStatus = useCallback((nextStatus) => {
    setStatus(nextStatus)
    setHistory((currentHistory) => {
      const previousPoint = currentHistory.at(-1)

      if (previousPoint?.timestamp === nextStatus.timestamp) {
        return currentHistory
      }

      if (!nextStatus.running && previousPoint && !previousPoint.running) {
        return currentHistory
      }

      const startsNewRun = Boolean(
        nextStatus.running && previousPoint && !previousPoint.running,
      )
      const historyPoint = createHistoryPoint(
        nextStatus,
        startsNewRun ? null : previousPoint,
      )
      const nextHistory = startsNewRun
        ? [historyPoint]
        : [...currentHistory, historyPoint]

      return nextHistory.slice(-MAX_HISTORY_POINTS)
    })
  }, [])

  const refresh = useCallback(async ({ background = false } = {}) => {
    if (pollingRequestRef.current) {
      return
    }

    const controller = new AbortController()
    pollingRequestRef.current = controller

    try {
      const nextStatus = await simulationApi.getStatus({
        signal: controller.signal,
      })

      if (mountedRef.current) {
        applyStatus(nextStatus)
        setError((currentError) => (
          currentError?.status ? currentError : null
        ))
      }
    } catch (requestError) {
      if (requestError.name !== 'AbortError' && mountedRef.current) {
        setError(requestError)
      }
    } finally {
      if (pollingRequestRef.current === controller) {
        pollingRequestRef.current = null
      }

      if (!background && mountedRef.current) {
        setIsLoading(false)
      }
    }
  }, [applyStatus])

  useEffect(() => {
    mountedRef.current = true
    const initialRequestId = window.setTimeout(refresh, 0)

    const intervalId = window.setInterval(() => {
      refresh({ background: true })
    }, pollingInterval)

    return () => {
      mountedRef.current = false
      window.clearTimeout(initialRequestId)
      window.clearInterval(intervalId)
      pollingRequestRef.current?.abort()
      pollingRequestRef.current = null
    }
  }, [pollingInterval, refresh])

  const runAction = useCallback(async (
    actionName,
    operation,
    { returnsStatus = true } = {},
  ) => {
    setPendingAction(actionName)
    setError(null)

    try {
      const result = await operation()

      if (mountedRef.current && returnsStatus) {
        applyStatus(result)
      }

      if (!returnsStatus) {
        await refresh({ background: true })
      }

      return result
    } catch (requestError) {
      if (mountedRef.current) {
        setError(requestError)
      }

      throw requestError
    } finally {
      if (mountedRef.current) {
        setPendingAction(null)
      }
    }
  }, [applyStatus, refresh])

  const start = useCallback(
    (configuration) => runAction(
      'start',
      () => simulationApi.start(configuration),
    ),
    [runAction],
  )

  const addWorkers = useCallback(
    (worker) => runAction(
      `add-${worker.type.toLowerCase()}`,
      () => simulationApi.addWorkers(worker),
    ),
    [runAction],
  )

  const updateWorkerPriority = useCallback(
    (workerPriority) => runAction(
      `priority-${workerPriority.type.toLowerCase()}`,
      () => simulationApi.updateWorkerPriority(workerPriority),
      { returnsStatus: false },
    ),
    [runAction],
  )

  const stopOneWorker = useCallback(
    (type) => runAction(
      `stop-one-${type.toLowerCase()}`,
      () => simulationApi.stopOneWorker(type),
    ),
    [runAction],
  )

  const stopWorker = useCallback(
    (workerId) => runAction(
      `stop-${workerId}`,
      () => simulationApi.stopWorker(workerId),
    ),
    [runAction],
  )

  const stop = useCallback(
    (scope) => runAction(
      `stop-${scope.toLowerCase()}`,
      () => simulationApi.stop(scope),
    ),
    [runAction],
  )

  const clearError = useCallback(() => setError(null), [])

  return {
    status,
    history,
    error,
    isLoading,
    isMutating: pendingAction !== null,
    pendingAction,
    refresh,
    clearError,
    start,
    addWorkers,
    updateWorkerPriority,
    stopOneWorker,
    stopWorker,
    stop,
  }
}

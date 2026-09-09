import { useCallback, useEffect, useRef, useState } from 'react'
import { simulationApi } from '../api/simulationApi.js'

const DEFAULT_POLLING_INTERVAL = 1_000

export function useSimulation({pollingInterval = DEFAULT_POLLING_INTERVAL,} = {}) {
  const [status, setStatus] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [pendingAction, setPendingAction] = useState(null)
  const mountedRef = useRef(false)
  const pollingRequestRef = useRef(null)

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
        setStatus(nextStatus)
        setError(null)
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
  }, [])

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

  const runAction = useCallback(async (actionName, operation, {returnsStatus = true,} = {}) => {
    setPendingAction(actionName)
    setError(null)

    try {
      const result = await operation()

      if (mountedRef.current && returnsStatus) {
        setStatus(result)
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
  }, [refresh])

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

import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { simulationApi } from '../api/simulationApi.js'
import { useSimulation } from './useSimulation.js'

vi.mock('../api/simulationApi.js', () => ({
  simulationApi: {
    getStatus: vi.fn(),
    start: vi.fn(),
    addWorkers: vi.fn(),
    updateWorkerPriority: vi.fn(),
    stopOneWorker: vi.fn(),
    stopWorker: vi.fn(),
    stop: vi.fn(),
  },
}))

function statusAt(second, {
  running = true,
  produced = second,
  consumed = second,
  senderActive = 1,
  receiverActive = 1,
} = {}) {
  return {
    running,
    queue: {
      size: Math.max(0, produced - consumed),
      capacity: running ? 20 : 0,
      occupancyPercentage: running ? 25 : 0,
    },
    messages: { produced, consumed },
    senders: {
      total: senderActive,
      runnable: 0,
      waiting: senderActive,
      blocked: 0,
      terminated: 0,
      priority: 5,
    },
    receivers: {
      total: receiverActive,
      runnable: 0,
      waiting: receiverActive,
      blocked: 0,
      terminated: 0,
      priority: 5,
    },
    workers: [],
    timestamp: new Date(Date.UTC(2026, 0, 1, 0, 0, second)).toISOString(),
  }
}

async function renderSimulationHook(initialStatus) {
  simulationApi.getStatus.mockResolvedValueOnce(initialStatus)
  const hook = renderHook(() => useSimulation({ pollingInterval: 60_000 }))

  await waitFor(() => {
    expect(hook.result.current.status).toEqual(initialStatus)
  })

  return hook
}

async function refreshWith(result, nextStatus) {
  simulationApi.getStatus.mockResolvedValueOnce(nextStatus)

  await act(async () => {
    await result.current.refresh()
  })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('useSimulation history', () => {
  it('calculates production and consumption rates from consecutive samples', async () => {
    const { result } = await renderSimulationHook(statusAt(0, {
      produced: 0,
      consumed: 0,
    }))

    await refreshWith(result, statusAt(1, { produced: 3, consumed: 1 }))

    expect(result.current.history).toHaveLength(2)
    expect(result.current.history.at(-1)).toMatchObject({
      productionRate: 3,
      consumptionRate: 1,
      senderActive: 1,
      receiverActive: 1,
    })
  })

  it('keeps only the latest 60 measurements', async () => {
    const { result } = await renderSimulationHook(statusAt(0))

    for (let second = 1; second <= 65; second += 1) {
      await refreshWith(result, statusAt(second))
    }

    expect(result.current.history).toHaveLength(60)
    expect(result.current.history[0].produced).toBe(6)
    expect(result.current.history.at(-1).produced).toBe(65)
  })

  it('freezes idle history and resets it when a new simulation starts', async () => {
    const { result } = await renderSimulationHook(statusAt(0))

    await refreshWith(result, statusAt(1, {
      running: false,
      produced: 1,
      consumed: 1,
      senderActive: 0,
      receiverActive: 0,
    }))
    await refreshWith(result, statusAt(2, {
      running: false,
      produced: 1,
      consumed: 1,
      senderActive: 0,
      receiverActive: 0,
    }))

    expect(result.current.history).toHaveLength(2)

    await refreshWith(result, statusAt(3, { produced: 0, consumed: 0 }))

    expect(result.current.history).toHaveLength(1)
    expect(result.current.history[0]).toMatchObject({
      produced: 0,
      consumed: 0,
      productionRate: 0,
      consumptionRate: 0,
    })
  })
})

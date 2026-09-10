import { afterEach, describe, expect, it, vi } from 'vitest'
import { simulationApi } from './simulationApi.js'

function createResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: {
      get: vi.fn(() => 'application/json'),
    },
    json: vi.fn().mockResolvedValue(body),
    text: vi.fn().mockResolvedValue(JSON.stringify(body)),
  }
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('simulationApi', () => {
  it('starts a simulation with the expected payload', async () => {
    const status = { running: true }
    const fetchMock = vi.fn().mockResolvedValue(createResponse(status, 201))
    vi.stubGlobal('fetch', fetchMock)

    await expect(simulationApi.start({
      senderCount: 3,
      receiverCount: 2,
      queueCapacity: 20,
    })).resolves.toEqual(status)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/simulation/start',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          senderCount: 3,
          receiverCount: 2,
          queueCapacity: 20,
        }),
      }),
    )
  })

  it('maps ProblemDetail responses to SimulationApiError', async () => {
    const problem = {
      title: 'Validation failed',
      detail: 'Request fields are invalid',
      errors: { queueCapacity: ['must be greater than zero'] },
    }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(createResponse(problem, 400)),
    )

    await expect(simulationApi.start({
      senderCount: 1,
      receiverCount: 1,
      queueCapacity: 0,
    })).rejects.toMatchObject({
      name: 'SimulationApiError',
      message: 'Request fields are invalid',
      status: 400,
      problem,
    })
  })

  it('encodes worker ids before sending a stop request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(createResponse({ running: true }))
    vi.stubGlobal('fetch', fetchMock)

    await simulationApi.stopWorker('worker/id with spaces')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/simulation/workers/worker%2Fid%20with%20spaces',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })
})

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? '/api/simulation'
).replace(/\/$/, '')

export class SimulationApiError extends Error {
  constructor(message, { status, problem } = {}) {
    super(message)
    this.name = 'SimulationApiError'
    this.status = status
    this.problem = problem
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const contentType = response.headers.get('content-type') ?? ''
  const payload = response.status === 204
    ? null
    : contentType.includes('json')
      ? await response.json()
      : await response.text()

  if (!response.ok) {
    const message = typeof payload === 'object' && payload !== null
      ? payload.detail ?? payload.title
      : payload

    throw new SimulationApiError(
      message || `Request failed with status ${response.status}`,
      { status: response.status, problem: payload },
    )
  }

  return payload
}

export const simulationApi = {
  getStatus({ signal } = {}) {
    return request('/status', { signal })
  },

  start({ senderCount, receiverCount, queueCapacity }) {
    return request('/start', {
      method: 'POST',
      body: JSON.stringify({ senderCount, receiverCount, queueCapacity }),
    })
  },

  addWorkers({ type, count }) {
    return request('/workers', {
      method: 'POST',
      body: JSON.stringify({ type, count }),
    })
  },

  updateWorkerPriority({ type, priority }) {
    return request('/workers/priority', {
      method: 'PATCH',
      body: JSON.stringify({ type, priority }),
    })
  },

  stopOneWorker(type) {
    const query = new URLSearchParams({ type })

    return request(`/workers?${query}`, { method: 'DELETE' })
  },

  stopWorker(workerId) {
    return request(`/workers/${encodeURIComponent(workerId)}`, {
      method: 'DELETE',
    })
  },

  stop(scope) {
    return request('/stop', {
      method: 'POST',
      body: JSON.stringify({ scope }),
    })
  },
}

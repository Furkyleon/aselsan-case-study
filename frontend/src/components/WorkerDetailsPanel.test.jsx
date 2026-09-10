import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import WorkerDetailsPanel from './WorkerDetailsPanel.jsx'

const ACTIVE_WORKER_ID = 'a7b5b8b6-5fbc-46b1-9453-8fccc86d229f'
const STOPPED_WORKER_ID = 'f8c2ad38-3a42-42fb-a012-84653a025caf'

const WORKERS = [
  {
    id: ACTIVE_WORKER_ID,
    type: 'SENDER',
    jvmState: 'TIMED_WAITING',
    activityState: 'PRODUCING',
    priority: 8,
    running: true,
    processedMessageCount: 24,
  },
  {
    id: STOPPED_WORKER_ID,
    type: 'RECEIVER',
    jvmState: 'TERMINATED',
    activityState: 'STOPPED',
    priority: 5,
    running: false,
    processedMessageCount: 12,
  },
]

describe('WorkerDetailsPanel', () => {
  it('expands worker details and stops an active worker by id', async () => {
    const user = userEvent.setup()
    const onStop = vi.fn()
    render(
      <WorkerDetailsPanel
        disabled={false}
        pendingAction={null}
        workers={WORKERS}
        onStop={onStop}
      />,
    )

    expect(screen.queryByText(ACTIVE_WORKER_ID)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Worker detayları/ }))

    expect(screen.getByText(ACTIVE_WORKER_ID)).toBeInTheDocument()
    expect(screen.getByText('Mesaj gönderiyor')).toBeInTheDocument()
    expect(screen.getByText('TIMED_WAITING')).toBeInTheDocument()
    expect(screen.getByText('24')).toBeInTheDocument()

    await user.click(screen.getByRole('button', {
      name: `${ACTIVE_WORKER_ID} worker'ını durdur`,
    }))

    expect(onStop).toHaveBeenCalledWith(ACTIVE_WORKER_ID)
    expect(screen.getByRole('button', {
      name: `${STOPPED_WORKER_ID} worker'ını durdur`,
    })).toBeDisabled()
  })

  it('shows a useful empty state', async () => {
    const user = userEvent.setup()
    render(
      <WorkerDetailsPanel
        disabled={false}
        pendingAction={null}
        workers={[]}
        onStop={vi.fn()}
      />,
    )

    await user.click(screen.getByRole('button', { name: /Worker detayları/ }))

    expect(screen.getByText(/Simülasyon başlatıldığında worker kayıtları/))
      .toBeInTheDocument()
  })
})

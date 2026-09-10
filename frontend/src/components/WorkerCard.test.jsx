import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import WorkerCard from './WorkerCard.jsx'

function renderWorkerCard(overrides = {}) {
  const props = {
    accent: 'cyan',
    disabled: false,
    pendingAction: null,
    running: true,
    status: {
      total: 1,
      runnable: 0,
      waiting: 1,
      blocked: 0,
      terminated: 0,
      priority: 8,
    },
    title: 'Sender workers',
    type: 'SENDER',
    onAdd: vi.fn(),
    onPriorityChange: vi.fn().mockResolvedValue(true),
    onStopGroup: vi.fn(),
    onStopOne: vi.fn(),
    ...overrides,
  }

  render(<WorkerCard {...props} />)
  return props
}

describe('WorkerCard', () => {
  it('shows the applied backend priority and submits a changed value', async () => {
    const user = userEvent.setup()
    const props = renderWorkerCard()
    const slider = screen.getByRole('slider', { name: 'Thread priority' })
    const applyButton = screen.getByRole('button', { name: 'Uygula' })

    expect(slider).toHaveValue('8')
    expect(applyButton).toBeDisabled()

    fireEvent.change(slider, { target: { value: '9' } })

    expect(screen.getByText('Uygulanan 8 · Seçilen 9')).toBeInTheDocument()
    expect(applyButton).toBeEnabled()

    await user.click(applyButton)

    expect(props.onPriorityChange).toHaveBeenCalledWith({
      type: 'SENDER',
      priority: 9,
    })
  })

  it('disables worker mutations when there are no active workers', () => {
    renderWorkerCard({
      status: {
        total: 1,
        runnable: 0,
        waiting: 0,
        blocked: 0,
        terminated: 1,
        priority: 8,
      },
    })

    expect(screen.getByRole('slider', { name: 'Thread priority' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Bir sender durdur' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Grubu durdur' })).toBeDisabled()
  })
})

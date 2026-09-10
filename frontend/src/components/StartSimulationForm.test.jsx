import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import StartSimulationForm from './StartSimulationForm.jsx'

function renderForm(overrides = {}) {
  const props = {
    disabled: false,
    pendingAction: null,
    running: false,
    serverErrors: null,
    onClearError: vi.fn(),
    onStart: vi.fn().mockResolvedValue(true),
    onStopAll: vi.fn(),
    ...overrides,
  }

  render(<StartSimulationForm {...props} />)
  return props
}

describe('StartSimulationForm', () => {
  it('submits the initial valid configuration', async () => {
    const user = userEvent.setup()
    const props = renderForm()

    await user.click(screen.getByRole('button', { name: 'Simülasyonu başlat' }))

    expect(props.onStart).toHaveBeenCalledWith({
      senderCount: 3,
      receiverCount: 2,
      queueCapacity: 20,
    })
  })

  it('shows client validation errors and blocks an invalid request', async () => {
    const user = userEvent.setup()
    const props = renderForm()

    fireEvent.change(screen.getByRole('spinbutton', { name: 'Sender' }), {
      target: { value: '-1' },
    })
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Receiver' }), {
      target: { value: '0' },
    })
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Queue kapasitesi' }), {
      target: { value: '0' },
    })
    await user.click(screen.getByRole('button', { name: 'Simülasyonu başlat' }))

    expect(screen.getByText('Sender sayısı negatif olamaz.')).toBeInTheDocument()
    expect(screen.getByText('Queue kapasitesi 1–1000 arasında olmalı.')).toBeInTheDocument()
    expect(screen.getByText('En az bir sender veya receiver gerekli.')).toBeInTheDocument()
    expect(props.onStart).not.toHaveBeenCalled()
  })

  it('renders backend field errors and clears them on edit', async () => {
    const user = userEvent.setup()
    const props = renderForm({
      serverErrors: {
        queueCapacity: ['Queue kapasitesi sunucu sınırını aşıyor.'],
      },
    })

    expect(screen.getByText('Queue kapasitesi sunucu sınırını aşıyor.'))
      .toBeInTheDocument()

    await user.type(
      screen.getByRole('spinbutton', { name: /^Queue kapasitesi/ }),
      '1',
    )

    expect(props.onClearError).toHaveBeenCalled()
  })

  it('exposes the stop-all action while a simulation is running', async () => {
    const user = userEvent.setup()
    const props = renderForm({ running: true })

    await user.click(screen.getByRole('button', { name: 'Tümünü durdur' }))

    expect(props.onStopAll).toHaveBeenCalledOnce()
  })
})

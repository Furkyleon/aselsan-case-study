import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import ConfirmDialog from './ConfirmDialog.jsx'

describe('ConfirmDialog', () => {
  it('supports confirm, cancel and Escape interactions', async () => {
    const user = userEvent.setup()
    const onCancel = vi.fn()
    const onConfirm = vi.fn()
    const { container } = render(
      <ConfirmDialog
        busy={false}
        description="Tüm worker'lar durdurulacak."
        title="Tüm simülasyonu durdur?"
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Evet, tümünü durdur' }))
    await user.click(screen.getByRole('button', { name: 'Vazgeç' }))
    fireEvent.keyDown(container.firstChild, { key: 'Escape' })

    expect(onConfirm).toHaveBeenCalledOnce()
    expect(onCancel).toHaveBeenCalledTimes(2)
  })

  it('locks dismissal while the operation is busy', () => {
    const onCancel = vi.fn()
    const { container } = render(
      <ConfirmDialog
        busy
        description="Tüm worker'lar durdurulacak."
        title="Tüm simülasyonu durdur?"
        onCancel={onCancel}
        onConfirm={vi.fn()}
      />,
    )

    fireEvent.keyDown(container.firstChild, { key: 'Escape' })
    fireEvent.click(container.firstChild)

    expect(onCancel).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Durduruluyor…' })).toBeDisabled()
  })
})

function ConfirmDialog({ busy, description, title, onCancel, onConfirm }) {
  function handleBackdropClick(event) {
    if (event.target === event.currentTarget && !busy) {
      onCancel()
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape' && !busy) {
      onCancel()
    }
  }

  return (
    <div
      className="dialog-backdrop"
      role="presentation"
      onClick={handleBackdropClick}
      onKeyDown={handleKeyDown}
    >
      <section
        className="confirm-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-description"
      >
        <span className="dialog-symbol" aria-hidden="true">!</span>
        <div>
          <p className="eyebrow">CONFIRM ACTION</p>
          <h2 id="confirm-dialog-title">{title}</h2>
          <p id="confirm-dialog-description">{description}</p>
        </div>
        <div className="dialog-actions">
          <button
            type="button"
            className="button button-quiet"
            disabled={busy}
            autoFocus
            onClick={onCancel}
          >
            Vazgeç
          </button>
          <button
            type="button"
            className="button button-danger"
            disabled={busy}
            aria-busy={busy}
            onClick={onConfirm}
          >
            {busy ? 'Durduruluyor…' : 'Evet, tümünü durdur'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default ConfirmDialog

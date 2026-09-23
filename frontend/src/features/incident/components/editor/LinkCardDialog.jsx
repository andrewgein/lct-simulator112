import { useEffect, useRef } from "preact/hooks";
import { applicantName, cardAddress, formatPhone } from "./editorHelpers.js";

export default function LinkCardDialog({ open, cards, selectedId, onSelect, onCancel, onConfirm }) {
  const dialogRef = useRef(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    dialog.open = open;
    const close = () => onCancel();
    dialog.addEventListener("wa-after-hide", close);
    return () => dialog.removeEventListener("wa-after-hide", close);
  }, [open, onCancel]);
  return (
    <wa-dialog ref={dialogRef} class="workspace-link-dialog" label="Выберите карточку для связи" with-footer>
      <div class="workspace-link-table-wrap">
        {cards.length ? <table class="workspace-link-table"><thead><tr><th aria-label="Выбор"></th><th>Карточка</th><th>Заявитель</th><th>Телефон</th><th>Адрес</th><th>Тип происшествия</th></tr></thead><tbody>{cards.map((card) => <tr key={card.cardId} onClick={() => onSelect(card.cardId)}><td><input type="radio" name="linked-card" value={card.cardId} checked={selectedId === card.cardId} onChange={() => onSelect(card.cardId)} /></td><td title={card.cardId}>{card.cardId.slice(0, 8)}</td><td>{applicantName(card)}</td><td>{formatPhone(card.applicant?.phone)}</td><td>{cardAddress(card)}</td><td>{card.incidentTypes?.join(", ") || "—"}</td></tr>)}</tbody></table> : <div class="workspace-link-empty">Нет карточек для связывания</div>}
      </div>
      <wa-button slot="footer" type="button" appearance="outlined" variant="neutral" onClick={onCancel}>Отмена</wa-button>
      <wa-button slot="footer" type="button" variant="brand" disabled={!selectedId} onClick={onConfirm}>Связать</wa-button>
    </wa-dialog>
  );
}

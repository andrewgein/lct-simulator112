import { useEffect, useState } from "preact/hooks";
import { REACTION_STATUS_LABELS, REACTION_STATUS_OPTIONS } from "./ddsLevelHelpers.js";

export default function DdsStageActions({ incidentId, serviceCode, currentStatus, onApply }) {
  const options = REACTION_STATUS_OPTIONS[currentStatus] || [];
  const [status, setStatus] = useState(options[0] || "");
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showCommentError, setShowCommentError] = useState(false);

  useEffect(() => {
    setStatus(options[0] || "");
    setComment("");
    setShowCommentError(false);
  }, [currentStatus]);

  if (!options.length) return null;
  const commentRequired = status === "NOT_ACCEPTED" || status === "WORK_REFUSED";
  const submit = async (event) => {
    if (submitting) return;
    if (commentRequired && !comment.trim()) {
      setShowCommentError(true);
      event.currentTarget.closest(".dds-stage-actions")?.querySelector("wa-input")?.focus();
      return;
    }
    const popover = event.currentTarget.closest("wa-popover");
    setSubmitting(true);
    const saved = await onApply(incidentId, serviceCode, status, comment);
    if (saved) {
      setComment("");
      popover?.hide();
    }
    setSubmitting(false);
  };
  return (
    <div class="dds-stage-actions">
      <wa-select label="Статус реагирования" size="s" value={status} onChange={(event) => { setStatus(event.currentTarget.value); setShowCommentError(false); }}>
        {options.map((value) => <wa-option key={value} value={value}>{REACTION_STATUS_LABELS[value]}</wa-option>)}
      </wa-select>
      <wa-input label="Комментарий" size="s" placeholder={commentRequired ? "Укажите причину отказа" : "Комментарий"} required={commentRequired} value={comment} onInput={(event) => { setComment(event.currentTarget.value); setShowCommentError(false); }}></wa-input>
      {showCommentError && <span class="dds-stage-error" role="alert">Укажите причину отказа</span>}
      <div class="dds-stage-buttons wa-cluster wa-gap-xs wa-justify-content-end">
        <wa-button type="button" size="m" appearance="filled" variant="brand" disabled={submitting} loading={submitting} onClick={submit}><wa-icon slot="start" name="check"></wa-icon>Подтвердить</wa-button>
        <wa-button class="dds-stage-cancel" type="button" size="m" appearance="outlined" variant="neutral" disabled={submitting} data-popover="close" aria-label="Отменить изменение статуса"><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>
      </div>
    </div>
  );
}

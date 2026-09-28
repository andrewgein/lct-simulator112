import { useEffect, useState } from "preact/hooks";

export default function DdsCallResult({ stageId, savedComment, busy, onConfirm }) {
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  useEffect(() => setComment(savedComment || ""), [stageId, savedComment]);

  const submit = async () => {
    if (!comment.trim() || busy || submitting) return;
    setSubmitting(true);
    const saved = await onConfirm(comment.trim());
    if (saved) setComment(comment.trim());
    setSubmitting(false);
  };

  return (
    <div class="saved-card-panel wa-stack wa-gap-s">
      <strong>
        Комментарий по разговору
      </strong>
      <wa-textarea label="Что сообщил собеседник" rows="3" maxlength="4000" value={comment} disabled={busy} onInput={(event) => setComment(event.currentTarget.value)}>
      </wa-textarea>
      {savedComment && <span>
        Комментарий сохранён. Его можно изменить до отправки на разбор.
      </span>}
      <wa-button type="button" appearance="filled" variant="brand" disabled={!comment.trim() || comment.trim() === savedComment || busy || submitting} loading={submitting} onClick={submit}>
        Сохранить комментарий
      </wa-button>
    </div>
  );
}

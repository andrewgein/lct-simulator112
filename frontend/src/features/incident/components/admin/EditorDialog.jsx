import { useEffect, useRef } from "preact/hooks";

export default function EditorDialog({ className, label, open, onCancel, onSave, children }) {
  const dialogRef = useRef(null);
  const formRef = useRef(null);
  const intentionalClose = useRef(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open) intentionalClose.current = false;
    dialog.open = open;
    const handleHide = (event) => {
      if (event.target !== dialog) return;
      if (intentionalClose.current) intentionalClose.current = false;
      else onCancel();
    };
    dialog.addEventListener("wa-after-hide", handleHide);
    return () => dialog.removeEventListener("wa-after-hide", handleHide);
  }, [open, onCancel]);

  const close = (callback) => {
    intentionalClose.current = true;
    callback();
  };
  const submit = (event) => {
    event.preventDefault();
    close(onSave);
  };

  return (
    <wa-dialog ref={dialogRef} class={className} label={label} style="--width: min(90vw, 80rem);" with-footer>
      <form ref={formRef} onSubmit={submit}>
        {children}
      </form>
      <wa-button slot="footer" type="button" appearance="outlined" variant="neutral" onClick={() => close(onCancel)}>Отмена</wa-button>
      <wa-button slot="footer" type="button" variant="success" onClick={() => formRef.current.requestSubmit()}>Сохранить</wa-button>
    </wa-dialog>
  );
}

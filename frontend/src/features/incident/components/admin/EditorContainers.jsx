export function EditorStageContainer({ children, topControl, bottomControl, className = "" }) {
  return <section class={`editor-stage wa-stack wa-gap-m ${className}`}>
    {topControl && <div class="editor-stage-control editor-stage-control-top">{topControl}</div>}
    <div class="editor-stage-content wa-stack wa-gap-m">{children}</div>
    {bottomControl && <div class="editor-stage-control editor-stage-control-bottom">{bottomControl}</div>}
  </section>;
}

export function EditorCallRow({ children }) {
  return <div class="editor-call-row wa-cluster wa-gap-m">{children}</div>;
}

export function EditorCallCard({ children, leftControl, rightControl, className = "" }) {
  return <wa-card class={`editor-call-card ${className}`}>
    {leftControl && <div class="editor-call-control editor-call-control-left">{leftControl}</div>}
    <div class="editor-call-content wa-stack wa-gap-s">{children}</div>
    {rightControl && <div class="editor-call-control editor-call-control-right">{rightControl}</div>}
  </wa-card>;
}

export function EditorAddCard({ children, className = "" }) {
  return <wa-card class={`editor-add-card ${className}`}>{children}</wa-card>;
}

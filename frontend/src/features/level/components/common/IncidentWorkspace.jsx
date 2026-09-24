const styles = `
.incident-workspace { position: fixed; z-index: 1000; inset: 0; display: grid; grid-template-columns: minmax(0, 1fr); grid-template-rows: auto minmax(0, 1fr) auto; min-width: 48rem; overflow: hidden; background: #c8d1d5; color: var(--wa-color-text-normal); }
.workspace-callbar { display: grid; grid-template-columns: minmax(16rem, 1fr) minmax(22rem, 1.5fr) minmax(16rem, 1fr) auto; box-sizing: border-box; width: auto; min-height: 6rem; border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-callbar > div { display: flex; box-sizing: border-box; min-width: 0; padding: var(--wa-space-m) var(--wa-space-l); border-inline-end: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-connection { align-items: center; gap: var(--wa-space-l); }
.workspace-connection wa-icon { color: #35434a; font-size: var(--wa-font-size-2xl); }
.workspace-connection-copy, .workspace-phone, .workspace-incident-meta { display: flex; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone strong { font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; }
.workspace-call-label, .workspace-incident-meta span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.workspace-incident-meta strong { overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.workspace-timer { align-items: center; justify-content: center; min-width: 8rem; background: #293238; color: #ffffff; font-size: var(--wa-font-size-2xl); font-weight: var(--wa-font-weight-bold); font-variant-numeric: tabular-nums; }
.workspace-footer { min-height: 6rem; background: #293238; color: #ffffff; }
.workspace-actions { padding: var(--wa-space-m); }
.workspace-actions wa-button::part(button) { min-width: 8rem; border-color: #ffffff; color: #ffffff; }
@media (max-width: 70rem) { .workspace-callbar { grid-template-columns: 1fr 1.3fr auto; } .workspace-incident-meta { display: none !important; } }
@media (max-width: 48rem) { .incident-workspace { min-width: 0; } .workspace-callbar { grid-template-columns: 1fr auto; } .workspace-phone { display: none !important; } }
`;

export default function IncidentWorkspace({ label, children }) {
  return (
    <div class="incident-workspace" role="dialog" aria-modal="true" aria-label={label}>
      <style>{styles}</style>
      {children}
    </div>
  );
}

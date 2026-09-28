import { useEffect, useState } from "preact/hooks";

const styles = `
  .service-load-indicator { display: flex; align-items: center; cursor: default; }
  .service-load-indicator wa-icon { font-size: var(--wa-font-size-l); }
  .service-load-indicator[data-level="low"] wa-icon { color: #2f9e44; }
  .service-load-indicator[data-level="medium"] wa-icon { color: #e8a70f; }
  .service-load-indicator[data-level="high"] wa-icon { color: #d9363e; }
`;

const LEVEL_LABELS = { low: "Низкая нагрузка", medium: "Средняя нагрузка", high: "Высокая нагрузка" };

function worseLevel(a, b) {
  const order = { low: 0, medium: 1, high: 2 };
  return order[b] > order[a] ? b : a;
}

export function useServiceLoad() {
  const [load, setLoad] = useState(null);

  useEffect(() => {
    const handler = (event) => setLoad(event.detail);
    window.addEventListener("dialog:service_load", handler);
    return () => window.removeEventListener("dialog:service_load", handler);
  }, []);

  return load?.llm && load?.tts ? load : null;
}

export default function ServiceLoadIndicator({ load }) {
  if (!load) return null;

  const level = worseLevel(load.llm.level, load.tts.level);
  const tooltip = `${LEVEL_LABELS[level]} · LLM: ${load.llm.avgMs} мс · TTS: ${load.tts.avgMs} мс`;

  return (
    <>
      <style>{styles}</style>
      <div class="service-load-indicator" data-level={level} title={tooltip} aria-label={tooltip}>
        <wa-icon name="signal" aria-hidden="true"></wa-icon>
      </div>
    </>
  );
}

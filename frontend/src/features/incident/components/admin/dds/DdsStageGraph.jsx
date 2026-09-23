import { hierarchy, tree } from "d3-hierarchy";
import { useEffect, useState } from "preact/hooks";
import EditorDialog from "../EditorDialog.jsx";
import DdsCallEditor, { ddsCallValue, normalizeDdsCall } from "./DdsCallEditor.jsx";

export const DDS_STAGE_TYPES = [
  { value: "ASSIGN_BRIGADE", label: "Назначить бригаду" },
  { value: "WAIT_FOR_BRIGADE_STATUS_CHANGE", label: "Ожидать изменение статуса бригады" },
  { value: "CALL_BRIGADE_FOR_STATUS", label: "Уточнить статус у бригады" },
  { value: "REQUEST_ADDITIONAL_SERVICE", label: "Запросить дополнительную службу" },
  { value: "COMPLETE_INCIDENT", label: "Завершить происшествие" }
];

const stageLabel = (type) => DDS_STAGE_TYPES.find((item) => item.value === type)?.label || "Действие не выбрано";
const stageTimeLimit = (stage) => stage.type === "ASSIGN_BRIGADE" ? 30 : stage.timeLimitSeconds;
const newStage = (pending = false) => ({ id: crypto.randomUUID(), title: "", description: "", type: "", timeLimitSeconds: 60, calls: [], success: null, failure: null, _pending: pending });
const newInitialStage = () => ({ ...newStage(), title: "Подтверждение получения карточки", description: "Принять или не принять карточку в течение 30 секунд после направления в службу.", type: "ASSIGN_BRIGADE", timeLimitSeconds: 30 });

function normalizeStages(incident) {
  const stages = incident?.stages || [];
  if (!stages.length) return null;
  const byId = new Map(stages.map((stage) => [stage.id, { ...stage, timeLimitSeconds: stageTimeLimit(stage), calls: (stage.calls || []).map((call) => normalizeDdsCall(call)), success: null, failure: null }]));
  for (const transition of incident.transitions || []) {
    const stage = byId.get(transition.stageId);
    if (!stage) continue;
    stage.success = byId.get(transition.successStageId) || null;
    stage.failure = byId.get(transition.failureStageId) || null;
  }
  const root = byId.get(incident.initialStageId) || byId.values().next().value || null;
  return root ? { ...root, type: "ASSIGN_BRIGADE", timeLimitSeconds: 30, calls: [] } : null;
}

function updateStage(node, id, updater) {
  if (node.id === id) return updater(node);
  return {
    ...node,
    success: node.success ? updateStage(node.success, id, updater) : null,
    failure: node.failure ? updateStage(node.failure, id, updater) : null
  };
}

function removeStage(node, id) {
  if (!node || node.id === id) return null;
  return {
    ...node,
    success: node.success?.id === id ? null : removeStage(node.success, id),
    failure: node.failure?.id === id ? null : removeStage(node.failure, id)
  };
}

function layoutTree(root) {
  const toLayoutNode = (node, branch = null) => ({
    kind: "stage",
    id: node.id,
    node,
    branch,
    children: [
      node.success ? toLayoutNode(node.success, "success") : { kind: "add", id: `${node.id}-success`, parentId: node.id, branch: "success" },
      node.failure ? toLayoutNode(node.failure, "failure") : { kind: "add", id: `${node.id}-failure`, parentId: node.id, branch: "failure" }
    ]
  });
  const graph = hierarchy(toLayoutNode(root), (item) => item.children);
  tree().nodeSize([400, 220])(graph);
  const descendants = graph.descendants();
  const minX = Math.min(...descendants.map((item) => item.x));
  const maxX = Math.max(...descendants.map((item) => item.x));
  const nodes = descendants.map((item) => ({ ...item.data, x: item.x - minX + 200, y: item.y + 32 }));
  const links = graph.links().map((link) => {
    const sourceY = link.source.y + 120;
    return {
      id: `${link.source.data.id}-${link.target.data.id}`,
      sourceId: link.source.data.id,
      branch: link.target.data.branch,
      sourceX: link.source.x - minX + 200,
      sourceY,
      splitY: sourceY + 24,
      targetX: link.target.x - minX + 200,
      targetY: link.target.y + 32
    };
  });
  const trunks = [...new Map(links.map((link) => [link.sourceId, { id: link.sourceId, x: link.sourceX, sourceY: link.sourceY, splitY: link.splitY }])).values()];
  return { nodes, links, trunks, width: Math.max(maxX - minX + 400, 720), height: Math.max(...nodes.map((item) => item.y)) + 210 };
}

export function ddsTreeValue(root) {
  const stages = [];
  const transitions = [];
  const walk = (node) => {
    stages.push({
      id: node.id,
      title: node.title.trim(),
      description: node.description.trim() || null,
      type: node.type,
      timeLimitSeconds: Number(stageTimeLimit(node)),
      calls: node.type === "CALL_BRIGADE_FOR_STATUS" ? node.calls.map(ddsCallValue) : []
    });
    if (node.success || node.failure) transitions.push({ stageId: node.id, successStageId: node.success?.id || null, failureStageId: node.failure?.id || null });
    if (node.success) walk(node.success);
    if (node.failure) walk(node.failure);
  };
  walk(root);
  return { stages, initialStageId: root.id, transitions };
}

export function validateDdsTree(root) {
  const walk = (node, path) => {
    const name = node.title.trim() || path;
    if (!node.title.trim()) throw new Error(`Укажите название этапа: ${path}`);
    if (!node.type) throw new Error(`Выберите тип этапа: ${name}`);
    if (node.id !== root.id && node.type === "ASSIGN_BRIGADE") throw new Error("Этап «Принять / не принять» может быть только первым");
    if (!Number.isInteger(Number(node.timeLimitSeconds)) || Number(node.timeLimitSeconds) <= 0) throw new Error(`Укажите положительный лимит времени: ${name}`);
    if (node.type === "CALL_BRIGADE_FOR_STATUS" && !node.calls.length) throw new Error(`Добавьте исходящий звонок бригаде: ${name}`);
    if (node.success) walk(node.success, `${name} → успех`);
    if (node.failure) walk(node.failure, `${name} → ошибка`);
  };
  walk(root, "начальный этап");
}

function DdsStageNode({ node, branch, root, incidentAddress, onUpdate, onRemove, style }) {
  const [open, setOpen] = useState(Boolean(node._pending));
  const [draft, setDraft] = useState(() => node._pending ? structuredClone({ ...node, success: undefined, failure: undefined, _pending: undefined }) : null);
  const edit = () => { setDraft(structuredClone({ ...node, success: undefined, failure: undefined, _pending: undefined })); setOpen(true); };
  const save = () => {
    onUpdate(node.id, (current) => ({ ...current, ...draft, _pending: false, success: current.success, failure: current.failure, calls: draft.type === "CALL_BRIGADE_FOR_STATUS" ? draft.calls : [] }));
    setOpen(false);
  };
  const cancel = () => {
    setOpen(false);
    if (node._pending) onRemove();
  };
  const updateDraft = (field, value) => setDraft((current) => ({ ...current, [field]: value }));
  return (
    <div class="dds-tree-node" style={style}>
      <wa-card class={`dds-stage-card ${root ? "dds-stage-card--root" : `dds-stage-card--${branch}`}`} appearance="filled-outlined">
        <div class="wa-stack wa-gap-xs">
          <div class="wa-split wa-flex-nowrap">
            <h3 class="dds-stage-title">{node.title || "Этап без названия"}</h3>
            <div class="wa-cluster wa-gap-2xs">
              <wa-button type="button" size="xs" appearance="plain" onClick={edit}><wa-icon name="pencil" label="Редактировать этап"></wa-icon></wa-button>
              {!root && <wa-button type="button" size="xs" appearance="plain" variant="danger" onClick={onRemove}><wa-icon name="trash" label="Удалить ветку"></wa-icon></wa-button>}
            </div>
          </div>
          <span class="dds-stage-meta">{stageLabel(node.type)} · {stageTimeLimit(node) || 0} сек.</span>
        </div>
      </wa-card>
      <EditorDialog className="dds-stage-dialog" label={root ? "Начальный этап ДДС" : "Этап ДДС"} open={open} onCancel={cancel} onSave={save}>
        {draft && <div class="wa-stack wa-gap-l">
          <div class="wa-grid">
            <wa-input value={draft.title} label="Название этапа" required onInput={(event) => updateDraft("title", event.currentTarget.value)}></wa-input>
            <wa-select value={draft.type} label="Действие этапа" required disabled={root} onChange={(event) => { const type = event.currentTarget.value; setDraft((current) => ({ ...current, type, timeLimitSeconds: type === "ASSIGN_BRIGADE" ? 30 : current.timeLimitSeconds })); }}>
              {DDS_STAGE_TYPES.filter((type) => root || type.value !== "ASSIGN_BRIGADE").map((type) => <wa-option key={type.value} value={type.value}>{type.label}</wa-option>)}
            </wa-select>
            <wa-number-input value={stageTimeLimit(draft)} label="Лимит времени, секунд" min="1" step="1" required disabled={draft.type === "ASSIGN_BRIGADE"} helpText={draft.type === "ASSIGN_BRIGADE" ? "Подтверждение получения карточки выполняется в течение 30 секунд после направления в службу" : undefined} onInput={(event) => updateDraft("timeLimitSeconds", event.currentTarget.value)}></wa-number-input>
          </div>
          <wa-textarea value={draft.description} label="Описание ожидаемого действия" rows="4" onInput={(event) => updateDraft("description", event.currentTarget.value)}></wa-textarea>
          {draft.type === "CALL_BRIGADE_FOR_STATUS" && <section class="wa-stack wa-gap-m">
            <div class="wa-split wa-align-items-center">
              <h3 class="wa-heading-l">Исходящие звонки бригаде</h3>
              <wa-button type="button" appearance="outlined" variant="brand" onClick={() => updateDraft("calls", [...draft.calls, normalizeDdsCall()])}><wa-icon name="plus" slot="start"></wa-icon>Добавить звонок</wa-button>
            </div>
            {!draft.calls.length && <wa-callout variant="warning">Для этого этапа нужен хотя бы один исходящий звонок бригаде.</wa-callout>}
            {draft.calls.map((call, index) => <DdsCallEditor key={call.key} call={call} index={index} incidentAddress={incidentAddress} onChange={(value) => updateDraft("calls", draft.calls.map((item) => item.key === call.key ? value : item))} onRemove={() => updateDraft("calls", draft.calls.filter((item) => item.key !== call.key))} />)}
          </section>}
        </div>}
      </EditorDialog>
    </div>
  );
}

export default function DdsStageGraph({ initialIncident, incidentAddress, onChange }) {
  const [root, setRoot] = useState(() => normalizeStages(initialIncident) || newInitialStage());
  useEffect(() => onChange(root), []);
  const update = (id, updater) => setRoot((current) => {
    const next = updateStage(current, id, updater);
    onChange(next);
    return next;
  });
  const remove = (id) => setRoot((current) => {
    const next = removeStage(current, id);
    onChange(next);
    return next;
  });
  const add = (parentId, branch) => update(parentId, (current) => ({ ...current, [branch]: newStage(true) }));
  const layout = layoutTree(root);
  return (
    <section class="wa-stack wa-gap-m">
      <div class="dds-tree-heading wa-split wa-align-items-end">
        <div>
          <h2 class="wa-heading-xl">Сценарий реагирования</h2>
          <p class="dds-section-hint">Постройте последовательность действий: каждый этап может продолжиться по ветке успеха или ошибки.</p>
        </div>
        <span class="dds-tree-note">Удаление этапа удалит всю ветку ниже</span>
      </div>
      <div class="dds-tree-scroller">
        <div class="dds-tree" style={{ width: `${layout.width}px`, height: `${layout.height}px` }}>
          <svg class="dds-tree-links" width={layout.width} height={layout.height} aria-hidden="true">
            {layout.trunks.map((trunk) => <path key={trunk.id} class="dds-tree-link dds-tree-link--trunk" d={`M ${trunk.x} ${trunk.sourceY} V ${trunk.splitY}`}></path>)}
            {layout.links.map((link) => <path key={link.id} class={`dds-tree-link dds-tree-link--${link.branch}`} d={`M ${link.sourceX} ${link.splitY} C ${link.sourceX} ${(link.splitY + link.targetY) / 2}, ${link.targetX} ${(link.splitY + link.targetY) / 2}, ${link.targetX} ${link.targetY}`}></path>)}
          </svg>
          {layout.nodes.map((item) => item.kind === "stage" ? <DdsStageNode key={item.id} node={item.node} branch={item.branch} root={item.node.id === root.id} incidentAddress={incidentAddress} onUpdate={update} onRemove={() => remove(item.id)} style={{ left: `${item.x}px`, top: `${item.y}px` }} /> : <div key={item.id} class={`dds-add-node dds-add-node--${item.branch}`} style={{ left: `${item.x}px`, top: `${item.y}px` }}>
            <wa-button class="add-branch" type="button" size="small" appearance="filled" variant={item.branch === "success" ? "success" : "danger"} onClick={() => add(item.parentId, item.branch)}>{item.branch === "success" ? "Успех — добавить этап" : "Ошибка — добавить этап"}</wa-button>
          </div>)}
        </div>
      </div>
    </section>
  );
}

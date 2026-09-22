import { useState } from "preact/hooks";
import DataGrid from "../../../../components/DataGrid.jsx";
import { applicantName, cardAddress, cardIsComplete, findIncident } from "../../../incident/components/editor/editorHelpers";

function arrangeCards(cards) {
  const childrenByMain = new Map();
  cards.forEach((card) => {
    if (!card.mainCardId) return;
    if (!childrenByMain.has(card.mainCardId)) childrenByMain.set(card.mainCardId, []);
    childrenByMain.get(card.mainCardId).push(card);
  });
  const roots = cards.filter((card) => !card.mainCardId || !cards.some((item) => item.cardId === card.mainCardId));
  return roots.map((card) => ({ card, children: childrenByMain.get(card.cardId) || [] }));
}

function createRow(card, kind, depth, relationCount, expanded, classifierState) {
  const complete = cardIsComplete(card, classifierState.classifier);
  const incidents = (card.incidentTypes || []).map((code) => findIncident(classifierState.classifier, code)).filter(Boolean);
  const incident = incidents[0];
  const victimSummary = card.victimCount > 0 ? `Есть · ${card.victimCount}` : "Нет";
  const additionalInfo = (incident?.fields || []).map((field) => `${field.name}: ${card.additionalInfo?.[field.id] ?? "—"}`).join(" · ");
  return {
    id: card.cardId,
    card,
    depth,
    kind,
    relationCount,
    expanded,
    incident: incidents.map((item) => item.finalName).join(" · ") || (classifierState.loading ? "Загрузка типа…" : "Тип не указан"),
    applicant: applicantName(card),
    applicantPhone: card.applicant?.phone || "Не указан",
    applicantContactPhone: card.applicant?.contactPhone || "Не указан",
    applicantOnScenePhone: card.applicant?.onScenePhone || "Не указан",
    victimSummary,
    address: cardAddress(card),
    additionalInfo: additionalInfo || "Дополнительная информация не заполнена",
    kindLabel: kind === "child" ? "Связанная" : "Основная",
    complete,
    status: complete ? "Заполнена" : "Заполнена не полностью"
  };
}

function flattenBranches(branches, collapsed, classifierState) {
  const rows = [];
  branches.forEach((branch) => {
    const expanded = !collapsed.has(branch.card.cardId);
    rows.push(createRow(branch.card, "primary", 0, branch.children.length, expanded, classifierState));
    if (expanded) branch.children.forEach((card) => rows.push(createRow(card, "child", 1, 0, false, classifierState)));
  });
  return rows;
}

export default function ActiveCards({ cards, loading, error, classifierState, searchQuery = "", onOpen }) {
  const [collapsed, setCollapsed] = useState(() => new Set());
  const [expandedDetails, setExpandedDetails] = useState(() => new Set());
  const rows = flattenBranches(arrangeCards(cards), collapsed, classifierState);
  const toggle = (id) => setCollapsed((current) => {
    const next = new Set(current);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  });
  const toggleDetails = (id) => setExpandedDetails((current) => {
    const next = new Set(current);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  });
  const columns = [
    {
      field: "details",
      label: "",
      sortable: false,
      render: (row) => <wa-button class="card-details-toggle" type="button" size="xs" appearance="plain" variant="neutral" aria-expanded={expandedDetails.has(row.id)} aria-label={`${expandedDetails.has(row.id) ? "Скрыть" : "Показать"} информацию о карточке`} onClick={() => toggleDetails(row.id)}><wa-icon name={expandedDetails.has(row.id) ? "chevron-up" : "chevron-down"}></wa-icon></wa-button>
    },
    {
      field: "relation",
      label: "Связи",
      sortable: false,
      render: (row) => (
        <span class="card-relation-indent" style={{ "--card-depth": row.depth }}>
          {row.kind !== "primary" && <wa-icon name="link" label={row.kindLabel}></wa-icon>}
          {row.kind === "primary" && !row.relationCount && <wa-icon name="minus" aria-hidden="true"></wa-icon>}
          {!!row.relationCount && (
            <wa-button class="card-collapse" type="button" size="xs" appearance="plain" variant="neutral" aria-expanded={row.expanded} aria-label={`${row.expanded ? "Скрыть" : "Показать"} связанные карточки (${row.relationCount})`} onClick={() => toggle(row.id)}>
              <wa-icon name={row.expanded ? "chevron-down" : "chevron-right"} aria-hidden="true"></wa-icon>
              <span>{row.relationCount}</span>
            </wa-button>
          )}
        </span>
      )
    },
    { field: "id", label: "Номер", sortable: false },
    { field: "incident", label: "Тип происшествия", sortable: false, render: (row) => <strong title={row.incident}>{row.incident}</strong> },
    { field: "applicant", label: "Заявитель", sortable: false },
    { field: "address", label: "Адрес", sortable: false },
    { field: "kindLabel", label: "Вид", sortable: false },
    { field: "status", label: "Заполнение", sortable: false, render: (row) => <span class="card-status-content wa-cluster wa-gap-xs wa-flex-nowrap"><wa-icon name={row.complete ? "circle-check" : "triangle-exclamation"} aria-hidden="true"></wa-icon><span>{row.status}</span></span> }
  ];
  return (
    <section class="active-cards wa-stack wa-gap-0" aria-labelledby="active-cards-heading">
      {loading && <div>Загрузка...</div>}
      {error && <div>Не удалось загрузить карточки</div>}
      {!loading && !error && !cards.length && <wa-callout variant="neutral">Активных карточек пока нет</wa-callout>}
      {!loading && !error && !!cards.length && (
        <>
          <div class="incident-list-heading wa-split wa-align-items-center">
            <h2 id="active-cards-heading">Список происшествий</h2>
            <span>{rows.length} {rows.length === 1 ? "карточка" : "карточек"}</span>
          </div>
          <DataGrid data={rows} columns={columns} label="Список активных карточек происшествий" pageSize={Math.max(rows.length, 1)} searchable={false} searchValue={searchQuery} className="cards-grid" tableClassName="cards-table" onRowClick={(row) => onOpen(row.card)} getRowClassName={(row) => row.complete ? "is-complete" : "is-incomplete"} renderExpandedRow={(row) => expandedDetails.has(row.id) ? <div class="incident-row-details"><div><span>Заявитель:</span><strong>{row.applicant}</strong><span>АОН: {row.applicantPhone}</span><span>Предоставленный: {row.applicantContactPhone}</span><span>На место: {row.applicantOnScenePhone}</span></div><div><span>Пострадавшие:</span><strong>{row.victimSummary}</strong></div><div><span>Адрес:</span><strong>{row.address}</strong></div><div class="incident-row-information"><span>Информация:</span><strong>{row.additionalInfo}</strong></div><div><span>Карточка:</span><strong>{row.kindLabel} · {row.status}</strong></div></div> : null} />
        </>
      )}
    </section>
  );
}

import { useState } from "preact/hooks";
import DataGrid from "../../../../components/DataGrid.jsx";
import { applicantName, cardAddress, cardIsComplete, findIncident } from "../../../incident/components/editor/editorHelpers";

const dateFormatter = new Intl.DateTimeFormat("ru-RU", { day: "2-digit", month: "2-digit", year: "2-digit" });
const timeFormatter = new Intl.DateTimeFormat("ru-RU", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });

const styles = `
.active-cards { padding: var(--wa-space-l); }
.incident-list-heading { padding: var(--wa-space-m) var(--wa-space-l); color: var(--wa-color-surface-default); }
.incident-list-heading h2 { margin: 0; font-size: var(--wa-font-size-xl); }
.incident-list-heading span { color: var(--wa-color-neutral-80); font-size: var(--wa-font-size-s); }
.data-grid.cards-grid { border: 0; border-radius: 0; background: var(--app-dispatch-workspace); --wa-color-surface-border: var(--app-dispatch-border); }
.cards-table { min-width: 78rem; border-collapse: separate; border-spacing: 0; color: var(--app-dispatch-text); --wa-color-fill-quiet: var(--app-dispatch-row-hover); --wa-color-border-quiet: var(--app-dispatch-border); --wa-color-border-normal: var(--wa-color-neutral-50); }
.cards-table thead { background: var(--app-dispatch-workspace); color: var(--wa-color-neutral-90); }
.cards-table th { padding: var(--wa-space-s) var(--wa-space-m); border-block-end: 0; font-weight: var(--wa-font-weight-semibold); white-space: nowrap; vertical-align: middle; }
.cards-table td { box-sizing: border-box; max-width: 24rem; padding: var(--wa-space-xs) var(--wa-space-s); overflow: hidden; border-inline-end: var(--wa-border-width-s) solid var(--app-dispatch-border); background: var(--app-dispatch-row-alternate); text-overflow: ellipsis; white-space: nowrap; vertical-align: middle; }
.cards-table tbody .data-grid__row td { border-block: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.cards-table tbody .data-grid__row td:first-child { border-inline-start: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.cards-table tbody .data-grid__row .data-grid__cell--time, .cards-table tbody .data-grid__row .data-grid__cell--incident, .cards-table tbody .data-grid__row .data-grid__cell--address { background: var(--app-dispatch-row); }
.cards-table tbody .data-grid__row:hover td, .cards-table tbody .data-grid__row:focus-visible td { filter: brightness(1.12); }
.cards-table tbody tr:focus-visible { outline: var(--wa-focus-ring-width) var(--wa-focus-ring-style) var(--wa-color-focus); outline-offset: calc(-1 * var(--wa-focus-ring-width)); }
.cards-table .data-grid__heading:first-child, .cards-table .data-grid__cell--details { width: 3rem; padding-inline: var(--wa-space-xs); text-align: center; }
.cards-table .data-grid__cell--relation { width: 6rem; color: var(--app-dispatch-text-secondary); text-align: center; }
.cards-table .data-grid__cell--relation:has(.card-relation-linked) { background: var(--app-dispatch-relation); color: var(--app-dispatch-text); }
.card-relation-linked { display: inline-flex; align-items: center; justify-content: center; }
.card-details-toggle { --wa-color-fill-quiet: transparent; --wa-color-neutral-on-quiet: var(--app-dispatch-text); --wa-color-neutral-on-normal: var(--wa-color-neutral-on-loud); }
.cards-table .data-grid__cell--id { width: 9rem; color: var(--app-dispatch-text-secondary); }
.cards-table .data-grid__cell--date, .cards-table .data-grid__cell--time { width: 6rem; }
.cards-table .data-grid__cell--time { font-weight: var(--wa-font-weight-semibold); }
.cards-table .data-grid__cell--incident { width: 25%; }
.cards-table .data-grid__cell--victimSummary { width: 7rem; }
.cards-table .data-grid__cell--status { width: 13rem; color: var(--app-dispatch-text-secondary); }
.cards-table .data-grid__cell--address { width: 32%; }
.cards-table .data-grid__cell--open { width: 4rem; padding-inline: var(--wa-space-xs); text-align: center; }
.cards-table .data-grid__expanded-row td { max-width: none; padding: 0; border: var(--wa-border-width-s) solid var(--app-dispatch-border); border-block-start: 0; background: var(--app-dispatch-row-alternate); white-space: normal; }
.cards-table .data-grid__expanded-row:not(:last-child) td { border-block-end: var(--wa-space-2xs) solid var(--app-dispatch-workspace); }
.cards-table .data-grid__expanded-row:hover td { background: var(--app-dispatch-row-alternate); }
.incident-description { display: grid; grid-template-columns: 7rem minmax(0, 1fr); align-items: baseline; gap: var(--wa-space-s); padding: var(--wa-space-xs) var(--wa-space-m); overflow-wrap: anywhere; }
.incident-description > span { color: var(--app-dispatch-text-muted); }
.incident-description > p { margin: 0; }
.incident-row-details { display: grid; border-block-start: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.incident-row-details > div { display: grid; grid-template-columns: 7rem minmax(0, 1fr); align-items: baseline; gap: var(--wa-space-s); min-width: 0; padding: var(--wa-space-xs) var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.incident-row-details > div:last-child { border-block-end: 0; }
.incident-detail-label, .incident-detail-content span { color: var(--app-dispatch-text-muted); }
.incident-detail-content { display: flex; flex-wrap: wrap; align-items: baseline; gap: var(--wa-space-xs) var(--wa-space-s); min-width: 0; }
.incident-detail-created { margin-inline-start: auto; }
.incident-row-details strong { overflow-wrap: anywhere; }
.incident-row-information strong { font-weight: var(--wa-font-weight-normal); }
@media (max-width: 40rem) { .active-cards { padding: var(--wa-space-xs); } .cards-grid { margin-inline: calc(-1 * var(--wa-space-xs)); } }
`;

function arrangeCards(cards) {
  const childrenByMain = new Map();
  cards.forEach((card) => {
    if (!card.mainCardId) return;
    if (!childrenByMain.has(card.mainCardId)) childrenByMain.set(card.mainCardId, []);
    childrenByMain.get(card.mainCardId).push(card);
  });
  const cardIds = new Set(cards.map((card) => card.cardId));
  const roots = cards.filter((card) => !card.mainCardId || !cardIds.has(card.mainCardId));
  const buildNode = (card) => ({ card, children: (childrenByMain.get(card.cardId) || []).map(buildNode) });
  return roots.map(buildNode);
}

function createRow(card, kind, depth, relationCount, expanded, classifierState, getCardMeta) {
  const meta = getCardMeta?.(card) || {};
  const complete = meta.complete ?? cardIsComplete(card, classifierState.classifier);
  const incidents = (card.incidentTypes || []).map((code) => findIncident(classifierState.classifier, code)).filter(Boolean);
  const incident = incidents[0];
  const victimSummary = card.victimCount > 0 ? `Есть · ${card.victimCount}` : "Нет";
  const classifierFeatures = incident ? [incident.feature1Name, incident.feature2Name, incident.feature3Name, incident.additionalFeatures].filter(Boolean) : [];
  const factsByCode = new Map((classifierState.routingFacts || []).map((fact) => [fact.code, fact]));
  const selectedFacts = (incident?.routingFactCodes || []).map((code) => factsByCode.get(code)).filter((fact) => fact && card.additionalInfo?.[fact.code]).map((fact) => `${fact.label}: ${fact.options.find((option) => option.value === card.additionalInfo[fact.code])?.label || card.additionalInfo[fact.code]}`);
  const additionalInfo = [...classifierFeatures, ...selectedFacts].join(" · ");
  const description = card.applicant?.additionalInfo?.trim() || "Описание не указано";
  const timestamp = meta.receivedAt ?? (getCardMeta ? null : card.createdAt);
  const date = timestamp ? new Date(timestamp) : null;
  const createdAt = card.createdAt ? new Date(card.createdAt) : null;
  const createdAtLabel = createdAt && !Number.isNaN(createdAt.getTime()) ? `${dateFormatter.format(createdAt)} ${timeFormatter.format(createdAt)}` : "—";
  return {
    id: card.cardId,
    card,
    timestamp: date && !Number.isNaN(date.getTime()) ? date.getTime() : 0,
    date: date && !Number.isNaN(date.getTime()) ? dateFormatter.format(date) : "—",
    time: date && !Number.isNaN(date.getTime()) ? timeFormatter.format(date) : "—",
    kind,
    depth,
    expanded,
    relationCount,
    incident: incidents.map((item) => item.finalName).join(" · ") || (classifierState.loading ? "Загрузка типа…" : "Тип не указан"),
    applicant: applicantName(card),
    description,
    createdAtLabel,
    applicantPhone: card.applicant?.phone || "Не указан",
    applicantContactPhone: card.applicant?.contactPhone || "Не указан",
    victimCount: card.victimCount,
    victimSummary,
    address: cardAddress(card),
    additionalInfo: additionalInfo || "Дополнительная информация не заполнена",
    services: card.services?.join(", ") || "Службы не указаны",
    kindLabel: meta.kindLabel || (kind === "child" ? "Связанная" : "Основная"),
    complete,
    rowClassName: meta.className || (complete ? "is-complete" : "is-incomplete"),
    status: meta.status || (complete ? "Заполнена" : "Заполнена не полностью")
  };
}

function flattenBranches(nodes, depth, collapsed, classifierState, getCardMeta, rows = []) {
  nodes.forEach((node) => {
    const expanded = !collapsed.has(node.card.cardId);
    rows.push(createRow(node.card, depth === 0 ? "primary" : "child", depth, node.children.length, expanded, classifierState, getCardMeta));
    if (expanded) flattenBranches(node.children, depth + 1, collapsed, classifierState, getCardMeta, rows);
  });
  return rows;
}

export default function ActiveCards({ cards, loading, error, classifierState, searchQuery = "", onOpen, getCardMeta, heading = "Список происшествий", emptyMessage = "Активных карточек пока нет", statusLabel = "Заполнение" }) {
  const [expandedDetails, setExpandedDetails] = useState(() => new Set());
  const [collapsed, setCollapsed] = useState(() => new Set());
  const rows = flattenBranches(arrangeCards(cards), 0, collapsed, classifierState, getCardMeta);
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
      render: (row) => row.relationCount > 0 ? <wa-button type="button" size="xs" appearance="plain" aria-expanded={row.expanded} aria-label={row.expanded ? "Свернуть связанные карточки" : "Показать связанные карточки"} onClick={() => toggle(row.id)}><wa-icon name={row.expanded ? "chevron-down" : "chevron-right"} label="Есть связанные карточки"></wa-icon></wa-button> : row.kind === "child" ? <wa-icon name="link" label="Связанная карточка"></wa-icon> : null
    },
    { field: "id", label: "Номер", searchValue: (row) => `${row.id} ${row.applicant} ${row.description}` },
    { field: "date", label: "Дата", sortValue: (row) => row.timestamp },
    { field: "time", label: "Время", sortValue: (row) => row.timestamp },
    { field: "incident", label: "Тип происшествия", render: (row) => <strong title={row.incident} style={{ paddingInlineStart: `${row.depth * 1.25}rem` }}>{row.incident}</strong> },
    { field: "victimSummary", label: "Постр.", sortValue: (row) => row.victimCount },
    { field: "address", label: "Адрес" },
    { field: "status", label: statusLabel },
    { field: "open", label: "", sortable: false, render: (row) => <span data-ignore-row-click><wa-icon name="clipboard" label={`Карточка ${row.id}`}></wa-icon></span> }
  ];
  return (
    <section class="active-cards wa-stack wa-gap-0" aria-labelledby="active-cards-heading">
      <style>{styles}</style>
      {loading && <div>Загрузка...</div>}
      {error && <div>Не удалось загрузить карточки</div>}
      {!loading && !error && !cards.length && <wa-callout variant="neutral">{emptyMessage}</wa-callout>}
      {!loading && !error && !!cards.length && (
        <>
          <div class="incident-list-heading wa-split wa-align-items-center">
            <h2 id="active-cards-heading">{heading}</h2>
            <span>{rows.length} {rows.length === 1 ? "карточка" : "карточек"}</span>
          </div>
          <DataGrid data={rows} columns={columns} label={heading} pageSize={Math.max(rows.length, 1)} searchable={false} searchValue={searchQuery} className="cards-grid" tableClassName="cards-table" onRowClick={(row) => onOpen(row.card)} getRowClassName={(row) => row.rowClassName} renderExpandedRow={(row) => <div><div class="incident-description"><span>Описание:</span><p>{row.description}</p></div>{expandedDetails.has(row.id) && <div class="incident-row-details"><div><span class="incident-detail-label">Службы:</span><div class="incident-detail-content"><strong>{row.services}</strong></div></div><div><span class="incident-detail-label">Заявитель:</span><div class="incident-detail-content"><strong>{row.applicant}</strong><span>АОН:</span><strong>{row.applicantPhone}</strong><span>Предоставленный телефон:</span><strong>{row.applicantContactPhone}</strong><span class="incident-detail-created">Создание:</span><strong>{row.createdAtLabel}</strong></div></div><div class="incident-row-information"><span class="incident-detail-label">Информация:</span><div class="incident-detail-content"><strong>{row.additionalInfo} · Пострадавшие: {row.victimSummary}</strong></div></div><div><span class="incident-detail-label">Отработки:</span><div class="incident-detail-content"><strong>{row.kindLabel} · {row.status}</strong></div></div></div>}</div>} />
        </>
      )}
    </section>
  );
}

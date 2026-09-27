import { Fragment } from "preact";
import { useMemo, useState } from "preact/hooks";
import { constructTable, createSortedRowModel, rowSortingFeature, tableFeatures } from "@tanstack/table-core";
import { storeReactivityBindings } from "@tanstack/table-core/store-reactivity-bindings";

const tableFeaturesConfig = tableFeatures({ coreReactivityFeature: storeReactivityBindings(), rowSortingFeature, sortedRowModel: createSortedRowModel() });
const styles = `
.data-grid { overflow: hidden; border: var(--wa-border-style) var(--wa-panel-border-width) var(--wa-color-surface-border); border-radius: var(--wa-border-radius-m); }
.data-grid__toolbar, .data-grid__footer { display: flex; align-items: center; justify-content: space-between; gap: var(--wa-space-m); padding: var(--wa-space-m); }
.data-grid__toolbar { border-block-end: var(--wa-border-style) var(--wa-panel-border-width) var(--wa-color-surface-border); }
.data-grid__toolbar wa-input { max-width: 20rem; }
.data-grid__summary, .data-grid__footer { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.data-grid__scroll { overflow-x: auto; }
.data-grid__table { width: 100%; min-width: 700px; }
.data-grid__sort { display: flex; width: 100%; align-items: center; justify-content: flex-start; gap: var(--wa-space-xs); padding: 0; border: 0; color: inherit; background: transparent; cursor: pointer; font: inherit; text-align: inherit; }
.data-grid__heading--end .data-grid__sort { justify-content: flex-end; }
.data-grid__sort-indicator { display: inline-flex; width: 1rem; min-width: 1rem; justify-content: center; opacity: 0; }
.data-grid__sort:hover .data-grid__sort-indicator, .data-grid__sort:focus-visible .data-grid__sort-indicator, .data-grid__sort-indicator--active { opacity: 1; }
.data-grid__empty { padding: var(--wa-space-2xl); color: var(--wa-color-text-quiet); text-align: center; }
.data-grid__pagination { display: flex; gap: var(--wa-space-xs); }
.data-grid__row--interactive { cursor: pointer; }
@media (max-width: 600px) { .data-grid__toolbar, .data-grid__footer { align-items: stretch; flex-direction: column; } .data-grid__toolbar wa-input { max-width: none; } }
`;

const formatValue = (value) => {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "boolean") return value ? "Да" : "Нет";
  return String(value);
};

export default function DataGrid({ data = [], columns = [], label = "Таблица", pageSize = 10, searchable = true, searchValue, onSearchChange, className = "", tableClassName = "", onRowClick, getRowClassName, renderExpandedRow }) {
  const [search, setSearch] = useState("");
  const effectiveSearch = searchValue ?? search;
  const [sort, setSort] = useState(null);
  const [page, setPage] = useState(0);
  const filteredRows = useMemo(() => {
    const query = effectiveSearch.trim().toLocaleLowerCase();
    return query ? data.filter((row) => columns.some((column) => formatValue(column.searchValue ? column.searchValue(row) : row[column.field]).toLocaleLowerCase().includes(query))) : [...data];
  }, [data, columns, effectiveSearch]);
  const table = constructTable({
    features: tableFeaturesConfig,
    data: filteredRows,
    columns: columns.map((column) => ({ id: column.field, ...(column.sortValue ? { accessorFn: column.sortValue } : { accessorKey: column.field }), header: column.label, enableSorting: column.sortable !== false })),
    state: { sorting: sort ? [{ id: sort.field, desc: sort.direction === "desc" }] : [] },
    onSortingChange: (updater) => {
      const current = sort ? [{ id: sort.field, desc: sort.direction === "desc" }] : [];
      const next = typeof updater === "function" ? updater(current) : updater;
      setSort(next[0] ? { field: next[0].id, direction: next[0].desc ? "desc" : "asc" } : null);
      setPage(0);
    }
  });
  const sortedRows = table.getRowModel().rows.map((row) => row.original);
  const pageCount = Math.max(1, Math.ceil(sortedRows.length / pageSize));
  const currentPage = Math.min(page, pageCount - 1);
  const visibleRows = sortedRows.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
  const activateRow = (event, row) => {
    if (!onRowClick || event.target.closest("button, a, input, wa-button, wa-checkbox")) return;
    onRowClick(row);
  };
  return (
    <>
      <style>{styles}</style>
      <section class={`data-grid ${className}`} aria-label={label}>
        {searchable && (
          <div class="data-grid__toolbar">
            <wa-input aria-label={`Поиск в таблице «${label}»`} placeholder="Поиск…" type="search" value={effectiveSearch} with-clear onInput={(event) => { setSearch(event.currentTarget.value); onSearchChange?.(event.currentTarget.value); setPage(0); }}></wa-input>
            <span class="data-grid__summary">Найдено: {filteredRows.length}</span>
          </div>
        )}
        <div class="data-grid__scroll">
          <table class={`data-grid__table wa-hover-rows wa-tabular-nums ${tableClassName}`} role="grid">
            <thead>
              <tr>
                {columns.map((column) => (
                  <th scope="col" class={`data-grid__heading data-grid__heading--${column.align || "start"}`}>
                    {column.sortable === false ? column.label : (
                      <button type="button" class="data-grid__sort" onClick={() => table.getColumn(column.field)?.toggleSorting()} aria-label={`Сортировать по полю «${column.label}»`}>
                        <span>{column.label}</span>
                        <span class={`data-grid__sort-indicator ${sort?.field === column.field ? "data-grid__sort-indicator--active" : ""}`} aria-hidden="true"><wa-icon name={sort?.field === column.field ? (sort.direction === "asc" ? "sort-up" : "sort-down") : "sort"}></wa-icon></span>
                      </button>
                    )}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row, index) => {
                const rowClass = getRowClassName?.(row) || "";
                const expandedContent = renderExpandedRow?.(row);
                return (
                  <Fragment key={row.id ?? index}>
                    <tr class={`data-grid__row ${onRowClick ? "data-grid__row--interactive" : ""} ${rowClass}`} tabindex={onRowClick ? "0" : undefined} onClick={(event) => activateRow(event, row)} onKeyDown={(event) => { if (onRowClick && ["Enter", " "].includes(event.key) && event.currentTarget === event.target) { event.preventDefault(); onRowClick(row); } }}>
                      {columns.map((column) => <td class={`data-grid__cell data-grid__cell--${column.field} data-grid__cell--${column.align || "start"}`}>{column.render ? column.render(row) : formatValue(row[column.field])}</td>)}
                    </tr>
                    {expandedContent && <tr class="data-grid__expanded-row"><td colSpan={columns.length}>{expandedContent}</td></tr>}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
          {!visibleRows.length && <div class="data-grid__empty">{effectiveSearch ? "Ничего не найдено" : "Нет данных для отображения"}</div>}
        </div>
        {pageCount > 1 && (
          <footer class="data-grid__footer">
            <span>Страница {currentPage + 1} из {pageCount}</span>
            <div class="data-grid__pagination">
              <wa-button size="s" appearance="outlined" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Назад</wa-button>
              <wa-button size="s" appearance="outlined" disabled={currentPage === pageCount - 1} onClick={() => setPage(currentPage + 1)}>Вперёд</wa-button>
            </div>
          </footer>
        )}
      </section>
    </>
  );
}

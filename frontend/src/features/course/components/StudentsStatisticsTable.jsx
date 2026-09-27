import DataGrid from "../../../components/DataGrid.jsx";

const dateFormatter = new Intl.DateTimeFormat("ru-RU", { dateStyle: "medium", timeStyle: "short" });

const columns = [
  {
    field: "name",
    label: "Обучающийся",
    searchValue: (row) => `${row.name} ${row.email}`,
    render: (row) => (
      <>
        <strong>{row.name}</strong>
        <small>{row.email}</small>
      </>
    )
  },
  { field: "groups", label: "Группа" },
  {
    field: "progress",
    label: "Прогресс",
    render: (row) => (
      <div class="progress-cell wa-stack wa-gap-2xs">
        <span>{row.completed} из {row.assignmentsCount}</span>
        <wa-progress-bar class="student-progress" value={row.progress} label={`Прогресс ${row.name}: ${row.progress}%`}></wa-progress-bar>
      </div>
    )
  },
  {
    field: "average",
    label: "Средний балл",
    render: (row) => (row.average === null || row.average === undefined ? "—" : `${row.average}%`)
  },
  {
    field: "lastActivityTime",
    label: "Последняя активность",
    render: (row) => (row.lastActivity ? dateFormatter.format(new Date(row.lastActivity)) : "Нет попыток")
  },
  {
    field: "attempts",
    label: "Попытки",
    sortable: false,
    searchValue: (row) => row.attempts.map((attempt) => attempt.label).join(" "),
    render: (row) => row.attempts.length ? (
      <div class="attempt-links wa-stack wa-gap-2xs">
        {row.attempts.map((attempt) => <a href={attempt.href}>{attempt.label}: {attempt.text}</a>)}
      </div>
    ) : "—"
  }
];

export default function StudentsStatisticsTable({ students = [] }) {
  return (
    <DataGrid
      data={students}
      columns={columns}
      label="Обучающиеся и попытки"
      pageSize={Math.max(students.length, 1)}
      tableClassName="statistics-table wa-zebra-rows"
    />
  );
}

const applicantStatuses = ["очевидец", "пострадавший", "родственник", "знакомый", "ребенок", "участник"];

export default function ApplicantHeader({ person, readonly = false, onChange }) {
  const name = [person.lastName, person.firstName, person.middleName].filter(Boolean).join(" ") || "Заявитель не указан";
  if (readonly) return <div class="workspace-applicant-summary workspace-applicant-readonly"><strong>{name}</strong><span class="workspace-call-label">{person.status || "Статус не выбран"}</span></div>;
  return (
    <div class="workspace-applicant-summary">
      <div class="workspace-applicant-name">
        <input class="workspace-applicant-name-input" aria-label="Фамилия заявителя" placeholder="Фамилия" value={person.lastName || ""} onInput={(event) => onChange({ ...person, lastName: event.currentTarget.value })} />
        <input class="workspace-applicant-name-input" aria-label="Имя заявителя" placeholder="Имя заявителя" value={person.firstName || ""} onInput={(event) => onChange({ ...person, firstName: event.currentTarget.value })} />
      </div>
      <select class="workspace-applicant-status" aria-label="Статус заявителя" required value={person.status || ""} onChange={(event) => onChange({ ...person, status: event.currentTarget.value })}>
        <option value="" disabled>Выберите статус</option>
        {applicantStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
      </select>
    </div>
  );
}

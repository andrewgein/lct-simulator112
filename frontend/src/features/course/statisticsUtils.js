export const scorePercent = (review) => {
  if (review.maxScore > 0) return Math.round((review.finalScore ?? review.automaticScore ?? 0) / review.maxScore * 100);
  const criteria = review.criterionResults || [];
  const criteriaScore = criteria.reduce((sum, criterion) => sum + criterion.score, 0);
  const criteriaMaxScore = criteria.reduce((sum, criterion) => sum + criterion.maxScore, 0);
  const score = review.finalScore ?? criteriaScore;
  const maxScore = review.maxScore ?? criteriaMaxScore;
  return maxScore ? Math.round(score / maxScore * 100) : null;
};

export const mean = (values) => values.length ? Math.round(values.reduce((sum, value) => sum + value, 0) / values.length) : null;

export const studentName = (student) => student?.profile
  ? [student.profile.surname, student.profile.name, student.profile.patronymic].filter(Boolean).join(" ")
  : student?.email || "Обучающийся";

export const formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return minutes ? `${minutes} мин ${rest} с` : `${rest} с`;
};

export const pluralAttempts = (count) => {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) return `${count} попытка`;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return `${count} попытки`;
  return `${count} попыток`;
};

export const errorLabel = (criterion) => {
  const feedback = (criterion.feedback || "").trim();
  if (!feedback) return criterion.criterionName;
  if (feedback.startsWith("Норматив превышен")) return "Превышен норматив времени";
  const text = feedback
    .replace(/^Звонок №\s*\d+:\s*/, "")
    .replace(/^Для звонка №\s*\d+\s*/, "")
    .replace(/(звонка|звонок)\s*№\s*\d+/gi, "$1")
    .replace(/\s+/g, " ")
    .replace(/\.$/, "")
    .trim();
  return text.charAt(0).toUpperCase() + text.slice(1);
};

/** Builds per-student attempt stats for one course's assignments. `groups` (array of group titles) is left for the caller to attach. */
export function buildStudentStats({ student, reviews, assignmentIds, assignmentTitles }) {
  const attempts = (reviews || [])
    .filter((review) => assignmentIds.has(String(review.assignmentId)))
    .map((review) => ({ ...review, percent: scorePercent(review), assignmentTitle: assignmentTitles.get(String(review.assignmentId)) || "Задание" }))
    .sort((left, right) => new Date(right.createdAt || 0).getTime() - new Date(left.createdAt || 0).getTime());
  const done = attempts.filter((attempt) => attempt.status === "DONE");
  const latestByAssignment = new Map();
  done.forEach((attempt) => {
    const key = String(attempt.assignmentId);
    if (!latestByAssignment.has(key)) latestByAssignment.set(key, attempt);
  });
  const latest = [...latestByAssignment.values()];
  return {
    student,
    name: studentName(student),
    attempts,
    done,
    latestByAssignment,
    completed: latest.length,
    average: mean(latest.map((attempt) => attempt.percent).filter((value) => value !== null)),
    lastActivity: attempts[0]?.createdAt || null
  };
}

export function buildSummary(students, assignmentsCount) {
  const doneAttempts = students.flatMap((student) => student.done);
  const completedTotal = students.reduce((sum, student) => sum + student.completed, 0);
  const possibleTotal = students.length * assignmentsCount;
  const completedShare = possibleTotal ? Math.round(completedTotal / possibleTotal * 100) : 0;
  const averageTotal = mean(students.map((student) => student.average).filter((value) => value !== null));
  const averageDuration = mean(doneAttempts.map((attempt) => Number(attempt.durationSeconds)).filter((value) => value > 0));
  const overtimeShare = doneAttempts.length ? Math.round(doneAttempts.filter((attempt) => Number(attempt.overtimeSeconds) > 0).length / doneAttempts.length * 100) : null;
  return { doneAttempts, completedTotal, possibleTotal, completedShare, averageTotal, averageDuration, overtimeShare };
}

export function buildTrend(doneAttempts, chart = { width: 640, height: 220, left: 40, right: 24, top: 20, bottom: 28 }) {
  const dayFormatter = new Intl.DateTimeFormat("ru-RU", { day: "numeric", month: "short" });
  const trendByDay = new Map();
  doneAttempts.filter((attempt) => attempt.percent !== null && attempt.createdAt).forEach((attempt) => {
    const date = new Date(attempt.createdAt);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    if (!trendByDay.has(key)) trendByDay.set(key, { date: new Date(date.getFullYear(), date.getMonth(), date.getDate()), values: [] });
    trendByDay.get(key).values.push(attempt.percent);
  });
  const yOf = (value) => chart.top + (100 - value) * (chart.height - chart.top - chart.bottom) / 100;
  const trendDays = [...trendByDay.entries()].sort(([left], [right]) => left.localeCompare(right)).slice(-30).map(([, day]) => day);
  const trendStep = trendDays.length > 1 ? (chart.width - chart.left - chart.right) / (trendDays.length - 1) : 0;
  const points = trendDays.map((day, index) => {
    const value = mean(day.values);
    return {
      value,
      count: day.values.length,
      label: dayFormatter.format(day.date),
      x: trendDays.length > 1 ? chart.left + index * trendStep : (chart.left + chart.width - chart.right) / 2,
      y: yOf(value)
    };
  });
  const hitWidth = trendDays.length > 1 ? trendStep : chart.width - chart.left - chart.right;
  return { chart, points, hitWidth };
}

export function buildTopErrors(doneAttempts, limit = 7) {
  const errorCounts = new Map();
  doneAttempts.forEach((attempt) => {
    const labels = new Set((attempt.criterionResults || [])
      .filter((criterion) => criterion.score < criterion.maxScore || (criterion.feedback || "").startsWith("Норматив превышен"))
      .map(errorLabel));
    labels.forEach((label) => errorCounts.set(label, (errorCounts.get(label) || 0) + 1));
  });
  return [...errorCounts.entries()]
    .map(([label, count]) => ({ label, count, share: Math.round(count / doneAttempts.length * 100) }))
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, "ru"))
    .slice(0, limit);
}

export function buildAssignmentAverages(assignments, students) {
  return assignments.map((assignment) => {
    const values = students.map((student) => student.latestByAssignment.get(String(assignment.id))?.percent).filter((value) => value !== null && value !== undefined);
    return { average: mean(values), completed: values.length };
  });
}

export function criterionLoss(attempts) {
  const totals = new Map();
  attempts.forEach((attempt) => (attempt.criterionResults || []).forEach((criterion) => {
    const current = totals.get(criterion.criterionName) || { score: 0, maxScore: 0 };
    current.score += criterion.score;
    current.maxScore += criterion.maxScore;
    totals.set(criterion.criterionName, current);
  }));
  return new Map([...totals.entries()]
    .filter(([, value]) => value.maxScore > 0)
    .map(([name, value]) => [name, Math.round((value.maxScore - value.score) / value.maxScore * 100)]));
}

export function buildCriteriaMap(students) {
  const latestAttempts = students.flatMap((student) => [...student.latestByAssignment.values()]);
  const overallLoss = criterionLoss(latestAttempts);
  const criteriaColumns = [...overallLoss.entries()].sort((left, right) => right[1] - left[1]).map(([name, loss]) => ({ name, loss }));
  const errorMap = students
    .filter((student) => student.latestByAssignment.size)
    .map((student) => ({ name: student.name, loss: criterionLoss([...student.latestByAssignment.values()]) }));
  return { criteriaColumns, errorMap };
}

export const scoreCellStyle = (value) => `--cell-weight: ${Math.round(10 + value * 0.8)}%`;
export const lossCellStyle = (value) => `--cell-weight: ${value === 0 ? 0 : Math.round(12 + value * 0.78)}%`;

/** Shapes one entry of the exported report's `courses` array. */
export function buildReportEntry({ title, assignments, students, completedTotal, possibleTotal, averageTotal }) {
  return {
    title,
    assignmentsCount: assignments.length,
    completed: completedTotal,
    possible: possibleTotal,
    average: averageTotal,
    students: students.map((student) => ({
      name: student.name,
      email: student.student?.email || "",
      groups: student.groups || [],
      completed: student.completed,
      assignmentsCount: assignments.length,
      average: student.average,
      lastActivity: student.lastActivity,
      attempts: student.attempts.map((attempt) => ({
        assignmentTitle: attempt.assignmentTitle,
        status: attempt.status,
        percent: attempt.percent,
        grade: attempt.grade,
        passed: attempt.passed,
        createdAt: attempt.createdAt,
        criteria: (attempt.criterionResults || []).map((criterion) => ({
          incidentOrder: criterion.incidentOrder,
          criterionName: criterion.criterionName,
          score: criterion.score,
          maxScore: criterion.maxScore,
          feedback: criterion.feedback
        }))
      }))
    }))
  };
}

export function buildStudentsTableRows(students, assignments) {
  return students.map((student, index) => ({
    id: student.student?.id ?? index,
    name: student.name,
    email: student.student?.email || "",
    groups: (student.groups || []).join(", "),
    completed: student.completed,
    assignmentsCount: assignments.length,
    progress: assignments.length ? Math.round(student.completed / assignments.length * 100) : 0,
    average: student.average,
    lastActivity: student.lastActivity,
    lastActivityTime: student.lastActivity ? new Date(student.lastActivity).getTime() : 0,
    attempts: student.attempts.map((attempt) => ({
      href: `/review/${encodeURIComponent(attempt.contextId)}`,
      label: attempt.assignmentTitle,
      text: attempt.status === "DONE" && attempt.percent !== null ? `${attempt.percent}%` : "формируется"
    }))
  }));
}

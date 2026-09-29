export default function ElapsedTime({ since, now }) {
  if (!since) return <span>—</span>;
  const startedAt = new Date(since).getTime();
  if (!Number.isFinite(startedAt)) return <span>—</span>;
  const seconds = Math.max(0, Math.floor((now.getTime() - startedAt) / 1000));
  const minutes = Math.floor(seconds / 60);
  const time = `${String(minutes % 60).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
  return <span>{minutes >= 60 ? `${String(Math.floor(minutes / 60)).padStart(2, "0")}:` : ""}{time}</span>;
}

export default function RemainingTime({ deadline, now }) {
  if (!deadline) return <span>Без таймера</span>;
  const seconds = Math.max(0, Math.ceil((new Date(deadline).getTime() - now.getTime()) / 1000));
  return <span class={seconds < 30 ? "dds-time--urgent" : ""}>{String(Math.floor(seconds / 60)).padStart(2, "0")}:{String(seconds % 60).padStart(2, "0")}</span>;
}

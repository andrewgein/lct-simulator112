import { INCIDENT_STATUSES } from "./ddsLevelHelpers.js";

export default function DdsProgressDetails({ progress }) {
  return (
    <div class="saved-card-panel dds-progress wa-stack wa-gap-s">
      <span class="saved-label">
        Ход реагирования
      </span>
      <strong>
        {progress?.status === "ACTIVE" ? "Сведения о бригаде уточняйте по телефону" : INCIDENT_STATUSES[progress?.status] || "Ожидает обработки"}
      </strong>
    </div>
  );
}

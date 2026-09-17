import { useRef } from "preact/hooks";
import EditorDialog from "./EditorDialog.jsx";
import PersonFields from "./PersonFields.jsx";
import { splitLines } from "./editorHelpers";

export default function DialupEditor({ dialup, number, index, count, incidentAddress, open, error, onChange, onOpen, onClose, onRemove, onMove }) {
  const snapshot = useRef(null);
  const setField = (field) => (event) => onChange({ ...dialup, [field]: event.currentTarget.value });
  const openEditor = () => {
    snapshot.current = structuredClone(dialup);
    onOpen();
  };
  const cancelEditor = () => {
    if (snapshot.current) onChange(snapshot.current);
    snapshot.current = null;
    onClose();
  };
  const saveEditor = () => {
    snapshot.current = null;
    onClose();
  };
  const facts = splitLines(dialup.knownFacts).length;
  const meta = [dialup.gender === "MAN" ? "Мужчина" : dialup.gender === "WOMEN" ? "Женщина" : null, dialup.applicant.age ? `${dialup.applicant.age} лет` : null, `${facts} известных фактов`].filter(Boolean).join(" · ");
  return (
    <div class="dialup wa-stack wa-gap-m">
      <wa-card class="dialup-summary">
        <wa-button class="move-up card-arrow card-arrow-left" type="button" size="small" appearance="plain" aria-label="Переместить звонок влево" disabled={index === 0} onClick={() => onMove(-1)}>
          <wa-icon name="chevron-left" label="Переместить влево">
          </wa-icon>
        </wa-button>
        <div class="dialup-content wa-stack wa-gap-s">
          <div class="wa-cluster wa-justify-content-space-between wa-align-items-center">
            <strong class="dialup-title">Звонок {number}</strong>
            <div class="wa-cluster wa-gap-xs">
              <wa-button class="edit-dialup" type="button" size="small" appearance="outlined" aria-label="Изменить звонок" onClick={openEditor}>
                <wa-icon name="pencil" label="Изменить">
                </wa-icon>
              </wa-button>
              <wa-button class="remove-dialup" type="button" size="small" appearance="outlined" variant="danger" aria-label="Удалить звонок" onClick={onRemove}>
                <wa-icon name="trash" label="Удалить">
                </wa-icon>
              </wa-button>
            </div>
          </div>
          <span class="dialup-meta">{meta}</span>
        </div>
        <wa-button class="move-down card-arrow card-arrow-right" type="button" size="small" appearance="plain" aria-label="Переместить звонок вправо" disabled={index === count - 1} onClick={() => onMove(1)}>
          <wa-icon name="chevron-right" label="Переместить вправо">
          </wa-icon>
        </wa-button>
      </wa-card>
      <EditorDialog className="dialup-dialog" label="Редактирование звонка" open={open} onCancel={cancelEditor} onSave={saveEditor}>
        <div class="wa-stack wa-gap-m">
          <p class="dialup-error" hidden={!error}>{error}</p>
          <div class="wa-cluster wa-align-items-stretch wa-gap-l">
            <div class="dialup-applicant dialog-section wa-stack wa-gap-s">
              <PersonFields title="Заявитель" person={dialup.applicant} gender={dialup.gender} incidentAddress={incidentAddress} onChange={(applicant) => onChange({ ...dialup, applicant })} />
            </div>
            <wa-divider class="dialog-divider-desktop" orientation="vertical">
            </wa-divider>
            <wa-divider class="dialog-divider-mobile">
            </wa-divider>
            <div class="dialog-section wa-stack wa-gap-m">
              <h3 class="wa-heading-l">Данные звонка для контекста ИИ</h3>
              <div class="wa-grid dialup-basics">
                <wa-select value={dialup.gender} label="Пол звонящего" onChange={setField("gender")}>
                  <wa-option value="MAN">Мужчина</wa-option>
                  <wa-option value="WOMEN">Женщина</wa-option>
                </wa-select>
                <wa-select value={dialup.emotionalState} label="Эмоциональное состояние" onChange={setField("emotionalState")}>
                  <wa-option value="CALM">Спокойное</wa-option>
                  <wa-option value="WORRIED">Встревоженное</wa-option>
                  <wa-option value="PANICKED">Паническое</wa-option>
                  <wa-option value="AGGRESSIVE">Агрессивное</wa-option>
                  <wa-option value="CONFUSED">Растерянное</wa-option>
                </wa-select>
              </div>
              <wa-textarea value={dialup.knownFacts} label="Известные факты (один на строку)" rows="5" required onInput={setField("knownFacts")}>
              </wa-textarea>
              <wa-textarea value={dialup.hiddenFacts} label="Скрытые факты (один на строку)" rows="4" onInput={setField("hiddenFacts")}>
              </wa-textarea>
              <wa-textarea value={dialup.aiContext} label="Контекст для ИИ" rows="5" onInput={setField("aiContext")}>
              </wa-textarea>
            </div>
          </div>
        </div>
      </EditorDialog>
    </div>
  );
}

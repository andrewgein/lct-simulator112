import { useRef } from "preact/hooks";
import IncidentTypeSelect from "../../../classifier/components/IncidentTypeSelect.jsx";
import AdditionalFields from "./AdditionalFields.jsx";
import DialupEditor from "./DialupEditor.jsx";
import EditorDialog from "./EditorDialog.jsx";
import VictimFields from "../VictimFields.jsx";
import { findIncident, moveItem, normalizeDialup } from "./editorHelpers";

export default function StageEditor({ stage, index, count, firstDialupNumber, classifier, routingFacts, incidentAddress, openStage, openDialup, dialupError, onChange, onOpenStage, onCloseStage, onOpenDialup, onCloseDialup, onRemove, onMove }) {
  const incidentType = findIncident(classifier, stage.typeId);
  const incidentTypes = stage.classifierCodes.filter(Boolean).map((code) => findIncident(classifier, code)?.finalName || code);
  const activeFactCodes = [...new Set(stage.classifierCodes.flatMap((code) => findIncident(classifier, code)?.routingFactCodes || []))];
  const expectedFacts = activeFactCodes.map((code) => routingFacts.find((fact) => fact.code === code)).filter(Boolean);
  const snapshot = useRef(null);
  const changeStageField = (field) => (event) => onChange({ ...stage, [field]: event.currentTarget.value });
  const openEditor = () => {
    snapshot.current = structuredClone(stage);
    onOpenStage();
  };
  const cancelEditor = () => {
    if (snapshot.current) onChange(snapshot.current);
    snapshot.current = null;
    onCloseStage();
  };
  const saveEditor = () => {
    snapshot.current = null;
    onCloseStage();
  };
  const changeDialup = (dialupIndex, dialup) => onChange({ ...stage, dialups: stage.dialups.map((item, index) => index === dialupIndex ? dialup : item) });
  const addDialup = () => onChange({ ...stage, dialups: [...stage.dialups, normalizeDialup()] });
  const changeClassifierCodes = (classifierCodes) => {
    const allowedCodes = new Set(classifierCodes.flatMap((code) => findIncident(classifier, code)?.routingFactCodes || []));
    onChange({ ...stage, classifierCodes, typeId: classifierCodes[0], expectedRoutingFacts: Object.fromEntries(Object.entries(stage.expectedRoutingFacts).filter(([code]) => allowedCodes.has(code))) });
  };
  return (
    <section class="stage wa-stack wa-gap-m">
      <wa-button class="move-up stage-arrow stage-arrow-up" type="button" size="small" appearance="plain" aria-label="Переместить этап выше" disabled={index === 0} onClick={() => onMove(-1)}>
        <wa-icon name="chevron-up" label="Переместить выше">
        </wa-icon>
      </wa-button>
      <div class="stage-content wa-stack wa-gap-m">
        <div class="stage-top wa-cluster wa-justify-content-space-between wa-align-items-center">
          <strong class="stage-number wa-heading-l">Этап {index + 1}</strong>
          <wa-input value={stage.title} aria-label="Название этапа" placeholder="Название этапа" class="stage-title" onInput={changeStageField("title")}>
          </wa-input>
          <wa-button class="edit-stage" type="button" size="small" appearance="outlined" aria-label="Изменить состояние этапа" onClick={openEditor}>
            <wa-icon name="pencil" label="Изменить состояние">
            </wa-icon>
          </wa-button>
          <wa-button class="remove-stage" type="button" size="small" appearance="outlined" variant="danger" aria-label="Удалить этап" onClick={onRemove}>
            <wa-icon name="trash" label="Удалить">
            </wa-icon>
          </wa-button>
        </div>
        <div class="stage-types wa-cluster wa-gap-xs">
          <span class="stage-types-label">Тип происшествия:</span>
          <span>{incidentTypes.length ? incidentTypes.join(", ") : "не выбран"}</span>
        </div>
        <div class="dialups wa-cluster wa-gap-m">
          {stage.dialups.map((dialup, dialupIndex) => (
            <DialupEditor key={dialup.key} dialup={dialup} number={firstDialupNumber + dialupIndex} index={dialupIndex} count={stage.dialups.length} incidentAddress={incidentAddress} open={openDialup === dialup.key} error={dialupError.key === dialup.key ? dialupError.message : ""} onChange={(value) => changeDialup(dialupIndex, value)} onOpen={() => onOpenDialup(dialup.key)} onClose={onCloseDialup} onRemove={() => onChange({ ...stage, dialups: stage.dialups.filter((item) => item.key !== dialup.key) })} onMove={(direction) => onChange({ ...stage, dialups: moveItem(stage.dialups, dialupIndex, direction) })} />
          ))}
          <wa-card class="add-dialup-card">
            <wa-button class="add-dialup" type="button" appearance="plain" variant="brand" onClick={addDialup}>+ Добавить звонок</wa-button>
          </wa-card>
        </div>
      </div>
      <wa-button class="move-down stage-arrow stage-arrow-down" type="button" size="small" appearance="plain" aria-label="Переместить этап ниже" disabled={index === count - 1} onClick={() => onMove(1)}>
        <wa-icon name="chevron-down" label="Переместить ниже">
        </wa-icon>
      </wa-button>
      <EditorDialog className="stage-dialog" label="Состояние этапа" open={openStage} onCancel={cancelEditor} onSave={saveEditor}>
        <div class="wa-stack wa-gap-m">
          <div class="wa-cluster wa-align-items-stretch wa-gap-l">
          <div class="stage-victim dialog-section wa-stack wa-gap-s">
            <h3 class="wa-heading-l">Пострадавшие</h3>
            <VictimFields victimCount={stage.victimCount} required onChange={(victimCount) => onChange({ ...stage, victimCount })} />
          </div>
          <wa-divider class="dialog-divider-desktop" orientation="vertical">
          </wa-divider>
          <wa-divider class="dialog-divider-mobile">
          </wa-divider>
          <div class="dialog-section wa-stack wa-gap-m">
            <h3 class="wa-heading-l">Информация о происшествии</h3>
            {stage.classifierCodes.map((code, codeIndex) => (
              <div class="wa-cluster" key={codeIndex}>
                <IncidentTypeSelect classifierState={{ classifier, loading: false, error: "" }} id={`stage-${stage.key}-incident-type-${codeIndex}`} name={null} value={code} required onChange={(value) => changeClassifierCodes(stage.classifierCodes.map((item, index) => index === codeIndex ? value : item))} />
                {stage.classifierCodes.length > 1 && (
                  <wa-button type="button" appearance="plain" variant="danger" onClick={() => changeClassifierCodes(stage.classifierCodes.filter((_, index) => index !== codeIndex))}>Удалить тип</wa-button>
                )}
              </div>
            ))}
            <wa-button type="button" appearance="plain" onClick={() => onChange({ ...stage, classifierCodes: [...stage.classifierCodes, ""] })}>Добавить тип происшествия</wa-button>
            {!!expectedFacts.length && <div class="wa-stack wa-gap-s">
              <h3 class="wa-heading-m">Ожидаемые ответы по происшествию</h3>
              {expectedFacts.map((fact) => <wa-select key={fact.code} label={fact.label} value={stage.expectedRoutingFacts[fact.code] || ""} onChange={(event) => { const expectedRoutingFacts = { ...stage.expectedRoutingFacts }; const value = event.currentTarget.value; if (value) expectedRoutingFacts[fact.code] = value; else delete expectedRoutingFacts[fact.code]; onChange({ ...stage, expectedRoutingFacts }); }}>
                <wa-option value="">Не задано</wa-option>
                {fact.options.map((option) => <wa-option key={option.value} value={option.value}>{option.label}</wa-option>)}
              </wa-select>)}
            </div>}
            <wa-textarea value={stage.description} label="Описание ситуации" rows="5" onInput={changeStageField("description")}>
            </wa-textarea>
            <div class="wa-stack wa-gap-m">
              <h3 class="wa-heading-m">Состояние происшествия</h3>
              <AdditionalFields fields={incidentType?.fields} values={stage.additionalInfo} onChange={(id, value) => onChange({ ...stage, additionalInfo: { ...stage.additionalInfo, [id]: value } })} />
            </div>
          </div>
        </div>
        </div>
      </EditorDialog>
    </section>
  );
}

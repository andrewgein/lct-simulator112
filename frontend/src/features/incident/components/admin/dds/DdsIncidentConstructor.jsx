import { useEffect, useRef, useState } from "preact/hooks";
import IncidentTypeSelect from "../../../../classifier/components/IncidentTypeSelect.jsx";
import { classifierInfo, loadClassifier } from "../../../storage/classifierStorage.js";
import AdditionalFields from "../AdditionalFields.jsx";
import PersonFields from "../PersonFields.jsx";
import VictimFields from "../../VictimFields.jsx";
import { emptyPerson, findIncident, personValue } from "../editorHelpers.js";
import DdsStageGraph, { ddsTreeValue, validateDdsTree } from "./DdsStageGraph.jsx";

const SERVICES = [
  { value: "FIRE", label: "Пожарная охрана" },
  { value: "POLICE", label: "Полиция" },
  { value: "AMBULANCE", label: "Скорая медицинская помощь" },
  { value: "GAS", label: "Аварийная газовая служба" },
  { value: "ANTI_TERROR", label: "Антитеррор" }
];

const normalizePrepared = (value = {}) => ({
  classifierCodes: value.classifierCodes?.length ? value.classifierCodes : [""],
  applicant: emptyPerson(value.applicant),
  victimCount: value.victimCount ?? 0,
  additionalInfo: value.additionalInfo || {}
});

export default function DdsIncidentConstructor({ incident = {} }) {
  const [classifierState, setClassifierState] = useState(classifierInfo.state);
  const [prepared, setPrepared] = useState(() => normalizePrepared(incident.preparedCardTemplate));
  const [assignment, setAssignment] = useState(() => ({ emergencyService: incident.initialAssignment?.emergencyService || "", classifierCode: incident.initialAssignment?.classifierCode || "", instructions: incident.initialAssignment?.instructions || "" }));
  const [tree, setTree] = useState(null);
  const [incidentAddress, setIncidentAddress] = useState("");
  const stateRef = useRef({ prepared, assignment, tree, classifier: classifierState.classifier });
  stateRef.current = { prepared, assignment, tree, classifier: classifierState.classifier };

  useEffect(() => {
    const unsubscribe = classifierInfo.subscribe((state) => setClassifierState({ ...state, classifier: [...state.classifier] }));
    loadClassifier().catch((error) => console.error("Failed to load incident classifier:", error));
    const addressChange = (event) => setIncidentAddress(event.detail || "");
    window.addEventListener("incident-address-change", addressChange);
    return () => { unsubscribe(); window.removeEventListener("incident-address-change", addressChange); };
  }, []);

  useEffect(() => {
    const form = document.querySelector("#incident-form");
    if (!form) return;
    form.getDdsStructure = () => {
      const current = stateRef.current;
      if (!current.prepared.classifierCodes.length || current.prepared.classifierCodes.some((code) => !code)) throw new Error("Выберите хотя бы один тип происшествия для подготовленной карточки");
      if (!current.assignment.emergencyService) throw new Error("Выберите службу первичного назначения");
      if (!current.assignment.classifierCode) throw new Error("Выберите тип происшествия первичного назначения");
      if (!current.tree) throw new Error("Граф этапов ещё не готов");
      validateDdsTree(current.tree);
      const requiredField = current.prepared.classifierCodes.flatMap((code) => findIncident(current.classifier, code)?.fields || []).find((field) => field.required && !current.prepared.additionalInfo[field.id]);
      if (requiredField) throw new Error(`Заполните обязательное поле подготовленной карточки: ${requiredField.name}`);
      return {
        preparedCardTemplate: {
          classifierCodes: current.prepared.classifierCodes,
          applicant: personValue(current.prepared.applicant),
          victimCount: current.prepared.victimCount,
          additionalInfo: Object.fromEntries(Object.entries(current.prepared.additionalInfo).filter(([, value]) => value !== ""))
        },
        initialAssignment: {
          emergencyService: current.assignment.emergencyService,
          classifierCode: current.assignment.classifierCode,
          instructions: current.assignment.instructions.trim() || null
        },
        ...ddsTreeValue(current.tree)
      };
    };
    return () => { delete form.getDdsStructure; };
  }, []);

  const codes = prepared.classifierCodes.length ? prepared.classifierCodes : [""];
  const entriesCount = classifierState.classifier.reduce((total, category) => total + category.entries.length, 0);
  const fields = [...new Map(codes.flatMap((code) => findIncident(classifierState.classifier, code)?.fields || []).map((field) => [field.id, field])).values()];
  const changeCode = (index, classifierCode) => setPrepared((current) => ({ ...current, classifierCodes: codes.map((code, codeIndex) => codeIndex === index ? classifierCode : code), additionalInfo: {} }));
  return (
    <div class="wa-stack wa-gap-3xl">
      <section class="wa-stack wa-gap-m">
        <div>
          <h2 class="wa-heading-xl">Подготовленная карточка</h2>
          <p class="dds-section-hint">Данные, которые поступают в ДДС до начала обработки происшествия.</p>
        </div>
        <div class="wa-stack wa-gap-s">
          {codes.map((code, index) => <div class="dds-classifier-row wa-flank:end wa-gap-xs" key={index}>
            <IncidentTypeSelect classifierState={classifierState} id={`dds-card-type-${index}`} name={null} value={code} excludedValues={codes.filter((_, codeIndex) => codeIndex !== index)} required onChange={(value) => changeCode(index, value)} />
            {index > 0 && <wa-button type="button" appearance="outlined" variant="danger" aria-label="Удалить тип происшествия" onClick={() => setPrepared((current) => ({ ...current, classifierCodes: codes.filter((_, codeIndex) => codeIndex !== index), additionalInfo: {} }))}><wa-icon name="trash" label="Удалить тип происшествия"></wa-icon></wa-button>}
          </div>)}
          {codes.every(Boolean) && codes.length < entriesCount && <wa-button class="dds-add-type" type="button" appearance="plain" variant="brand" onClick={() => setPrepared((current) => ({ ...current, classifierCodes: [...codes, ""] }))}><wa-icon name="plus" slot="start"></wa-icon>Добавить тип происшествия</wa-button>}
        </div>
        <div class="dds-people wa-grid wa-gap-l">
          <wa-card appearance="filled-outlined"><PersonFields title="Заявитель" person={prepared.applicant} incidentAddress={incidentAddress} onChange={(applicant) => setPrepared((current) => ({ ...current, applicant }))} /></wa-card>
          <wa-card appearance="filled-outlined"><h3 class="wa-heading-l">Пострадавшие</h3><VictimFields victimCount={prepared.victimCount} required onChange={(victimCount) => setPrepared((current) => ({ ...current, victimCount }))} /></wa-card>
        </div>
        <div class="wa-stack wa-gap-s">
          <h3 class="wa-heading-l">Дополнительные сведения</h3>
          <AdditionalFields fields={codes.some(Boolean) ? fields : undefined} values={prepared.additionalInfo} onChange={(id, value) => setPrepared((current) => ({ ...current, additionalInfo: { ...current.additionalInfo, [id]: value } }))} />
        </div>
      </section>
      <section class="wa-stack wa-gap-m">
        <div>
          <h2 class="wa-heading-xl">Первичное назначение</h2>
          <p class="dds-section-hint">Служба и классификация, с которыми карточка поступит диспетчеру.</p>
        </div>
        <div class="wa-grid">
          <wa-select value={assignment.emergencyService} label="Служба ДДС" required onChange={(event) => setAssignment((current) => ({ ...current, emergencyService: event.currentTarget.value }))}>
            {SERVICES.map((service) => <wa-option key={service.value} value={service.value}>{service.label}</wa-option>)}
          </wa-select>
          <IncidentTypeSelect classifierState={classifierState} id="dds-assignment-type" name={null} value={assignment.classifierCode} required onChange={(classifierCode) => setAssignment((current) => ({ ...current, classifierCode }))} />
        </div>
        <wa-textarea value={assignment.instructions} label="Инструкции диспетчеру" rows="4" onInput={(event) => setAssignment((current) => ({ ...current, instructions: event.currentTarget.value }))}></wa-textarea>
      </section>
      <DdsStageGraph initialIncident={incident} incidentAddress={incidentAddress} onChange={setTree} />
    </div>
  );
}

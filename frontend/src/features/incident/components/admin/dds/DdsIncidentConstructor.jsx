import { useEffect, useRef, useState } from "preact/hooks";
import { createPortal } from "preact/compat";
import IncidentTypeSelect from "../../../../classifier/components/IncidentTypeSelect.jsx";
import { classifierInfo, loadClassifier } from "../../../storage/classifierStorage.js";
import AdditionalFields from "../AdditionalFields.jsx";
import EditorDialog from "../EditorDialog.jsx";
import { EditorAddCard } from "../EditorContainers.jsx";
import PersonFields from "../PersonFields.jsx";
import VictimFields from "../../VictimFields.jsx";
import { emptyPerson, findIncident, personValue } from "../editorHelpers.js";
import { automaticServices } from "../../editor/DispatchServicesPanel.jsx";
import DdsStageTimeline, { timelineValue, validateTimeline } from "./DdsStageTimeline.jsx";

const normalizePrepared = (value = {}) => ({
  classifierCodes: value.classifierCodes?.length ? value.classifierCodes : [""],
  applicant: emptyPerson(value.applicant),
  victimCount: value.victimCount ?? 0,
  additionalInfo: value.additionalInfo || {},
  assignedServices: value.assignedServices || []
});

export default function DdsIncidentConstructor({ incident = {} }) {
  const [classifierState, setClassifierState] = useState(classifierInfo.state);
  const [services, setServices] = useState([]);
  const [servicesError, setServicesError] = useState("");
  const [routingError, setRoutingError] = useState("");
  const [servicesDialogOpen, setServicesDialogOpen] = useState(false);
  const [servicesDraft, setServicesDraft] = useState([]);
  const [servicesQuery, setServicesQuery] = useState("");
  const [serviceSelectHost, setServiceSelectHost] = useState(null);
  const [prepared, setPrepared] = useState(() => normalizePrepared({ ...incident.preparedCardTemplate, assignedServices: incident.preparedCardTemplate?.assignedServices ?? (incident.initialAssignment?.emergencyService ? [incident.initialAssignment.emergencyService] : []) }));
  const [assignment, setAssignment] = useState(() => ({ emergencyService: incident.initialAssignment?.emergencyService || "" }));
  const [timeline, setTimeline] = useState(null);
  const [incidentAddress, setIncidentAddress] = useState("");
  const stateRef = useRef({ prepared, assignment, timeline, classifier: classifierState.classifier, services });
  const routingVersion = useRef(0);
  stateRef.current = { prepared, assignment, timeline, classifier: classifierState.classifier, services };

  useEffect(() => {
    setServiceSelectHost(document.getElementById("dds-service-select"));
    const unsubscribe = classifierInfo.subscribe((state) => setClassifierState({ ...state, classifier: [...state.classifier] }));
    loadClassifier().catch((error) => console.error("Failed to load incident classifier:", error));
    fetch("/api/v1/classifier/services").then((response) => {
      if (!response.ok) throw new Error("Не удалось загрузить службы");
      return response.json();
    }).then((items) => { setServices(items); setServicesError(""); }).catch((error) => { console.error("Failed to load dispatch services:", error); setServicesError("Не удалось загрузить службы из классификатора"); });
    const addressChange = (event) => setIncidentAddress(event.detail || "");
    window.addEventListener("incident-address-change", addressChange);
    return () => { unsubscribe(); window.removeEventListener("incident-address-change", addressChange); };
  }, []);

  useEffect(() => {
    const form = document.querySelector("#incident-form");
    if (!form) return;
    form.getDdsDraft = () => {
      const current = stateRef.current;
      if (!current.timeline || !current.services.length) throw new Error("Подождите, пока загрузятся этапы и службы");
      return {
        preparedCardTemplate: {
          classifierCodes: current.prepared.classifierCodes,
          applicant: personValue(current.prepared.applicant),
          victimCount: Number(current.prepared.victimCount),
          assignedServices: current.prepared.assignedServices,
          additionalInfo: current.prepared.additionalInfo
        },
        initialAssignment: { emergencyService: current.assignment.emergencyService },
        ...timelineValue(current.timeline),
        availableServices: current.services
      };
    };
    form.getDdsStructure = () => {
      const current = stateRef.current;
      if (!current.prepared.classifierCodes.length || current.prepared.classifierCodes.some((code) => !code)) throw new Error("Выберите хотя бы один тип происшествия для подготовленной карточки");
      if (!current.assignment.emergencyService) throw new Error("Выберите службу первичного назначения");
      if (current.prepared.assignedServices.some((code) => !current.services.some((service) => service.code === code))) throw new Error("Выберите существующие службы для подготовленной карточки");
      if (!current.timeline) throw new Error("Этапы ещё не готовы");
      validateTimeline(current.timeline, current.assignment.emergencyService);
      const requiredField = current.prepared.classifierCodes.flatMap((code) => findIncident(current.classifier, code)?.fields || []).find((field) => field.required && !current.prepared.additionalInfo[field.id]);
      if (requiredField) throw new Error(`Заполните обязательное поле подготовленной карточки: ${requiredField.name}`);
      return {
        preparedCardTemplate: {
          classifierCodes: current.prepared.classifierCodes,
          applicant: personValue(current.prepared.applicant),
          victimCount: current.prepared.victimCount,
          assignedServices: current.prepared.assignedServices,
          additionalInfo: Object.fromEntries(Object.entries(current.prepared.additionalInfo).filter(([, value]) => value !== ""))
        },
        initialAssignment: {
          emergencyService: current.assignment.emergencyService
        },
        ...timelineValue(current.timeline)
      };
    };
    const applyGenerated = (event) => {
      const patch = event.detail;
      if (patch.preparedCardTemplate) {
        routingVersion.current++;
        setRoutingError("");
        setPrepared((current) => normalizePrepared({ ...current, ...patch.preparedCardTemplate,
          applicant: { ...current.applicant, ...patch.preparedCardTemplate.applicant },
          additionalInfo: { ...current.additionalInfo, ...patch.preparedCardTemplate.additionalInfo } }));
      }
      if (patch.initialAssignment) setAssignment((current) => ({ ...current, ...patch.initialAssignment }));
    };
    form.addEventListener("apply-generated-dds", applyGenerated);
    return () => { form.removeEventListener("apply-generated-dds", applyGenerated); delete form.getDdsDraft; delete form.getDdsStructure; };
  }, []);

  const codes = prepared.classifierCodes.length ? prepared.classifierCodes : [""];
  const entriesCount = classifierState.classifier.reduce((total, category) => total + category.entries.length, 0);
  const fields = [...new Map(codes.flatMap((code) => findIncident(classifierState.classifier, code)?.fields || []).map((field) => [field.id, field])).values()];
  const updateCodes = (nextCodes) => {
    const version = ++routingVersion.current;
    setRoutingError("");
    setPrepared((current) => ({ ...current, classifierCodes: nextCodes, assignedServices: automaticServices(classifierState.classifier, nextCodes), additionalInfo: {} }));
    const selectedCodes = nextCodes.filter(Boolean);
    if (!selectedCodes.length) return;
    Promise.all(selectedCodes.map(async (code) => {
      const response = await fetch(`/api/v1/classifier/${encodeURIComponent(code)}/routing`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ facts: {} }) });
      if (!response.ok) throw new Error("Не удалось рассчитать подключаемые службы");
      return response.json();
    })).then((results) => {
      if (routingVersion.current !== version) return;
      const routedCodes = results.flatMap((result) => result.decisions.map((decision) => decision.service.code));
      setPrepared((current) => ({ ...current, assignedServices: [...new Set([...current.assignedServices, ...routedCodes])] }));
    }).catch((error) => {
      if (routingVersion.current !== version) return;
      console.error("Failed to resolve dispatch services:", error);
      setRoutingError(error.message);
    });
  };
  const changeCode = (index, classifierCode) => {
    if (codes[index] !== classifierCode) updateCodes(codes.map((code, codeIndex) => codeIndex === index ? classifierCode : code));
  };
  const editServices = (update) => {
    routingVersion.current++;
    setPrepared((current) => ({ ...current, assignedServices: update(current.assignedServices) }));
  };
  const openServicesDialog = () => {
    routingVersion.current++;
    setServicesDraft([...prepared.assignedServices]);
    setServicesQuery("");
    setServicesDialogOpen(true);
  };
  const serviceNames = new Map(services.map(({ code, name }) => [code, name]));
  const filteredServices = services.filter((service) => `${service.name} ${service.code}`.toLocaleLowerCase("ru").includes(servicesQuery.trim().toLocaleLowerCase("ru")));
  return (
    <div class="wa-stack wa-gap-3xl">
      {serviceSelectHost && createPortal(
        <div class="wa-stack wa-gap-s">
          {servicesError && <wa-callout variant="danger">
            {servicesError}
          </wa-callout>}
          <wa-select value={assignment.emergencyService} label="Служба ДДС" required onChange={(event) => setAssignment({ emergencyService: event.currentTarget.value })}>
            {services.map((service) => <wa-option key={service.code} value={service.code}>{service.name}</wa-option>)}
          </wa-select>
        </div>, serviceSelectHost)}
      <section class="wa-stack wa-gap-m">
        <h3 class="wa-heading-l">Тип происшествия</h3>
        <div class="wa-stack wa-gap-s">
          {codes.map((code, index) => <div class="dds-classifier-row wa-flank:end wa-gap-xs" key={index}>
            <IncidentTypeSelect classifierState={classifierState} id={`dds-card-type-${index}`} name={null} value={code} excludedValues={codes.filter((_, codeIndex) => codeIndex !== index)} required label="Тип" onChange={(value) => changeCode(index, value)} onInput={(value) => changeCode(index, value)} />
            {index > 0 && <wa-button type="button" appearance="outlined" variant="danger" aria-label="Удалить тип происшествия" onClick={() => updateCodes(codes.filter((_, codeIndex) => codeIndex !== index))}><wa-icon name="trash" label="Удалить тип происшествия"></wa-icon></wa-button>}
          </div>)}
          {codes.every(Boolean) && codes.length < entriesCount && <wa-button class="dds-add-type" type="button" appearance="plain" variant="brand" onClick={() => setPrepared((current) => ({ ...current, classifierCodes: [...codes, ""] }))}><wa-icon name="plus" slot="start"></wa-icon>Добавить тип происшествия</wa-button>}
        </div>
        <div class="wa-stack wa-gap-s">
          <h3 class="wa-heading-l">Назначенные службы</h3>
          {servicesError && <wa-callout variant="danger">{servicesError}</wa-callout>}
          {routingError && <wa-callout variant="warning">{routingError}</wa-callout>}
          <div class="dds-assigned-services wa-gap-s" aria-label="Назначенные службы">
            {prepared.assignedServices.map((code) => <wa-card class="dds-assigned-service" key={code}>
              <strong>{serviceNames.get(code) || code}</strong>
              <wa-button class="dds-service-remove" type="button" size="small" appearance="plain" variant="danger" aria-label={`Удалить службу ${serviceNames.get(code) || code}`} onClick={() => editServices((current) => current.filter((value) => value !== code))}><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>
            </wa-card>)}
            <EditorAddCard className="dds-add-service-card">
              <wa-button type="button" appearance="plain" variant="brand" disabled={!services.length} onClick={openServicesDialog}>+ Добавить службу</wa-button>
            </EditorAddCard>
          </div>
          {!services.length && !servicesError && <p class="dds-section-hint">Загрузка служб…</p>}
          <EditorDialog className="dds-services-dialog" label="Назначенные службы" width="min(90vw, 38rem)" open={servicesDialogOpen} onCancel={() => setServicesDialogOpen(false)} onSave={() => { editServices(() => servicesDraft); setServicesDialogOpen(false); }}>
            <div class="wa-stack wa-gap-m">
              <wa-input value={servicesQuery} placeholder="Поиск службы" aria-label="Поиск службы" onInput={(event) => setServicesQuery(event.currentTarget.value)} onKeyDown={(event) => { if (event.key === "Enter") event.preventDefault(); }}><wa-icon slot="start" name="magnifying-glass" aria-hidden="true"></wa-icon></wa-input>
              <div class="dds-services-dialog-list">
                {filteredServices.map((service) => <wa-checkbox key={service.code} checked={servicesDraft.includes(service.code)} onChange={(event) => { const checked = event.currentTarget.checked; setServicesDraft((current) => checked ? [...current, service.code] : current.filter((code) => code !== service.code)); }}>{service.name}</wa-checkbox>)}
                {!filteredServices.length && <p>Службы не найдены</p>}
              </div>
            </div>
          </EditorDialog>
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
      <DdsStageTimeline initialIncident={incident} onChange={setTimeline} services={services.filter((service) => service.code !== assignment.emergencyService)} />
    </div>
  );
}

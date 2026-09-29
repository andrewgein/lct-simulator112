import { useEffect, useRef, useState } from "preact/hooks";
import AdditionalInfoCard from "./AdditionalInfoCard.jsx";
import ApplicantHeader from "./ApplicantHeader.jsx";
import DispatchServicesPanel, { automaticServices } from "./DispatchServicesPanel.jsx";
import { noResponseServiceCodes } from "./serviceRouting.js";
import IncidentTypeSearch from "./IncidentTypeSearch.jsx";
import LinkCardDialog from "./LinkCardDialog.jsx";
import PersonCard from "./PersonCard.jsx";
import PhoneField from "./PhoneField.jsx";
import VictimStatusBar from "./VictimStatusBar.jsx";
import { cardAddress, emptyPerson, findIncident, findLinkSuggestions } from "./editorHelpers";
import { useClassifier } from "../../hooks/useClassifier";
import IncidentWorkspace from "../../../level/components/common/IncidentWorkspace.jsx";
import DdsCallControls from "../../../level/components/player/DdsCallControls.jsx";
import ServiceLoadIndicator, { useServiceLoad } from "../../../level/components/common/ServiceLoadIndicator.jsx";

const styles = `
.incident-workspace { position: fixed; z-index: 1000; inset: 0; display: grid; grid-template-rows: auto minmax(0, 1fr) auto; min-width: 48rem; background: #c8d1d5; color: var(--wa-color-text-normal); }
.workspace-callbar { display: grid; grid-template-columns: minmax(13rem, 0.8fr) repeat(3, minmax(15rem, 1fr)) auto; box-sizing: border-box; width: auto; height: auto; min-height: 6rem; padding: 0; border-block-end: var(--wa-space-s) solid #c8d1d5; background: #f4f6f6; }
.workspace-callbar > div { display: flex; box-sizing: border-box; min-width: 0; padding: var(--wa-space-m) var(--wa-space-l); border-inline-end: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-callbar:has(.dds-call-controls) { grid-template-columns: minmax(22rem, 1fr) repeat(3, minmax(12rem, 1fr)) auto; }
.workspace-callbar--without-connection { grid-template-columns: repeat(3, minmax(12rem, 1fr)) auto; }
.workspace-callbar > .dds-call-controls { display: grid; padding: var(--wa-space-s) var(--wa-space-m); }
.workspace-applicant-summary { display: flex; box-sizing: border-box; min-height: 4.5rem; align-items: end; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-applicant-name { display: flex; flex: 1; min-width: 0; gap: var(--wa-space-xs); }
.workspace-applicant-name-input, .workspace-applicant-status { min-width: 0; padding: var(--wa-space-2xs) 0; border: 0; border-block-end: 2px solid #b1bbc0; outline: 0; background: transparent; color: #35434a; font: inherit; }
.workspace-applicant-name-input { width: 50%; }
.workspace-applicant-name-input::placeholder, .workspace-applicant-status:invalid { color: #7b8b93; }
.workspace-applicant-name-input:focus, .workspace-applicant-status:focus { border-block-end-color: #008dca; }
.workspace-applicant-status { flex: 0 0 9.5rem; cursor: pointer; }
.workspace-applicant-readonly { flex-direction: row; align-items: center; justify-content: flex-start; gap: var(--wa-space-l); }
.workspace-applicant-readonly strong { overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.workspace-applicant-readonly .workspace-call-label { flex: 0 0 auto; color: #687880; font-size: var(--wa-font-size-s); }
.workspace-victim-status { display: flex; box-sizing: border-box; min-height: 4.5rem; align-items: center; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-victim-status-label { color: #35434a; font-size: var(--wa-font-size-l); }
.workspace-victim-button { min-width: 5rem; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: transparent; color: #26343b; font: inherit; font-weight: var(--wa-font-weight-semibold); cursor: pointer; }
.workspace-victim-button--selected { border-color: #008dca; background: #008dca; color: #ffffff; }
.workspace-victim-count { width: 6rem; padding: var(--wa-space-xs); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; }
.workspace-victim-readonly { font-size: var(--wa-font-size-l); font-weight: var(--wa-font-weight-semibold); }
.workspace-connection { align-items: center; gap: var(--wa-space-l); }
.workspace-connection wa-icon { color: #35434a; font-size: var(--wa-font-size-2xl); }
.workspace-connection-copy, .workspace-phone, .workspace-incident-meta { display: flex; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone { position: relative; flex-direction: row; align-items: center; gap: var(--wa-space-s); }
.workspace-phone > wa-icon { flex: 0 0 auto; color: #687880; font-size: var(--wa-font-size-l); }
.workspace-phone-content { display: flex; flex: 1; min-width: 0; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone strong { overflow: hidden; font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; text-overflow: ellipsis; white-space: nowrap; }
.workspace-phone-control { display: flex; min-width: 0; align-items: end; gap: var(--wa-space-xs); }
.workspace-phone-input { flex: 1; min-width: 0; padding: var(--wa-space-2xs) 0; border: 0; border-block-end: 2px solid #9ba8ae; outline: 0; background: transparent; color: #26343b; font: inherit; font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; }
.workspace-phone-input::placeholder { color: #aeb8bd; }
.workspace-phone-input:focus { border-block-end-color: #008dca; }
.workspace-aoh-button { flex: 0 0 auto; min-width: 4.25rem; padding: var(--wa-space-xs) var(--wa-space-s); border: var(--wa-border-width-s) solid #87969d; background: transparent; color: #35434a; font: inherit; font-weight: var(--wa-font-weight-semibold); cursor: pointer; }
.workspace-aoh-button:hover:not(:disabled) { border-color: #008dca; color: #007bad; }
.workspace-aoh-button:focus-visible { outline: 2px solid #008dca; outline-offset: 2px; }
.workspace-aoh-button:disabled { cursor: not-allowed; opacity: .45; }
.workspace-call-label, .workspace-incident-meta span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.workspace-incident-meta strong { overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.workspace-timer { align-items: center; justify-content: center; min-width: 8rem; background: #293238; color: #ffffff; font-size: var(--wa-font-size-2xl); font-weight: var(--wa-font-weight-bold); font-variant-numeric: tabular-nums; }
.saved-view-label { background: #008dca; font-size: var(--wa-font-size-m); text-transform: uppercase; }
.workspace-body { display: grid; grid-template-columns: minmax(24rem, 0.9fr) minmax(30rem, 1.1fr); width: 100%; min-width: 0; gap: 0.5rem; min-height: 0; overflow: hidden; padding: 0 0.5rem; }
.saved-card-body { display: grid; grid-template-columns: minmax(24rem, 0.9fr) minmax(30rem, 1.1fr); gap: var(--wa-space-s); min-height: 0; padding: 0 var(--wa-space-s) var(--wa-space-s); background: #c8d1d5; }
.saved-card-column { min-width: 0; overflow-y: auto; }
.saved-card-column.wa-stack { --wa-content-spacing: var(--wa-space-s); }
.saved-card-column > .workspace-victim-status, .workspace-applicant-readonly { height: 4.5rem; min-height: 4.5rem; border: var(--wa-border-width-s) solid #b8c1c5; }
.saved-card-panel { padding: var(--wa-space-m); background: #f4f6f6; border: var(--wa-border-width-s) solid #b8c1c5; }
.saved-person-heading strong { font-size: var(--wa-font-size-xl); }
.saved-person-heading span, .saved-label { color: var(--wa-color-text-quiet); }
.saved-address { display: flex; box-sizing: border-box; height: 4.5rem; min-height: 4.5rem; align-items: center; gap: var(--wa-space-m); }
.saved-address strong { flex: 1; min-width: 0; overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.saved-address-map { flex: 0 0 auto; }
.saved-address-map::part(button) { border-color: transparent; color: #35434a; font-size: var(--wa-font-size-xl); }
.saved-address-map::part(button):hover { border-color: #87969d; background: #e8ecec; }
.saved-card-spacer { flex: 1; min-height: 12rem; }
.saved-incident-heading { padding: var(--wa-space-m); background: #293238; color: #ffffff; font-size: var(--wa-font-size-l); }
.saved-incident-heading strong { text-decoration: underline dotted; text-underline-offset: var(--wa-space-xs); }
.saved-details-list { margin: 0; padding: 0; list-style: none; }
.saved-details-list span { color: var(--wa-color-text-quiet); }
.workspace-column { min-width: 0; overflow-y: auto; background: #f4f6f6; }
.workspace-column-inner { padding: var(--wa-space-m); }
.workspace-column wa-card { --spacing: var(--wa-space-m); }
.workspace-column wa-card::part(base) { border-color: #b8c1c5; border-radius: 0; box-shadow: none; }
.workspace-column .wa-stack { --wa-content-spacing: var(--wa-space-s); }
.incident-details-panel { border: var(--wa-border-width-s) solid #b8c1c5; background: #f4f6f6; }
.incident-details-header { display: flex; min-height: 3.5rem; box-sizing: border-box; align-items: center; justify-content: space-between; gap: var(--wa-space-m); padding: var(--wa-space-s) var(--wa-space-m); background: #293238; color: #ffffff; font-size: var(--wa-font-size-l); }
.incident-details-header strong { text-decoration: underline; text-underline-offset: var(--wa-space-xs); }
.incident-details-header button { display: grid; width: 2.5rem; height: 2.5rem; place-items: center; border: 0; background: transparent; color: #ffffff; font-size: var(--wa-font-size-xl); cursor: pointer; }
.incident-details-header button:hover { background: rgba(255, 255, 255, .12); }
.incident-details-content { padding: var(--wa-space-m); }
.incident-details-empty { color: var(--wa-color-text-quiet); }
.incident-routing-facts { display: grid; gap: var(--wa-space-s); }
.incident-routing-row { display: grid; grid-template-columns: minmax(10rem, 14rem) minmax(0, 1fr); align-items: start; gap: var(--wa-space-m); }
.incident-routing-row > span { padding-block: var(--wa-space-xs); color: #687880; }
.incident-routing-options { display: flex; flex-wrap: wrap; gap: var(--wa-space-xs); }
.incident-routing-option { min-height: 2.6rem; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; font-weight: var(--wa-font-weight-semibold); cursor: pointer; }
.incident-routing-option:hover, .incident-routing-option:focus-visible { border-color: #008dca; outline: 0; }
.incident-routing-option--selected { border-color: #008dca; background: #008dca; color: #ffffff; }
.incident-routing-status { margin: 0; color: #687880; }
.incident-routing-error { margin: 0; color: var(--wa-color-danger-on-quiet); }
.incident-classifier-details { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--wa-space-s); margin: 0; }
.incident-classifier-details div { min-width: 0; }
.incident-classifier-details dt { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.incident-classifier-details dd { margin: var(--wa-space-3xs) 0 0; overflow-wrap: anywhere; font-weight: var(--wa-font-weight-semibold); }
.incident-details-instructions { border-block-start: var(--wa-border-width-s) solid #b8c1c5; padding-block-start: var(--wa-space-m); }
.workspace-link-dialog { --width: min(92vw, 72rem); }
.workspace-link-table-wrap { max-height: min(60vh, 36rem); overflow: auto; border: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-link-table { width: 100%; border-collapse: collapse; }
.workspace-link-table th, .workspace-link-table td { padding: var(--wa-space-s) var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid #d4dadd; text-align: start; vertical-align: middle; }
.workspace-link-table th { position: sticky; z-index: 1; inset-block-start: 0; background: #e8ecec; color: #45525a; }
.workspace-link-table tbody tr { cursor: pointer; }
.workspace-link-table tbody tr:hover { background: #edf7fb; }
.workspace-link-table tbody tr:has(input:checked) { background: #d9f0fa; }
.workspace-link-table tbody tr.is-suggested { background: #fff6e5; }
.workspace-link-table td:first-child { width: 3rem; text-align: center; }
.workspace-link-table td wa-badge { margin-inline-start: var(--wa-space-xs); }
.workspace-link-empty { padding: var(--wa-space-xl); color: var(--wa-color-text-quiet); text-align: center; }
.workspace-link-hint { display: flex; align-items: center; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: .5rem solid #c8d1d5; background: #fff6e5; color: #6b4e00; }
.workspace-link-hint wa-icon { flex: 0 0 auto; font-size: var(--wa-font-size-l); }
.workspace-link-hint span { flex: 1; min-width: 0; }
.workspace-link-hint wa-button { flex: 0 0 auto; }
.workspace-section-title { margin: 0; padding: var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid #b8c1c5; color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-xl); font-weight: var(--wa-font-weight-normal); }
.workspace-footer { width: 100%; min-width: 0; min-height: 6rem; overflow: hidden; color: #ffffff; }
.workspace-footer--editable { background: #ff5b2d; }
.workspace-footer--readonly { background: #45525a; }
.workspace-actions { flex: 0 0 auto; padding: var(--wa-space-m); }
.workspace-actions wa-button::part(button) { min-width: 8rem; border-color: #ffffff; color: #ffffff; }
.workspace-actions .workspace-save::part(button) { min-width: 13rem; }
.workspace-actions .workspace-link::part(button), .workspace-actions .workspace-close::part(button) { min-width: 4rem; }
.workspace-actions .workspace-link[appearance='filled']::part(button) { background: #ffffff; color: #ff5b2d; }
@media (max-width: 70rem) { .workspace-callbar { grid-template-columns: repeat(3, minmax(13rem, 1fr)) auto; } .workspace-connection { display: none !important; } }
@media (max-width: 48rem) { .incident-workspace { min-width: 0; } .workspace-callbar { grid-template-columns: repeat(3, minmax(12rem, 1fr)) auto; overflow-x: auto; } .workspace-body, .saved-card-body { grid-template-columns: 1fr; overflow-y: auto; } .workspace-column { overflow: visible; } .incident-routing-row { grid-template-columns: 1fr; gap: var(--wa-space-xs); } }
`;

function formatTime(seconds) {
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function routingFactsComplete(incident, additionalInfo) {
  return incident.routingFactCodes.every((code) => additionalInfo[code] !== undefined && additionalInfo[code] !== null && additionalInfo[code] !== "");
}

function classifierDetails(incident, values, routingFacts) {
  const features = [incident.feature1Name, incident.feature2Name, incident.feature3Name]
    .map((value, index) => value ? { name: `Признак ${index + 1}`, value } : null)
    .filter(Boolean);
  if (incident.additionalFeatures) features.push({ name: "Дополнительные признаки", value: incident.additionalFeatures });
  const factsByCode = new Map(routingFacts.map((fact) => [fact.code, fact]));
  const selectedFacts = incident.routingFactCodes.map((code) => factsByCode.get(code)).filter((fact) => fact && values[fact.code]).map((fact) => ({ name: fact.label, value: fact.options.find((option) => option.value === values[fact.code])?.label || values[fact.code] }));
  return [...features, ...selectedFacts];
}

export default function CardEditor({ contextId, cards, call, editor, isDev, dadataApiKey, onChange, onClose, readOnly = false, hideReadonlyExtras = false, classifier, routingFacts = [], dispatchServices, readonlyTitle = "Карточка сохранена", readonlyHint = "режим просмотра", readonlyStatus = "Карточка сохранена", readonlyTimer = "Просмотр", readonlyDetails, readonlyServiceStatus, readonlyServiceHistory, readonlyServiceEditor, readonlyServiceCalls, readonlyCallEnabled, onServiceCall, onAcceptCall, onDropCall }) {
  const loadedClassifierState = useClassifier();
  const classifierState = classifier ? { classifier, routingFacts, loading: false, error: null } : loadedClassifierState;
  const [serviceCatalog, setServiceCatalog] = useState(dispatchServices || []);
  const [seconds, setSeconds] = useState(0);
  const serviceLoad = useServiceLoad();
  const [savedEditMode, setSavedEditMode] = useState(false);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [linkTargetId, setLinkTargetId] = useState("");
  const [routingErrors, setRoutingErrors] = useState({});
  const [routingPending, setRoutingPending] = useState({});
  const [routingDecisions, setRoutingDecisions] = useState({});
  const routingGeneration = useRef(0);
  const editorRef = useRef(editor);
  const routingRequestVersion = useRef({});
  const routedServices = useRef({});
  editorRef.current = editor;
  const incidentTypes = editor.incidentTypes.filter(Boolean);
  const incidents = incidentTypes.map((code) => findIncident(classifierState.classifier, code)).filter(Boolean);
  const routingSignature = JSON.stringify(incidents.map((incident) => [incident.code, incident.routingFactCodes.map((code) => [code, editor.additionalInfo[code]])]));
  const noResponseServices = noResponseServiceCodes(routingDecisions);
  const editingCard = cards.find((card) => card.cardId === editor.editingCardId);
  const linkCards = cards.filter((card) => card.cardId !== editor.editingCardId);
  const selectedCard = linkCards.find((card) => card.cardId === editor.selectedCardId);
  const relatedCard = !!editingCard?.mainCardId;
  const canUnlink = relatedCard && call.phase === "active" && call.activeCallId === editingCard.callId;
  const relationLocked = relatedCard || call.phase === "finished";
  const linkSuggestions = relationLocked || editor.operation === "LINK" ? [] : findLinkSuggestions(editor.applicant, linkCards);
  const routingReady = !Object.values(routingPending).some(Boolean) && !Object.values(routingErrors).some(Boolean);
  const canSave = !editor.cardSaved && routingReady && Number.isInteger(editor.victimCount) && editor.victimCount >= 0 && incidentTypes.length === editor.incidentTypes.length && incidentTypes.length > 0 && (editor.operation !== "LINK" || !!selectedCard);
  const aoh = call.phone || editor.applicant.phone;

  useEffect(() => {
    if (dispatchServices) {
      setServiceCatalog(dispatchServices);
      return;
    }
    fetch("/api/v1/classifier/services").then((response) => response.ok ? response.json() : [])
      .then(setServiceCatalog).catch((error) => console.error("Failed to load dispatch services", error));
  }, [dispatchServices]);

  useEffect(() => {
    setSavedEditMode(false);
    setRoutingErrors({});
    setRoutingPending({});
    setRoutingDecisions({});
    routingRequestVersion.current = {};
    routedServices.current = {};
  }, [editor.editingCardId, editor.open]);

  useEffect(() => {
    if (!editor.open || call.phase !== "active") {
      setSeconds(0);
      return;
    }
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [editor.open, call.phase]);

  const setApplicant = (applicant) => onChange({ ...editor, applicant });
  const addIncidentType = (code) => {
    const nextIncidentTypes = [...incidentTypes, code];
    const nextEditor = { ...editorRef.current, incidentTypes: nextIncidentTypes, services: [...new Set([...(editorRef.current.services || []), ...automaticServices(classifierState.classifier, nextIncidentTypes)])] };
    editorRef.current = nextEditor;
    onChange(nextEditor);
  };
  const removeIncidentType = (removedIncident) => {
    const remainingTypes = incidentTypes.filter((code) => code !== removedIncident.code);
    const remainingFacts = new Set(remainingTypes.map((code) => findIncident(classifierState.classifier, code)).filter(Boolean).flatMap((incident) => incident.routingFactCodes));
    const removedFacts = new Set(removedIncident.routingFactCodes.filter((code) => !remainingFacts.has(code)));
    const previousRouted = new Set(Object.values(routedServices.current).flat());
    const nextRoutedServices = { ...routedServices.current };
    delete nextRoutedServices[removedIncident.code];
    routedServices.current = nextRoutedServices;
    routingRequestVersion.current[removedIncident.code] = (routingRequestVersion.current[removedIncident.code] || 0) + 1;
    setRoutingErrors((current) => ({ ...current, [removedIncident.code]: "" }));
    setRoutingPending((current) => ({ ...current, [removedIncident.code]: false }));
    setRoutingDecisions((current) => Object.fromEntries(Object.entries(current).filter(([code]) => code !== removedIncident.code)));
    const remainingRouted = Object.values(nextRoutedServices).flat();
    const services = [...new Set([...editor.services.filter((code) => !previousRouted.has(code)), ...automaticServices(classifierState.classifier, remainingTypes), ...remainingRouted])];
    onChange({ ...editor, incidentTypes: remainingTypes, additionalInfo: Object.fromEntries(Object.entries(editor.additionalInfo).filter(([key]) => !removedFacts.has(key))), services });
  };
  const resolveIncidentRouting = async (incident, nextEditor, generation) => {
    setRoutingErrors((current) => ({ ...current, [incident.code]: "" }));
    setRoutingPending((current) => ({ ...current, [incident.code]: true }));
    const version = (routingRequestVersion.current[incident.code] || 0) + 1;
    routingRequestVersion.current[incident.code] = version;
    const isCurrent = () => routingGeneration.current === generation && routingRequestVersion.current[incident.code] === version;
    const facts = Object.fromEntries(incident.routingFactCodes.filter((code) => nextEditor.additionalInfo[code] !== undefined).map((code) => [code, nextEditor.additionalInfo[code]]));
    try {
      const response = await fetch(`/api/v1/classifier/${encodeURIComponent(incident.code)}/routing`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ facts }) });
      if (!response.ok) throw new Error("Не удалось рассчитать подключаемые службы");
      const result = await response.json();
      if (!isCurrent()) return;
      setRoutingDecisions((current) => ({ ...current, [incident.code]: result.decisions }));
      if (readOnly || nextEditor.cardSaved) return;
      const previousRouted = new Set(Object.values(routedServices.current).flat());
      routedServices.current = { ...routedServices.current, [incident.code]: result.decisions.map((decision) => decision.service.code) };
      const currentEditor = editorRef.current;
      const services = [...new Set([...currentEditor.services.filter((code) => !previousRouted.has(code)), ...automaticServices(classifierState.classifier, currentEditor.incidentTypes), ...Object.values(routedServices.current).flat()])];
      const routedEditor = { ...currentEditor, services };
      editorRef.current = routedEditor;
      onChange(routedEditor);
    } catch (error) {
      if (isCurrent()) setRoutingErrors((current) => ({ ...current, [incident.code]: error.message }));
    } finally {
      if (isCurrent()) setRoutingPending((current) => ({ ...current, [incident.code]: false }));
    }
  };
  const changeRoutingFact = (factCode, value) => {
    const nextEditor = { ...editorRef.current, additionalInfo: { ...editorRef.current.additionalInfo, [factCode]: value } };
    editorRef.current = nextEditor;
    onChange(nextEditor);
  };
  useEffect(() => {
    const generation = ++routingGeneration.current;
    setRoutingDecisions({});
    setRoutingPending({});
    setRoutingErrors({});
    if (editor.open) {
      incidents.filter((incident) => routingFactsComplete(incident, editor.additionalInfo)).forEach((incident) => resolveIncidentRouting(incident, editor, generation));
    }
    return () => { routingGeneration.current++; };
  }, [editor.open, editor.editingCardId, routingSignature, readOnly]);
  const openLinkDialog = () => {
    if (relationLocked || !linkCards.length) return;
    setLinkTargetId(linkCards.some((card) => card.cardId === editor.selectedCardId) ? editor.selectedCardId : "");
    setLinkDialogOpen(true);
  };
  const openLinkSuggestion = (cardId) => {
    if (relationLocked) return;
    setLinkTargetId(cardId);
    setLinkDialogOpen(true);
  };
  const confirmLink = () => {
    if (!linkTargetId) return;
    onChange({ ...editor, operation: "LINK", selectedCardId: linkTargetId });
    setLinkDialogOpen(false);
  };
  const autofill = () => {
    const firstIncident = classifierState.classifier.flatMap((category) => category.entries)[0];
    if (!firstIncident) return;
    const factsByCode = new Map(classifierState.routingFacts.map((fact) => [fact.code, fact]));
    onChange({
      ...editor,
      applicant: { ...emptyPerson(), phone: "79001234567", contactPhone: "79001234567", onScenePhone: "79001234567", lastName: "Иванов", firstName: "Иван", middleName: "Иванович", address: "г. Москва, ул. Тверская, д. 1", additionalInfo: "Тестовый заявитель" },
      victimCount: 1,
      incidentTypes: [firstIncident.code],
      additionalInfo: Object.fromEntries(firstIncident.routingFactCodes.map((code) => [code, factsByCode.get(code)?.options[0]?.value]).filter(([, value]) => value)),
      services: automaticServices(classifierState.classifier, [firstIncident.code])
    });
  };
  const unlink = async () => {
    if (!canUnlink) return;
    onChange({ ...editor, saving: true });
    try {
      const response = await fetch(`/api/v1/context/${contextId}/cards/${editingCard.cardId}/revisions`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ operation: "UNLINK", cardId: editingCard.cardId, expectedVersion: editingCard.version }) });
      if (!response.ok) throw new Error(await response.text());
      onClose(false);
    } catch (error) {
      console.error("Failed to unlink card", error);
      onChange({ ...editor, saving: false });
    }
  };
  const save = async () => {
    if (!canSave) return;
    const revisingCard = !!editingCard;
    if (!revisingCard && !call.activeCallId) return console.error("Cannot create a card without callId");
    const url = revisingCard ? `/api/v1/context/${contextId}/cards/${editingCard.cardId}/revisions` : `/api/v1/context/${contextId}/calls/${call.activeCallId}/cards`;
    const operation = editor.operation === "LINK" ? "LINK" : revisingCard ? "SAVE" : "CREATE";
    onChange({ ...editor, saving: true });
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ operation, cardId: revisingCard ? editingCard.cardId : null, expectedVersion: revisingCard ? editingCard.version : null, mainCardId: operation === "LINK" ? selectedCard.cardId : null, applicant: editor.applicant, victimCount: editor.victimCount, incidentTypes, additionalInfo: editor.additionalInfo, services: editor.services })
      });
      if (!response.ok) throw new Error(await response.text());
      onClose(call.phase === "finished");
    } catch (error) {
      console.error("Failed to save card", error);
      onChange({ ...editor, saving: false });
    }
  };
  const cancel = () => onChange({ ...editor, open: false, saving: false });
  const openAddressMap = async () => {
    const address = cardAddress(editingCard);
    const mapWindow = window.open("about:blank", `AddressMap_${editingCard.cardId}`, "width=1000,height=700");
    if (!mapWindow) return;
    try {
      const response = await fetch(`/api/v1/geocode?address=${encodeURIComponent(address)}`);
      if (!response.ok) throw new Error(await response.text());
      const { lat, lng } = await response.json();
      mapWindow.location.href = `/map?address=${encodeURIComponent(address)}&lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`;
    } catch (error) {
      console.error("OpenStreetMap address geocoding failed", error);
      mapWindow.location.href = `/map?address=${encodeURIComponent(address)}&geocodeError=1`;
    }
  };

  if (!editor.open) return null;
  if (editingCard && (!savedEditMode || readOnly)) return (
    <IncidentWorkspace label={`Просмотр карточки ${editingCard.cardId}`}>
      <style>{styles}</style>
      <header class={`workspace-callbar ${readOnly && !["incoming", "active"].includes(call.phase) ? "workspace-callbar--without-connection" : ""}`}>
        {readOnly ? (["incoming", "active"].includes(call.phase) && <DdsCallControls call={call} load={serviceLoad} seconds={seconds} onAccept={onAcceptCall} onDrop={onDropCall} />) : <div class="workspace-connection">
          <wa-icon name="phone" aria-hidden="true"></wa-icon>
          <div class="workspace-connection-copy"><strong>{readonlyTitle}</strong><span class="workspace-call-label">{readonlyHint}</span></div>
        </div>}
        <PhoneField label="АОН" value={editor.applicant.phone} readonly />
        <PhoneField label="Предоставленный" value={editor.applicant.contactPhone} readonly />
        <PhoneField label="Телефон на месте" value={editor.applicant.onScenePhone} readonly />
        <div class="workspace-timer saved-view-label">{readonlyTimer}</div>
      </header>
      <div class="saved-card-body">
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о заявителе">
          <ApplicantHeader person={editor.applicant} readonly />
          <div class="saved-card-panel saved-address"><strong title={cardAddress(editingCard)}>{cardAddress(editingCard)}</strong><wa-button class="saved-address-map" type="button" size="m" appearance="plain" variant="neutral" aria-label="Показать адрес на карте" onClick={openAddressMap}><wa-icon name="map-location-dot" aria-hidden="true"></wa-icon></wa-button></div>
          <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Описание со слов заявителя</span><p>{editor.applicant.additionalInfo || "Описание не заполнено"}</p></div>
        </section>
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о происшествии">
          <VictimStatusBar victimCount={editor.victimCount} readonly />
          {incidents.map((item) => { const details = classifierDetails(item, editor.additionalInfo, classifierState.routingFacts); return <div class="wa-stack wa-gap-0" key={item.code}>
            <div class="saved-incident-heading">
              <strong>{item.finalName}</strong>
            </div>
            <div class="saved-card-panel">
              <span class="saved-label">Признаки классификации</span>
              {!!details.length && <ul class="saved-details-list wa-stack wa-gap-xs">{details.map((detail, index) => <li class="wa-cluster wa-gap-s" key={`${index}-${detail.name}`}><span>{detail.name}:</span><strong>{detail.value}</strong></li>)}</ul>}
              {!details.length && <p>Уточняющие признаки не указаны.</p>}
            </div>
            {!hideReadonlyExtras && <div class="saved-card-panel">
              <span class="saved-label">Инструкции</span>
              {item.instructions?.length ? <ul>{item.instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}</ul> : <p>Инструкции не указаны.</p>}
            </div>}
          </div>; })}
          {!incidents.length && <div class="saved-card-panel">Тип происшествия не выбран.</div>}
          {!hideReadonlyExtras && readonlyDetails}
          {!hideReadonlyExtras && <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Статус</span><p><strong>{readonlyStatus}</strong></p></div>}
        </section>
      </div>
      <footer class="workspace-footer workspace-footer--readonly wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap">
        <DispatchServicesPanel classifier={classifierState.classifier} dispatchServices={serviceCatalog} services={editor.services} noResponseServices={noResponseServices} readonly status={readonlyServiceStatus} statusHistory={readonlyServiceHistory} statusEditor={readonlyServiceEditor} calls={readonlyServiceCalls} onCall={onServiceCall} callEnabled={readonlyCallEnabled} onChange={() => {}} />
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          {!readOnly && <wa-button class="workspace-save" size="l" type="button" appearance="outlined" variant="neutral" onClick={() => setSavedEditMode(true)}><wa-icon slot="start" name="pencil"></wa-icon>Редактировать</wa-button>}
          {!readOnly && relatedCard && <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button>}
          <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" onClick={cancel}><wa-icon name="xmark" label="Закрыть карточку"></wa-icon></wa-button>
        </div>
      </footer>
    </IncidentWorkspace>
  );
  return (
    <IncidentWorkspace label="Карточка происшествия">
      <style>{styles}</style>
      <header class="workspace-callbar">
        <div class="workspace-connection">
          <wa-icon name={call.phase === "active" ? "phone-volume" : "phone"} aria-hidden="true"></wa-icon>
          {call.phase === "active" && <ServiceLoadIndicator load={serviceLoad} />}
          <div class="workspace-connection-copy"><strong>{call.phase === "active" ? (serviceLoad ? "На линии" : "Ожидание собеседника") : "Карточка происшествия"}</strong><span class="workspace-call-label">{call.phase === "active" ? "активное соединение" : "редактирование"}</span></div>
        </div>
        <PhoneField label="АОН" value={aoh} readonly />
        <PhoneField label="Предоставленный" value={editor.applicant.contactPhone} aoh={aoh} onChange={(contactPhone) => setApplicant({ ...editor.applicant, contactPhone })} />
        <PhoneField label="Телефон на месте" value={editor.applicant.onScenePhone} aoh={aoh} onChange={(onScenePhone) => setApplicant({ ...editor.applicant, onScenePhone })} />
        <div class="workspace-timer" aria-label={`Время звонка: ${formatTime(seconds)}`}>{formatTime(seconds)}</div>
      </header>
      <div class="workspace-body">
        <section class="workspace-column" aria-label="Заявитель и пострадавшие">
          <ApplicantHeader person={editor.applicant} onChange={setApplicant} />
          <div class="workspace-column-inner wa-stack wa-gap-m">
            <PersonCard key={`${contextId}:${editor.editingCardId || call.activeCallId || "new"}`} kind="applicant" person={editor.applicant} addressRequired dadataApiKey={dadataApiKey} onChange={setApplicant} />
          </div>
        </section>
        <section class="workspace-column" aria-label="Классификация происшествия">
          <VictimStatusBar victimCount={editor.victimCount} onChange={(victimCount) => onChange({ ...editor, victimCount })} />
          <LinkCardDialog open={linkDialogOpen} cards={linkCards} suggestedIds={new Set(linkSuggestions.map((card) => card.cardId))} selectedId={linkTargetId} onSelect={setLinkTargetId} onCancel={() => setLinkDialogOpen(false)} onConfirm={confirmLink} />
          {!!linkSuggestions.length && (
            <div class="workspace-link-hint">
              <wa-icon name="triangle-exclamation" aria-hidden="true"></wa-icon>
              <span>Найдена карточка с тем же адресом, телефоном или заявителем — возможно, это тот же случай.</span>
              <wa-button type="button" size="s" appearance="outlined" variant="warning" onClick={() => openLinkSuggestion(linkSuggestions[0].cardId)}>Связать карточки</wa-button>
            </div>
          )}
          <IncidentTypeSearch classifierState={classifierState} selectedCodes={incidentTypes} onAdd={addIncidentType} />
          <div class="workspace-column-inner wa-stack wa-gap-m">
            {incidents.map((item) => <AdditionalInfoCard key={item.code} incident={item} routingFacts={classifierState.routingFacts} values={editor.additionalInfo} routingError={routingErrors[item.code]} routingPending={routingPending[item.code]} onChange={changeRoutingFact} onRemove={() => removeIncidentType(item)} />)}
          </div>
        </section>
      </div>
      <footer class={`workspace-footer ${editingCard ? "workspace-footer--readonly" : "workspace-footer--editable"} wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap`}>
        <DispatchServicesPanel classifier={classifierState.classifier} dispatchServices={serviceCatalog} services={editor.services} noResponseServices={noResponseServices} readonly={!!editingCard} onChange={(services) => onChange({ ...editor, services })} />
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          {isDev && <wa-button size="l" type="button" appearance="outlined" onClick={autofill}><wa-icon name="wand-magic-sparkles" label="Автозаполнение"></wa-icon></wa-button>}
          <wa-button class="workspace-save" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canSave} loading={editor.saving} onClick={save}>Сохранить</wa-button>
          {relatedCard ? <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button> : <wa-button class="workspace-link" type="button" size="l" appearance={editor.operation === "LINK" ? "filled" : "outlined"} variant="neutral" disabled={relationLocked || !linkCards.length} aria-label="Связать карточку" onClick={openLinkDialog}><wa-icon name="link"></wa-icon></wa-button>}
          {editingCard && <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" aria-label="Закрыть карточку" onClick={cancel}><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>}
        </div>
      </footer>
    </IncidentWorkspace>
  );
}

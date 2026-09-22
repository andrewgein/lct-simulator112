import { useEffect, useRef, useState } from "preact/hooks";
import { classifierInfo, loadClassifier } from "../../storage/classifierStorage";
import StageEditor from "./StageEditor.jsx";
import { findIncident, normalizeStage, personIsIncomplete, personValue, request, splitLines } from "./editorHelpers";

/**
 * @param {{ initialStages?: import("../../contract/Incident").IncidentStage[] }} props
 */
export default function IncidentStagesEditor({ initialStages = [] }) {
  const [stages, setStages] = useState(() => initialStages.map((stage, index) => normalizeStage(stage, stage.id || `stage-${index}`)));
  const [classifier, setClassifier] = useState([]);
  const [incidentAddress, setIncidentAddress] = useState("");
  const [openStage, setOpenStage] = useState("");
  const [openDialup, setOpenDialup] = useState("");
  const [dialupError, setDialupError] = useState({ key: "", message: "" });
  const stagesRef = useRef(stages);
  const initialStageIds = useRef(new Set(initialStages.map((stage) => stage.id).filter(Boolean)));
  const initialDialupIds = useRef(new Set(initialStages.flatMap((stage) => stage.dialups || []).map((dialup) => dialup.id).filter(Boolean)));
  stagesRef.current = stages;

  useEffect(() => {
    const unsubscribe = classifierInfo.subscribe((state) => setClassifier([...state.classifier]));
    loadClassifier().catch((error) => console.error("Failed to load incident classifier:", error));
    return unsubscribe;
  }, []);

  useEffect(() => {
    const updateAddress = (event) => {
      const value = event.detail || "";
      setIncidentAddress(value);
      setStages((items) => items.map((stage) => ({
        ...stage,
        dialups: stage.dialups.map((dialup) => dialup.applicant.useIncidentAddress ? { ...dialup, applicant: { ...dialup.applicant, address: value } } : dialup)
      })));
    };
    const input = document.querySelector("#incident-main-address input");
    if (input) setIncidentAddress(input.value || "");
    window.addEventListener("incident-address-change", updateAddress);
    return () => window.removeEventListener("incident-address-change", updateAddress);
  }, []);

  useEffect(() => {
    const form = document.querySelector("#incident-form");
    if (!form) return;

    form.validateIncidentStructure = () => {
      for (const stage of stagesRef.current) {
        if (!stage.typeId) {
          setOpenStage(stage.key);
          throw new Error("Выберите тип происшествия для этапа");
        }
        const missingField = findIncident(classifierInfo.state.classifier, stage.typeId)?.fields?.find((field) => field.required && !stage.additionalInfo[field.id]);
        if (missingField) {
          setOpenStage(stage.key);
          throw new Error(`Заполните обязательное поле: ${missingField.name}`);
        }
        for (const dialup of stage.dialups) {
          const fail = (message) => {
            setDialupError({ key: dialup.key, message });
            setOpenDialup(dialup.key);
            throw new Error(message);
          };
          if (personIsIncomplete(dialup.applicant)) fail("Для заявителя укажите имя, фамилию и телефон");
          if (!splitLines(dialup.knownFacts).length) fail("Добавьте хотя бы один известный звонящему факт");
        }
      }
    };

    form.saveIncidentStructure = async (incidentId) => {
      form.validateIncidentStructure();
      const currentStages = stagesRef.current.map((stage) => ({ ...stage, dialups: stage.dialups.map((dialup) => ({ ...dialup })) }));
      for (const [position, stage] of currentStages.entries()) {
        const payload = {
          title: stage.title || null,
          position,
          typeId: stage.typeId,
          description: stage.description || null,
          victimCount: stage.victimCount,
          additionalInfo: Object.entries(stage.additionalInfo).filter(([, fieldValue]) => fieldValue !== "").map(([additionalInfoId, fieldValue]) => ({ additionalInfoId, fieldValue }))
        };
        const saved = stage.id
          ? await request(`/api/v1/admin/incident/stages/${stage.id}`, "PATCH", payload)
          : await request(`/api/v1/admin/incident/incidents/${incidentId}/stages`, "POST", payload);
        stage.id = saved.id;
      }

      for (const stage of currentStages) {
        for (const [position, dialup] of stage.dialups.entries()) {
          const payload = {
            position,
            applicant: personValue(dialup.applicant),
            dialupDetails: {
              gender: dialup.gender || null,
              knownFacts: splitLines(dialup.knownFacts),
              hiddenFacts: splitLines(dialup.hiddenFacts),
              aiContext: dialup.aiContext || null,
              emotionalState: dialup.emotionalState || null
            }
          };
          const saved = dialup.id
            ? await request(`/api/v1/admin/incident/dialups/${dialup.id}`, "PATCH", payload)
            : await request(`/api/v1/admin/incident/stages/${stage.id}/dialups`, "POST", payload);
          dialup.id = saved.id;
        }
      }

      const currentDialupIds = new Set(currentStages.flatMap((stage) => stage.dialups).map((dialup) => dialup.id));
      for (const id of Array.from(initialDialupIds.current).reverse()) {
        if (!currentDialupIds.has(id)) await request(`/api/v1/admin/incident/dialups/${id}`, "DELETE");
      }
      const currentStageIds = new Set(currentStages.map((stage) => stage.id));
      for (const id of initialStageIds.current) {
        if (!currentStageIds.has(id)) await request(`/api/v1/admin/incident/stages/${id}`, "DELETE");
      }
      stagesRef.current = currentStages;
      setStages(currentStages);
    };

    return () => {
      delete form.validateIncidentStructure;
      delete form.saveIncidentStructure;
    };
  }, []);

  let dialupNumber = 1;
  return (
    <section class="wa-stack wa-gap-m">
      <h2 class="wa-heading-xl">Этапы и звонки</h2>
      <div id="incident-stages" class="wa-stack wa-gap-l">
        {stages.map((stage, index) => {
          const firstDialupNumber = dialupNumber;
          dialupNumber += stage.dialups.length;
          return (
            <StageEditor key={stage.key} stage={stage} index={index} count={stages.length} firstDialupNumber={firstDialupNumber} classifier={classifier} incidentAddress={incidentAddress} openStage={openStage === stage.key} openDialup={openDialup} dialupError={dialupError} onChange={(value) => setStages((items) => items.map((item) => item.key === stage.key ? value : item))} onOpenStage={() => setOpenStage(stage.key)} onCloseStage={() => setOpenStage("")} onOpenDialup={(key) => { setDialupError({ key: "", message: "" }); setOpenDialup(key); }} onCloseDialup={() => setOpenDialup("")} onRemove={() => setStages((items) => items.filter((item) => item.key !== stage.key))} onMove={(direction) => setStages((items) => moveItem(items, index, direction))} />
          );
        })}
      </div>
      <wa-card class="add-stage-card">
        <wa-button id="add-stage" type="button" appearance="plain" variant="brand" onClick={() => setStages((items) => [...items, normalizeStage()])}>+ Добавить этап</wa-button>
      </wa-card>
    </section>
  );
}

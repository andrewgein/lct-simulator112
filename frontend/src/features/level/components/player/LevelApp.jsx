import { useCallback, useEffect, useRef, useState } from "preact/hooks";
import { initializeDialogSession, requestNextCall, startDialog, stopDialog } from "../../../dialog/api/DialogApi";
import CardEditor from "../../../incident/components/editor/CardEditor.jsx";
import { cardIsComplete, emptyPerson, normalizePerson } from "../../../incident/components/editor/editorHelpers";
import { useClassifier } from "../../../incident/hooks/useClassifier";
import LevelCompletionNotice from "../common/LevelCompletionNotice.jsx";
import LevelSearchInput from "../common/LevelSearchInput.jsx";
import { useLevelClock } from "../../hooks/useLevelClock.js";
import ActiveCards from "./ActiveCards.jsx";
import LevelCommandBar from "./LevelCommandBar.jsx";

const emptyCall = () => ({ phase: "idle", activeCallId: null, phone: "", interrupted: false });
const emptyEditor = (values = {}) => ({ open: false, operation: "CREATE", editingCardId: null, selectedCardId: "", applicant: emptyPerson(), victimCount: 0, incidentTypes: [""], additionalInfo: {}, services: [], cardSaved: false, saving: false, ...values });
const editorFor = (card, phone) => emptyEditor({
  open: true,
  operation: card ? "SAVE" : "CREATE",
  editingCardId: card?.cardId || null,
  selectedCardId: card?.cardId || "",
  applicant: { ...normalizePerson(card?.applicant), phone: card?.applicant?.phone || phone },
  victimCount: card?.victimCount ?? 0,
  incidentTypes: card?.incidentTypes?.length ? card.incidentTypes : [""],
  additionalInfo: card?.additionalInfo || {},
  services: card?.services || []
});

export default function LevelApp({ contextId, courseId, dialogEndpoint, dadataApiKey, isDev = false }) {
  const [cards, setCards] = useState([]);
  const [cardsLoading, setCardsLoading] = useState(true);
  const [cardsError, setCardsError] = useState(false);
  const [call, setCall] = useState(emptyCall);
  const [editor, setEditor] = useState(emptyEditor);
  const [finishing, setFinishing] = useState(false);
  const [cardSearch, setCardSearch] = useState("");
  const now = useLevelClock();
  const classifierState = useClassifier();
  const nextCallTimer = useRef();
  const firstCallRequested = useRef(false);
  const latest = useRef();
  const ringtone = useRef();
  latest.current = { call, editor };

  useEffect(() => {
    const audio = ringtone.current;
    if (!audio) return;
    if (call.phase === "incoming") {
      audio.currentTime = 0;
      audio.play().catch((error) => console.error("Can't play ringtone", error));
    } else {
      audio.pause();
      audio.currentTime = 0;
    }
  }, [call.phase]);

  const cardsReady = !cardsLoading && !cardsError && !classifierState.loading && !classifierState.error && cards.length > 0;
  const allCardsComplete = cardsReady && cards.every((card) => cardIsComplete(card, classifierState.classifier));

  const loadCards = useCallback(async () => {
    setCardsLoading(true);
    setCardsError(false);
    try {
      const response = await fetch(`/api/v1/context/${contextId}/cards`);
      if (!response.ok) throw new Error(await response.text());
      setCards(await response.json());
    } catch (error) {
      console.error("Failed to load cards", error);
      setCardsError(true);
    } finally {
      setCardsLoading(false);
    }
  }, [contextId]);

  const requestNext = useCallback(() => {
    setCall(emptyCall());
    window.clearTimeout(nextCallTimer.current);
    nextCallTimer.current = window.setTimeout(requestNextCall, 2000 + Math.random() * 3000);
  }, []);

  const closeEditor = useCallback(async (callCompleted = false) => {
    setEditor((value) => ({ ...value, open: false, cardSaved: true, saving: false }));
    await loadCards();
    if (callCompleted) requestNext();
  }, [loadCards, requestNext]);

  const openEditor = useCallback((card = null, phone = "") => setEditor(editorFor(card, phone)), []);

  useEffect(() => {
    loadCards();
  }, [loadCards]);

  useEffect(() => {
    const timer = window.setInterval(() => fetch("/api/v1/auth/session").catch(() => {}), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const setPhase = (phase) => () => setCall((value) => ({ ...value, phase }));
    const receiveCall = ({ detail }) => {
      const activeCallId = detail?.callId || null;
      setCall({ phase: activeCallId ? "incoming" : "invalid", activeCallId, phone: detail?.phoneNumber || detail?.phone || "", interrupted: !!detail?.interrupted });
    };
    const sessionIdle = () => {
      if (firstCallRequested.current) return;
      firstCallRequested.current = true;
      requestNextCall();
    };
    const callFinished = ({ detail }) => {
      const activeCallId = latest.current.call.activeCallId || detail?.callId || null;
      setCall((value) => ({ ...value, phase: "finished", activeCallId }));
      if (latest.current.editor.cardSaved) closeEditor(true);
    };
    const listeners = {
      "dialog:idle": sessionIdle,
      "dialog:call_ready": receiveCall,
      "dialog:session_restored": receiveCall,
      "dialog:call_started": setPhase("active"),
      "dialog:call_finished": callFinished,
      "dialog:no_more_calls": setPhase("completed"),
      "dialog:error": setPhase("error")
    };
    Object.entries(listeners).forEach(([name, listener]) => window.addEventListener(name, listener));
    initializeDialogSession(dialogEndpoint, contextId);
    return () => {
      window.clearTimeout(nextCallTimer.current);
      Object.entries(listeners).forEach(([name, listener]) => window.removeEventListener(name, listener));
    };
  }, [closeEditor, contextId, dialogEndpoint]);

  const acceptCall = () => {
    openEditor(null, call.phone);
    startDialog(dialogEndpoint, contextId);
  };

  const restartCall = () => {
    openEditor(null, call.phone);
    startDialog(dialogEndpoint, contextId, { restart: true });
  };

  const finishLevel = async () => {
    if (!cardsReady) return;
    setFinishing(true);
    try {
      const response = await fetch(`/api/v1/context/${contextId}/close`, { method: "POST" });
      if (!response.ok) throw new Error(await response.text());
      window.location.href = `/review/${contextId}`;
    } catch (error) {
      console.error("Failed to finish level", error);
      alert("Не удалось завершить уровень. Попробуйте ещё раз.");
      setFinishing(false);
    }
  };

  return (
    <div class="level-app wa-stack wa-gap-0">
      <audio ref={ringtone} src="/audio/incoming-call.mp3" loop preload="auto" />
      <LevelCommandBar call={call} now={now} onAccept={acceptCall} onRestart={restartCall} onDrop={stopDialog} exitHref={`/courses/${courseId}`}>
        <LevelSearchInput value={cardSearch} hint="Поиск по номеру, типу, заявителю и адресу" iconSlot="end" onInput={(event) => setCardSearch(event.currentTarget.value)} />
      </LevelCommandBar>
      <CardEditor contextId={contextId} cards={cards} call={call} editor={editor} isDev={isDev} dadataApiKey={dadataApiKey} onChange={setEditor} onClose={closeEditor} />
      {call.phase === "completed" && <LevelCompletionNotice complete={allCardsComplete} ready={cardsReady} finishing={finishing} onFinish={finishLevel} />}
      <ActiveCards cards={cards} loading={cardsLoading} error={cardsError} classifierState={classifierState} searchQuery={cardSearch} onOpen={openEditor} />
    </div>
  );
}

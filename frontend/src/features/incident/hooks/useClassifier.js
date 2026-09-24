import { useEffect, useState } from "preact/hooks";
import { classifierInfo, loadClassifier } from "../storage/classifierStorage";

export function useClassifier() {
  const [state, setState] = useState(() => ({ ...classifierInfo.state, classifier: [...classifierInfo.state.classifier], routingFacts: [...classifierInfo.state.routingFacts] }));

  useEffect(() => {
    const unsubscribe = classifierInfo.subscribe((value) => setState({ ...value, classifier: [...value.classifier], routingFacts: [...value.routingFacts] }));
    loadClassifier().catch(() => {});
    return unsubscribe;
  }, []);

  return state;
}

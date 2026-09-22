import { useEffect, useState } from "preact/hooks";
import { classifierInfo, loadClassifier } from "../storage/classifierStorage";

export function useClassifier() {
  const [state, setState] = useState(() => ({ ...classifierInfo.state, classifier: [...classifierInfo.state.classifier] }));

  useEffect(() => {
    const unsubscribe = classifierInfo.subscribe((value) => setState({ ...value, classifier: [...value.classifier] }));
    loadClassifier().catch(() => {});
    return unsubscribe;
  }, []);

  return state;
}

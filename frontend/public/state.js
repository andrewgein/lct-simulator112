export function createReactiveState(initialState) {
	const listeners = new Set();

	const proxy = new Proxy(initialState, {
		set(target, property, value) {
			if (target[property] === value) return true;
			target[property] = value;
			listeners.forEach(listener => listener(target, property));
			return true;
		}
	});

	return {
		state: proxy,
		subscribe: (listener) => {
			listeners.add(listener);
			listener(initialState);
			return () => listeners.delete(listener);
		}
	};
}

function bindElement(store, stateKey, element, event, eventValue) {
	if (!element) return;

	element.addEventListener(event, (e) => {
        store.state[stateKey] = e.target[eventValue];
	});

	return store.subscribe((currentState, changedProp) => {
		if (changedProp === stateKey) {
			element.value = currentState[stateKey];
		}
	});
}

export function bind(store, stateKey, element) {
    if (element.tagName == "WA-INPUT" || element.tagName == "WA-TEXTAREA" || element.tagName == "INPUT") {
        return bindElement(store, stateKey, element, "input", "value");
    } else if(element.tagName == "WA-CHECKBOX") {
        return bindElement(store, stateKey, element, "change", "checked");
    } else if(element.tagName == "WA-SELECT") {
        return bindElement(store, stateKey, element, "change", "value");
    }
}

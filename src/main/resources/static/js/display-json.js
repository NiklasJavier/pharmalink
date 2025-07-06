const clickableRules = [
    {
        type: 'key',
        value: 'status',
        label: 'Status',
        styles: { value: 'value-style-status' },
        action: (currentValue, label) => {
            const options = ['geliefert', 'in Bearbeitung', 'storniert'];
            const selection = prompt(`Neuen ${label} wählen:\n(${options.join(', ')})`, currentValue);
            return options.includes(selection) ? selection : null;
        }
    },
    {
        type: 'key',
        value: 'versiegelt',
        label: 'Versiegelungs-Status',
        styles: { value: 'value-style-boolean' },
        action: (currentValue, label) => {
            const isSealed = JSON.parse(currentValue);
            return confirm(`Soll der ${label} umgeschaltet werden? Aktuell: ${isSealed}`) ? !isSealed : null;
        }
    },
    {
        type: 'prefix',
        value: 'med-',
        label: 'Medizinische ID',
        styles: { value: 'value-style-med-id' },
        action: (currentValue, label) => prompt(`Aktion für ${label} "${currentValue}":`, currentValue)
    },
    {
        type: 'prefix',
        value: 'hersteller-',
        label: 'Hersteller-ID',
        styles: { key: 'key-style-hersteller', value: 'value-style-hersteller' },
        action: (currentValue, label) => prompt(`Aktion für ${label} "${currentValue}":`, currentValue)
    }
];

document.addEventListener('DOMContentLoaded', () => {
    renderAllJsonContainers();
});

function renderAllJsonContainers() {
    const containers = document.querySelectorAll('.json-container-item:not(.json-rendered)');
    let isFirstContainer = true; // NEU: Flag, um den ersten Container zu identifizieren

    containers.forEach(container => {
        const jsonDataScript = container.querySelector('script[type="application/json"]');
        if (jsonDataScript) {
            try {
                const jsonObj = JSON.parse(jsonDataScript.textContent);
                if (typeof JSONFormatter !== 'undefined') {
                    // NEU: Setze initialExpandedLevel basierend auf dem Flag
                    const initialLevel = isFirstContainer ? 2 : 0; // 2 für den ersten, 0 für alle anderen (vollständig eingeklappt)

                    const formatter = new JSONFormatter(jsonObj, initialLevel, { theme: 'viewer-theme' });
                    container.innerHTML = '';
                    container.appendChild(formatter.render());

                    makeValuesInteractive(container);
                    removeQuotesFromValues(container);

                    container.classList.add('json-rendered');
                    isFirstContainer = false; // Setze das Flag auf false, nachdem der erste Container verarbeitet wurde
                } else {
                    container.textContent = 'Fehler: Renderer-Bibliothek fehlt.';
                }
            } catch (e) {
                container.textContent = 'Fehler: Ungültiges JSON-Format.';
                console.error('JSON Parse Error:', e);
            }
        }
    });
}

function makeValuesInteractive(container) {
    const valueElements = container.querySelectorAll('.json-formatter-string, .json-formatter-number, .json-formatter-boolean');
    let idCounter = 0;

    valueElements.forEach(valueEl => {
        const valueText = valueEl.textContent.replace(/"/g, '');
        const parentRow = valueEl.parentElement;
        const keyEl = parentRow.querySelector('.json-formatter-key');
        const keyText = keyEl ? keyEl.textContent.trim() : null;

        for (const rule of clickableRules) {
            let match = (rule.type === 'key' && keyText === rule.value) ||
                (rule.type === 'prefix' && typeof valueText === 'string' && valueText.startsWith(rule.value));

            if (match) {
                if (rule.styles) {
                    if (rule.styles.row && parentRow) parentRow.classList.add(rule.styles.row);
                    if (rule.styles.key && keyEl) keyEl.classList.add('styled-key', rule.styles.key);
                    if (rule.styles.value) valueEl.classList.add('styled-value', rule.styles.value);
                }
                valueEl.classList.add('clickable-value');
                valueEl.id = `interactive-${rule.value}-${idCounter++}`;
                valueEl.addEventListener('click', () => {
                    const currentValue = valueEl.textContent.replace(/"/g, '');
                    const updatedValue = rule.action(currentValue, rule.label);
                    if (updatedValue !== null) {
                        valueEl.textContent = updatedValue;
                    }
                });
                break;
            }
        }
    });
}

/**
 * Entfernt die umschließenden Anführungszeichen von allen String-Werten.
 * @param {HTMLElement} container Der Container, in dem gesucht werden soll.
 */
function removeQuotesFromValues(container) {
    const stringElements = container.querySelectorAll('.json-formatter-string');
    stringElements.forEach(el => {
        if (el.textContent.startsWith('"') && el.textContent.endsWith('"')) {
            el.textContent = el.textContent.slice(1, -1);
        }
    });
}
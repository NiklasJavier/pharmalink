// ===================================================================
//  ZENTRALE KONFIGURATION (MIT STEUERUNG FÜR ZEILE, KEY & VALUE)
//  Hier definieren Sie alle Ausnahmen.
// ===================================================================
const clickableRules = [
    {
        type: 'key',
        value: 'status',
        label: 'Status',
        styles: {
            row: 'row-layout-status',   // Eigene Klasse für die Zeile (Layout)
            key: 'key-style-status',    // Eigene Klasse für den Key
            value: 'value-style-status' // Eigene Klasse für den Value
        },
        action: (currentValue, label) => {
            const options = ['geliefert', 'in Bearbeitung', 'storniert'];
            const selection = prompt(`Neuen ${label} wählen:\n(${options.join(', ')})`, currentValue);
            return options.includes(selection) ? selection : null;
        }
    },
    {
        type: 'prefix',
        value: 'MED-',
        label: 'Medizinische ID',
        styles: {
            // Hier wird nur der Wert hervorgehoben
            value: 'value-style-med-id'
        },
        action: (currentValue, label) => prompt(`Aktion für ${label} "${currentValue}":`, currentValue)
    },
    {
        type: 'prefix',
        value: 'hersteller-',
        label: 'Hersteller-ID',
        styles: {
            // Hier bekommen Key und Value einen Stil
            key: 'key-style-hersteller',
            value: 'value-style-hersteller'
        },
        action: (currentValue, label) => prompt(`Aktion für ${label} "${currentValue}":`, currentValue)
    }
];


// ===================================================================
//  HAUPTLOGIK (muss nicht mehr geändert werden)
// ===================================================================

/**
 * Startet den Prozess, sobald das HTML-Dokument geladen ist.
 */
document.addEventListener('DOMContentLoaded', () => {
    renderAllJsonContainers();
});

/**
 * Findet alle JSON-Container, rendert ihren Inhalt und macht Elemente interaktiv.
 */
function renderAllJsonContainers() {
    const containers = document.querySelectorAll('.json-container-item:not(.json-rendered)');
    containers.forEach(container => {
        const jsonDataScript = container.querySelector('script[type="application/json"]');
        if (jsonDataScript) {
            try {
                const jsonObj = JSON.parse(jsonDataScript.textContent);
                if (typeof JSONFormatter !== 'undefined') {
                    const formatter = new JSONFormatter(jsonObj, 2, { theme: 'light-mode' });
                    container.innerHTML = '';
                    container.appendChild(formatter.render());
                    makeValuesInteractive(container);
                    container.classList.add('json-rendered');
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

/**
 * Wendet das universelle Regelwerk an und steuert das Design von Zeile, Key und Value.
 */
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
                // Prüfe, ob ein styles-Objekt in der Regel existiert
                if (rule.styles) {
                    // Wende die Klassen auf die jeweiligen Elemente an
                    if (rule.styles.row && parentRow) parentRow.classList.add(rule.styles.row);
                    if (rule.styles.key && keyEl) keyEl.classList.add(rule.styles.key);
                    if (rule.styles.value) valueEl.classList.add(rule.styles.value);
                }

                // Fügt die Klick-Funktionalität zum Value-Element hinzu
                valueEl.classList.add('clickable-value'); // Basis-Klasse für Klickbarkeit
                valueEl.id = `interactive-${rule.value}-${idCounter++}`;

                valueEl.addEventListener('click', () => {
                    const currentValue = valueEl.textContent.replace(/"/g, '');
                    const updatedValue = rule.action(currentValue, rule.label);

                    if (updatedValue !== null) {
                        valueEl.textContent = (typeof updatedValue === 'string') ? `"${updatedValue}"` : updatedValue;
                    }
                });

                break;
            }
        }
    });
}
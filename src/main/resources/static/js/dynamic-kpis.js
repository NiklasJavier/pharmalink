/**
 * Rendert dynamische KPI-Karten aus einem JSON-String in ein bestimmtes Wurzelelement.
 *
 * @param {string} jsonString Der JSON-String, der ein Objekt mit Key-Value-Paaren enthält.
 * @param {string} rootElementId Die ID des HTML-Elements, in das die Karten gerendert werden sollen.
 * @param {string} title Ein Titel für Debugging-Zwecke (optional).
 */
function renderDynamicKPIsFromJson(jsonString, rootElementId, title) {
    const rootElement = document.getElementById(rootElementId);
    if (!rootElement) {
        console.error(`KPI-Renderer: Root-Element mit ID '${rootElementId}' nicht gefunden.`);
        return;
    }

    try {
        const data = JSON.parse(jsonString);

        // Erstelle den Container für die KPI-Karten
        const kpiGrid = document.createElement('div');
        kpiGrid.className = 'kpi-grid';

        // Iteriere über die Key-Value-Paare im JSON-Objekt
        for (const key in data) {
            if (Object.hasOwnProperty.call(data, key)) {
                const value = data[key];

                // Erstelle eine einzelne KPI-Karte
                const card = document.createElement('div');
                card.className = 'kpi-card';

                // Der Wert (die große Zahl)
                const valueElement = document.createElement('div');
                valueElement.className = 'kpi-value';
                valueElement.textContent = value;
                card.appendChild(valueElement);

                // Der Schlüssel (die Beschreibung)
                const keyElement = document.createElement('div');
                keyElement.className = 'kpi-key';
                keyElement.textContent = key;
                card.appendChild(keyElement);

                kpiGrid.appendChild(card);
            }
        }

        // Leere das Wurzelelement und füge das neue Grid ein
        rootElement.innerHTML = '';
        rootElement.appendChild(kpiGrid);

    } catch (error) {
        console.error(`Fehler beim Parsen oder Rendern der KPIs für '${title}':`, error);
        rootElement.innerHTML = '<p class="text-danger">Fehler beim Laden der Kennzahlen.</p>';
    }
}
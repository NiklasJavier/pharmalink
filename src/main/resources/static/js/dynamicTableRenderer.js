// src/main/resources/static/js/dynamicTableRenderer.js

/**
 * Rendert dynamisch eine HTML-Tabelle aus einem JSON-Array von Objekten.
 * Spaltennamen werden von den Keys des ersten Objekts abgeleitet.
 * Die Tabelle wird scrollbar, wenn sie eine bestimmte maximale Höhe überschreitet.
 *
 * @param {string} jsonString Der JSON-String, der ein Array von Objekten darstellt.
 * @param {string} targetElementId Die ID des HTML-Elements, in das die Tabelle gerendert werden soll.
 * @param {number} maxBodyHeight Die maximale Höhe (in Pixeln) des Tabellenkörpers, bevor gescrollt wird.
 * @param {string} tableTitle Optionaler Titel für die Tabelle (wird hier nicht gerendert, da das Fragment dies tut).
 */
function renderDynamicTableFromJson(jsonString, targetElementId, maxBodyHeight, tableTitle = '') {
    const targetElement = document.getElementById(targetElementId);

    if (!targetElement) {
        console.error(`Fehler: Zielelement mit ID '${targetElementId}' nicht gefunden.`);
        return;
    }

    let data;
    try {
        data = JSON.parse(jsonString);
    } catch (e) {
        console.error(`Fehler: Ungültiges JSON-Format für Element mit ID '${targetElementId}'.`, e);
        targetElement.innerHTML = `<p style="color: red;">Fehler: Ungültiges JSON-Format für Daten.</p>`;
        return;
    }

    if (!Array.isArray(data) || data.length === 0) {
        targetElement.innerHTML = `<p>Keine Daten verfügbar.</p>`;
        return;
    }

    const headers = (typeof data[0] === 'object' && data[0] !== null) ? Object.keys(data[0]) : [];

    if (headers.length === 0) {
        targetElement.innerHTML = `<p>Keine Spalten für die Daten gefunden.</p>`;
        return;
    }

    const table = document.createElement('table');
    table.className = 'dynamic-generated-table';

    const thead = table.createTHead();
    const headerRow = thead.insertRow();
    headers.forEach(headerText => {
        const th = document.createElement('th');
        th.textContent = headerText;
        headerRow.appendChild(th);
    });

    const tbody = table.createTBody();
    data.forEach(rowObject => {
        const row = tbody.insertRow();
        headers.forEach(headerKey => {
            const cell = row.insertCell();
            let cellValue = rowObject[headerKey];

            const MAX_CHARS_DISPLAY = 30;
            const TRUNCATE_SUFFIX = '...';

            // NEU: Styling für 'successful' Spalte
            if (headerKey === 'successful') {
                if (typeof cellValue === 'boolean') {
                    if (cellValue === true) {
                        cell.classList.add('cell-successful-true');
                        cell.textContent = 'True'; // Standardisiert die Anzeige auf 'True'/'False'
                    } else {
                        cell.classList.add('cell-successful-false');
                        cell.textContent = 'False';
                    }
                } else {
                    // Falls der Wert nicht boolean ist, einfach den Wert anzeigen
                    cell.textContent = String(cellValue);
                }
            } else if (typeof cellValue === 'object' && cellValue !== null) {
                try {
                    cellValue = JSON.stringify(cellValue, null, 2);
                    if (cellValue.length > MAX_CHARS_DISPLAY) {
                        cell.textContent = cellValue.substring(0, MAX_CHARS_DISPLAY) + TRUNCATE_SUFFIX;
                        cell.title = cellValue;
                        cell.classList.add('truncated-cell');
                    } else {
                        cell.textContent = cellValue;
                    }
                    cell.classList.add('json-cell-content');
                } catch (e) {
                    cell.textContent = '[Ungültiges Objekt]';
                }
            } else if (cellValue === null || cellValue === undefined || (typeof cellValue === 'string' && cellValue.trim() === '')) {
                cell.textContent = '-';
                cell.classList.add('empty-cell-content');
            } else {
                let stringValue = String(cellValue);
                if (stringValue.length > MAX_CHARS_DISPLAY) {
                    cell.textContent = stringValue.substring(0, MAX_CHARS_DISPLAY) + TRUNCATE_SUFFIX;
                    cell.title = stringValue;
                    cell.classList.add('truncated-cell');
                } else {
                    cell.textContent = stringValue;
                }
            }
        });
    });

    const scrollableBodyContainer = document.createElement('div');
    scrollableBodyContainer.className = 'dynamic-table-scroll-container';
    scrollableBodyContainer.style.maxHeight = `${maxBodyHeight}px`;
    scrollableBodyContainer.style.overflowY = 'auto';

    scrollableBodyContainer.appendChild(table);

    targetElement.innerHTML = '';
    targetElement.appendChild(scrollableBodyContainer);
}
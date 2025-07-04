// In src/main/resources/static/js/main.js

/**
 * Diese Funktion wird ausgeführt, sobald das HTML-Dokument vollständig geladen ist.
 * Sie sucht nach Elementen, die JSON-Daten enthalten, und rendert diese
 * mithilfe einer externen Bibliothek (wie z.B. json-formatter-js).
 */
document.addEventListener('DOMContentLoaded', function() {
    renderAllJsonContainers();
});

/**
 * Findet alle JSON-Container auf der Seite und rendert ihren Inhalt.
 * Dies ist nützlich für die dynamische Anzeige von JSON-Objekten auf dem Dashboard.
 */
function renderAllJsonContainers() {
    // Finde alle Platzhalter-Container, die noch nicht gerendert wurden.
    const containers = document.querySelectorAll('.json-container-item:not(.json-rendered)');

    containers.forEach(container => {
        // Finde das zugehörige Daten-Skript innerhalb des Containers.
        const jsonDataScript = container.querySelector('script[type="application/json"]');

        if (jsonDataScript) {
            try {
                // Lese und parse den JSON-String.
                const jsonObj = JSON.parse(jsonDataScript.textContent);

                // Prüft, ob die JSONFormatter-Bibliothek geladen ist, bevor sie verwendet wird.
                if (typeof JSONFormatter !== 'undefined') {
                    const formatter = new JSONFormatter(jsonObj, 1, { theme: 'dark' });

                    // Leere den Container und füge den gerenderten HTML-Baum ein.
                    container.innerHTML = '';
                    container.appendChild(formatter.render());

                    // Markiere den Container als gerendert, um doppeltes Rendern zu vermeiden.
                    container.classList.add('json-rendered');
                } else {
                    console.error('JSONFormatter is not loaded. Please include the library.');
                    container.textContent = 'Fehler: JSON-Renderer-Bibliothek fehlt.';
                }

            } catch (e) {
                container.textContent = 'Fehler beim Parsen des JSON: ' + e.message;
                console.error('JSON Parse Error:', e, jsonDataScript.textContent);
            }
        }
    });
}
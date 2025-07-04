// In /static/js/display-json.js

/**
 * Diese Funktion wird ausgeführt, sobald das HTML-Dokument vollständig geladen ist.
 * Sie initialisiert das Rendern aller JSON-Container.
 */
document.addEventListener('DOMContentLoaded', function() {
    renderAllJsonContainers();
});

/**
 * Findet alle Elemente mit der Klasse '.json-container-item' auf der Seite,
 * liest die darin enthaltenen JSON-Daten und rendert sie als interaktiven Baum
 * mithilfe der JSONFormatter-Bibliothek.
 */
function renderAllJsonContainers() {
    // Finde alle Platzhalter, die noch nicht bearbeitet wurden.
    const containers = document.querySelectorAll('.json-container-item:not(.json-rendered)');

    containers.forEach(container => {
        // Finde das zugehörige Skript mit den Rohdaten.
        const jsonDataScript = container.querySelector('script[type="application/json"]');

        if (jsonDataScript) {
            try {
                // Lese und parse den JSON-String.
                const jsonObj = JSON.parse(jsonDataScript.textContent);

                // Prüfe, ob die JSONFormatter-Bibliothek geladen ist.
                if (typeof JSONFormatter !== 'undefined') {
                    // Erstelle den Renderer mit dem 'ocean'-Theme.
                    const formatter = new JSONFormatter(jsonObj, 1, { theme: 'light-mode' });

                    // Leere den Platzhalter und füge den gerenderten Baum ein.
                    container.innerHTML = '';
                    container.appendChild(formatter.render());

                    // Markiere den Container als "gerendert", um Doppelungen zu vermeiden.
                    container.classList.add('json-rendered');
                } else {
                    console.error('Die JSONFormatter-Bibliothek wurde nicht gefunden. Bitte binden Sie sie vor diesem Skript ein.');
                    container.textContent = 'Fehler: Renderer-Bibliothek fehlt.';
                }
            } catch (e) {
                console.error('Fehler beim Parsen des JSON:', e, jsonDataScript.textContent);
                container.textContent = 'Fehler: Ungültiges JSON-Format.';
            }
        }
    });
}
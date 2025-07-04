/**
 * Fügt eine globale "Copy-on-Click"-Funktionalität hinzu.
 * Zeigt ein Haken-Icon als Bestätigung neben dem Text an.
 */
document.addEventListener('DOMContentLoaded', () => {

    // SVG-Code für das Haken-Icon
    const checkmarkSVG = `
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
            <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z"/>
        </svg>
    `;

    document.body.addEventListener('click', function(event) {
        const copyTarget = event.target.closest('.copy-on-click');
        if (!copyTarget) {
            return;
        }

        // Verhindert, dass mehrere Icons gleichzeitig angezeigt werden
        if (copyTarget.dataset.isAnimating) {
            return;
        }

        const textToCopy = copyTarget.innerText;

        navigator.clipboard.writeText(textToCopy).then(() => {
            // Markiere das Element, um Doppel-Klicks zu ignorieren
            copyTarget.dataset.isAnimating = 'true';

            // Erstelle das Icon-Element
            const icon = document.createElement('span');
            icon.className = 'copy-feedback-icon';
            icon.innerHTML = checkmarkSVG;

            // Füge das Icon direkt nach dem geklickten Element ein
            copyTarget.insertAdjacentElement('afterend', icon);

            // Entferne das Icon nach 1,5 Sekunden wieder
            setTimeout(() => {
                icon.remove();
                delete copyTarget.dataset.isAnimating;
            }, 1000);

        }).catch(err => {
            console.error('Fehler beim Kopieren des Textes: ', err);
        });
    });
});
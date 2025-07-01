// Service für das Extrahieren und Verarbeiten von Meta-Popup-Daten aus JSON

export interface MetaPopup {
  id: string
  author: string // Wer hat die Meldung verfasst
  message: string // Inhalt der Meldung
  type?: "info" | "warning" | "error" | "success" // Optional für Styling
  dismissible?: boolean
  priority?: number
}

export interface MetaPopupState {
  popup: MetaPopup
  dismissed: boolean
  timestamp: string
}

// Extrahiert Meta-Popup-Daten aus JSON-Objekten
export function extractMetaPopups(data: any): MetaPopup[] {
  const popups: MetaPopup[] = []

  function searchForMetaPopups(obj: any, path: string[] = []): void {
    if (typeof obj === "object" && obj !== null && !Array.isArray(obj)) {
      Object.entries(obj).forEach(([key, value]) => {
        if (key.startsWith("_meta_popup_")) {
          // Extrahiere Popup-ID aus dem Key
          const popupId = key.replace("_meta_popup_", "")

          if (typeof value === "object" && value !== null) {
            const popupData = value as any

            // Vereinfachte Struktur: nur author und message
            const popup: MetaPopup = {
              id: popupId,
              author: popupData.author || popupData.title || "System", // Fallback auf title oder "System"
              message: popupData.message || popupData.value || "", // Fallback auf value
              type: popupData.type || "info",
              dismissible: popupData.dismissible !== false, // Default: true
              priority: popupData.priority || 0,
            }

            // Nur hinzufügen wenn message vorhanden
            if (popup.message.trim()) {
              popups.push(popup)
            }
          } else if (typeof value === "string") {
            // Einfache String-Werte als Nachrichten behandeln
            const popup: MetaPopup = {
              id: popupId,
              author: popupId, // Key als Author verwenden
              message: value,
              type: "info",
              dismissible: true,
              priority: 0,
            }

            if (popup.message.trim()) {
              popups.push(popup)
            }
          }
        } else if (typeof value === "object" && value !== null) {
          // Rekursiv in verschachtelte Objekte schauen
          searchForMetaPopups(value, [...path, key])
        }
      })
    } else if (Array.isArray(obj)) {
      obj.forEach((item, index) => {
        searchForMetaPopups(item, [...path, index.toString()])
      })
    }
  }

  searchForMetaPopups(data)

  // Sortiere nach Priorität (höhere Priorität zuerst)
  return popups.sort((a, b) => (b.priority || 0) - (a.priority || 0))
}

// Farb-Mapping für verschiedene Popup-Typen
export function getPopupColors(type: MetaPopup["type"]): {
  bg: string
  border: string
  text: string
  accent: string
} {
  switch (type) {
    case "warning":
      return {
        bg: "bg-amber-50/90",
        border: "border-amber-200/60",
        text: "text-amber-800",
        accent: "bg-amber-400",
      }
    case "error":
      return {
        bg: "bg-red-50/90",
        border: "border-red-200/60",
        text: "text-red-800",
        accent: "bg-red-400",
      }
    case "success":
      return {
        bg: "bg-emerald-50/90",
        border: "border-emerald-200/60",
        text: "text-emerald-800",
        accent: "bg-emerald-400",
      }
    case "info":
    default:
      return {
        bg: "bg-blue-50/90",
        border: "border-blue-200/60",
        text: "text-blue-800",
        accent: "bg-blue-400",
      }
  }
}

// Local Storage für dismissed Popups
const DISMISSED_POPUPS_KEY = "pharmalink_dismissed_popups"

export function getDismissedPopups(): string[] {
  if (typeof window === "undefined") return []

  try {
    const dismissed = localStorage.getItem(DISMISSED_POPUPS_KEY)
    return dismissed ? JSON.parse(dismissed) : []
  } catch {
    return []
  }
}

export function dismissPopup(popupId: string): void {
  if (typeof window === "undefined") return

  try {
    const dismissed = getDismissedPopups()
    if (!dismissed.includes(popupId)) {
      dismissed.push(popupId)
      localStorage.setItem(DISMISSED_POPUPS_KEY, JSON.stringify(dismissed))
    }
  } catch (error) {
    console.error("Failed to dismiss popup:", error)
  }
}

export function clearDismissedPopups(): void {
  if (typeof window === "undefined") return

  try {
    localStorage.removeItem(DISMISSED_POPUPS_KEY)
  } catch (error) {
    console.error("Failed to clear dismissed popups:", error)
  }
}

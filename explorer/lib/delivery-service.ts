// Service für das Extrahieren und Verarbeiten von Lieferketten-Daten aus JSON

export interface DeliveryEntry {
  id: string
  recipient: string // Benutzername (key)
  timestamp: string // Zeitstempel/Datum (value)
  location?: string
  status?: string
}

export interface DeliveryChain {
  id: string
  title: string
  entries: DeliveryEntry[]
  totalDeliveries: number
}

// Extrahiert Lieferketten-Daten aus JSON-Objekten
export function extractDeliveryChains(data: any): DeliveryChain[] {
  const chains: DeliveryChain[] = []

  function searchForDeliveryChains(obj: any, path: string[] = []): void {
    if (typeof obj === "object" && obj !== null && !Array.isArray(obj)) {
      Object.entries(obj).forEach(([key, value]) => {
        if (key.startsWith("_meta_lieferkette")) {
          // Extrahiere Chain-ID aus dem Key
          const chainId = key.replace("_meta_lieferkette", "") || "default"

          if (typeof value === "object" && value !== null) {
            const chainData = value as any

            // Extrahiere Lieferungen aus dem Objekt
            const entries: DeliveryEntry[] = []

            // Durchsuche alle Eigenschaften nach Lieferungen
            Object.entries(chainData).forEach(([entryKey, entryValue]) => {
              if (entryKey !== "title" && entryKey !== "description") {
                // Jeder Key ist ein Benutzername, Value ist Zeitstempel
                if (typeof entryValue === "string") {
                  entries.push({
                    id: `${chainId}_${entryKey}`,
                    recipient: entryKey,
                    timestamp: entryValue,
                  })
                } else if (typeof entryValue === "object" && entryValue !== null) {
                  // Erweiterte Struktur mit zusätzlichen Informationen
                  const extendedEntry = entryValue as any
                  entries.push({
                    id: `${chainId}_${entryKey}`,
                    recipient: entryKey,
                    timestamp: extendedEntry.timestamp || extendedEntry.datum || "",
                    location: extendedEntry.location || extendedEntry.ort,
                    status: extendedEntry.status,
                  })
                }
              }
            })

            // Sortiere Einträge nach Zeitstempel (neueste zuerst)
            entries.sort((a, b) => {
              const dateA = new Date(a.timestamp).getTime()
              const dateB = new Date(b.timestamp).getTime()
              return dateB - dateA // Neueste zuerst
            })

            const chain: DeliveryChain = {
              id: chainId,
              title: chainData.title || `Lieferkette ${chainId}`,
              entries,
              totalDeliveries: entries.length,
            }

            chains.push(chain)
          } else if (typeof value === "string") {
            // Einfache String-Werte als einzelne Lieferung behandeln
            try {
              // Versuche JSON zu parsen falls es ein JSON-String ist
              const parsed = JSON.parse(value)
              if (typeof parsed === "object") {
                const entries: DeliveryEntry[] = []
                Object.entries(parsed).forEach(([entryKey, entryValue]) => {
                  entries.push({
                    id: `${chainId}_${entryKey}`,
                    recipient: entryKey,
                    timestamp: String(entryValue),
                  })
                })

                chains.push({
                  id: chainId,
                  title: `Lieferkette ${chainId}`,
                  entries,
                  totalDeliveries: entries.length,
                })
              }
            } catch {
              // Wenn kein JSON, behandle als einfachen Text
              chains.push({
                id: chainId,
                title: `Lieferkette ${chainId}`,
                entries: [
                  {
                    id: `${chainId}_info`,
                    recipient: "System",
                    timestamp: value,
                  },
                ],
                totalDeliveries: 1,
              })
            }
          }
        } else if (typeof value === "object" && value !== null) {
          // Rekursiv in verschachtelte Objekte schauen
          searchForDeliveryChains(value, [...path, key])
        }
      })
    } else if (Array.isArray(obj)) {
      obj.forEach((item, index) => {
        searchForDeliveryChains(item, [...path, index.toString()])
      })
    }
  }

  searchForDeliveryChains(data)

  return chains
}

// Formatiere Zeitstempel für Anzeige
export function formatDeliveryTimestamp(timestamp: string): string {
  try {
    const date = new Date(timestamp)
    if (isNaN(date.getTime())) {
      // Wenn kein gültiges Datum, versuche verschiedene Formate
      if (/^\d{2}\.\d{2}\.\d{4}/.test(timestamp)) {
        // DD.MM.YYYY Format
        return timestamp
      }
      return timestamp
    }

    return date.toLocaleString("de-DE", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  } catch {
    return timestamp
  }
}

// Berechne Lieferstatus basierend auf Zeitstempel
export function getDeliveryStatus(timestamp: string): {
  status: "delivered" | "in-transit" | "pending"
  color: string
  bgColor: string
} {
  try {
    const date = new Date(timestamp)
    const now = new Date()
    const diffHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60)

    if (diffHours < 0) {
      // Zukünftig - geplant
      return {
        status: "pending",
        color: "text-blue-700",
        bgColor: "bg-blue-50",
      }
    } else if (diffHours < 24) {
      // Innerhalb der letzten 24 Stunden - in Transit
      return {
        status: "in-transit",
        color: "text-amber-700",
        bgColor: "bg-amber-50",
      }
    } else {
      // Älter als 24 Stunden - zugestellt
      return {
        status: "delivered",
        color: "text-emerald-700",
        bgColor: "bg-emerald-50",
      }
    }
  } catch {
    return {
      status: "delivered",
      color: "text-gray-700",
      bgColor: "bg-gray-50",
    }
  }
}

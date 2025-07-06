// History Service für Demo-Daten und REST-Simulation

import { apiClient } from "./api-client"
import { getDataSource } from "./config"

export interface HistoryEntry {
  value: string
  timestamp: string
  user?: string
  source?: string
  reason?: string
}

export interface HistoryResponse {
  key: string
  current_value: string
  history: HistoryEntry[]
  total_changes: number
}

// Demo History-Daten für verschiedene Keys
const demoHistoryData: Record<string, HistoryResponse> = {
  // MED-1 Beispiele
  "MED-1.identifikation.pzn": {
    key: "identifikation.pzn",
    current_value: "08296572",
    total_changes: 3,
    history: [
      {
        value: "08296572",
        timestamp: "2024-06-15T14:30:00Z",
        user: "System",
        source: "Automatische Aktualisierung",
        reason: "Neue PZN-Vergabe durch BfArM",
      },
      {
        value: "08296571",
        timestamp: "2024-03-20T09:15:00Z",
        user: "Dr. Schmidt",
        source: "Manuelle Korrektur",
        reason: "Korrektur nach Rücksprache mit Zulassungsbehörde",
      },
      {
        value: "08296570",
        timestamp: "2024-01-10T16:45:00Z",
        user: "System",
        source: "Erstregistrierung",
        reason: "Initiale PZN-Zuteilung",
      },
    ],
  },

  "MED-1.identifikation.name": {
    key: "identifikation.name",
    current_value: "Aspirin 500mg",
    total_changes: 2,
    history: [
      {
        value: "Aspirin 500mg",
        timestamp: "2024-05-10T11:20:00Z",
        user: "Marketing Team",
        source: "Produktname-Update",
        reason: "Vereinfachung des Produktnamens für bessere Verständlichkeit",
      },
      {
        value: "Aspirin 500mg Tabletten",
        timestamp: "2024-01-10T16:45:00Z",
        user: "System",
        source: "Erstregistrierung",
        reason: "Initiale Produktregistrierung",
      },
    ],
  },

  "MED-1.wirkstoff.menge_pro_einheit": {
    key: "wirkstoff.menge_pro_einheit",
    current_value: "500mg",
    total_changes: 1,
    history: [
      {
        value: "500mg",
        timestamp: "2024-01-10T16:45:00Z",
        user: "System",
        source: "Erstregistrierung",
        reason: "Initiale Wirkstoffmenge",
      },
    ],
  },

  // HERSTELLER-1 Beispiele
  "HERSTELLER-1.unternehmensdaten.firmenname": {
    key: "unternehmensdaten.firmenname",
    current_value: "Bayer AG",
    total_changes: 1,
    history: [
      {
        value: "Bayer AG",
        timestamp: "2023-12-01T10:00:00Z",
        user: "System",
        source: "Unternehmensregister",
        reason: "Offizielle Firmenbezeichnung",
      },
    ],
  },

  "HERSTELLER-1.unternehmensdaten.hauptsitz": {
    key: "unternehmensdaten.hauptsitz",
    current_value: "Leverkusen, Deutschland",
    total_changes: 2,
    history: [
      {
        value: "Leverkusen, Deutschland",
        timestamp: "2024-02-15T14:30:00Z",
        user: "Legal Department",
        source: "Adressaktualisierung",
        reason: "Präzisierung der Hauptsitz-Angabe",
      },
      {
        value: "Leverkusen",
        timestamp: "2023-12-01T10:00:00Z",
        user: "System",
        source: "Erstregistrierung",
        reason: "Initiale Hauptsitz-Angabe",
      },
    ],
  },

  // UNIT-1 Beispiele
  "UNIT-1.unit_identifikation.serialnummer": {
    key: "unit_identifikation.serialnummer",
    current_value: "2024070115432",
    total_changes: 1,
    history: [
      {
        value: "2024070115432",
        timestamp: "2024-07-01T14:32:15Z",
        user: "Production System",
        source: "Automatische Generierung",
        reason: "Eindeutige Serialnummer bei Produktion",
      },
    ],
  },

  "UNIT-1.lieferkette_tracking.aktueller_status": {
    key: "lieferkette_tracking.aktueller_status",
    current_value: "Im Transit",
    total_changes: 4,
    history: [
      {
        value: "Im Transit",
        timestamp: "2024-07-02T08:30:00Z",
        user: "DHL System",
        source: "Tracking Update",
        reason: "Fahrzeug hat Verteilzentrum verlassen",
      },
      {
        value: "Im Verteilzentrum",
        timestamp: "2024-07-02T06:15:00Z",
        user: "DHL System",
        source: "Tracking Update",
        reason: "Ankunft im Berliner Hub",
      },
      {
        value: "Unterwegs",
        timestamp: "2024-07-01T20:30:00Z",
        user: "DHL System",
        source: "Tracking Update",
        reason: "Abgang vom Versandzentrum Köln",
      },
      {
        value: "Versandbereit",
        timestamp: "2024-07-01T16:00:00Z",
        user: "Production System",
        source: "Qualitätskontrolle",
        reason: "QC bestanden, versandbereit",
      },
    ],
  },
}

export async function fetchKeyHistory(productId: string, keyPath: string): Promise<HistoryResponse | null> {
  const dataSource = getDataSource()

  if (dataSource === "local") {
    // Simuliere REST-Call mit Delay
    await new Promise((resolve) => setTimeout(resolve, 500))

    const historyKey = `${productId}.${keyPath}`
    const historyData = demoHistoryData[historyKey]

    if (!historyData) {
      return {
        key: keyPath,
        current_value: "Unbekannt",
        total_changes: 0,
        history: [],
      }
    }

    return historyData
  }

  // API MODE - verwende History-URL
  try {
    const response = await apiClient.getHistory(productId, keyPath)

    if (response.error) {
      throw new Error(response.error)
    }

    return response.data
  } catch (error) {
    console.error("Failed to fetch history from API:", error)

    // Fallback für API-Fehler
    return {
      key: keyPath,
      current_value: "Unbekannt",
      total_changes: 0,
      history: [],
    }
  }
}

// Helper function to build key identifier from path
export function buildKeyIdentifier(path: string[], keyName: string): string {
  // Entferne Array-Indizes und baue sauberen Pfad
  const cleanPath = path.filter((segment) => !segment.match(/^\d+$/))
  return [...cleanPath, keyName].join(".")
}

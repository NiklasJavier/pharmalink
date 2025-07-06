// Optimierte Version des Data Service mit Caching

import { getDataSource } from "./config"
import { apiClient } from "./api-client"
import { dataCache } from "./cache-service"

export interface DataServiceResponse {
  data: any | null
  error: string | null
  source: "local" | "api" | "cache"
  timestamp: string
  cached?: boolean
  correctedId?: string
  linkedIds?: {
    medikament?: string
    hersteller?: string
    unit?: string[]
  }
}

// Demo data bleibt gleich
const demoData: Record<string, any> = {
  "MED-1": {
    identifikation: {
      medikamenten_id: "MED-1",
      pzn: "08296572",
      name: "Aspirin 500mg",
      handelsname: "Aspirin® 500mg Tabletten",
      hersteller: "Bayer AG",
      hersteller_referenz: "HERSTELLER-1",
      zulassungsnummer: "DE-12345-67890",
      produktwebsite: "https://www.aspirin.de/produkte/aspirin-500mg",
      fachinformation: "https://www.fachinfo.de/database/fi/aspirin-500mg.pdf",
    },
    wirkstoff: {
      hauptwirkstoff: "Acetylsalicylsäure",
      cas_nummer: "50-78-2",
      menge_pro_einheit: "500mg",
      reinheit: "99.5%",
      herkunft: "Synthetisch",
      molekulargewicht: "180.16 g/mol",
      sicherheitsdatenblatt: "https://www.bayer.com/sites/default/files/2020-02/sdb-acetylsalicylsaeure.pdf",
      pubchem_url: "https://pubchem.ncbi.nlm.nih.gov/compound/2244",
    },
    // ... rest of MED-1 data
    _meta_popup_warnung: "Dieses Medikament sollte nicht mit Alkohol eingenommen werden.",
    _meta_lieferkette_produktion: {
      title: "Produktions-Lieferkette",
      "Dr. Schmidt": "2024-07-01T14:32:00Z",
      "QS-Team": "2024-07-01T15:45:00Z",
      Verpackung: "2024-07-01T16:00:00Z",
    },
  },
  "HERSTELLER-1": {
    unternehmensdaten: {
      hersteller_id: "HERSTELLER-1",
      firmenname: "Bayer AG",
      rechtsform: "Aktiengesellschaft",
      hauptsitz: "Leverkusen, Deutschland",
      // ... rest of HERSTELLER-1 data
    },
  },
  "UNIT-1": {
    unit_identifikation: {
      unit_id: "UNIT-1",
      serialnummer: "2024070115432",
      batch_id: "BATCH-MED1-240701",
      // ... rest of UNIT-1 data
    },
  },
}

function extractLinkedIds(data: any): {
  medikament?: string
  hersteller?: string
  unit?: string[]
} {
  const linkedIds: { medikament?: string; hersteller?: string; unit?: string[] } = {}

  function search(obj: any): void {
    if (typeof obj === "string") {
      if (/^MED-/.test(obj) && !linkedIds.medikament) linkedIds.medikament = obj
      else if (/^HERSTELLER-/.test(obj) && !linkedIds.hersteller) linkedIds.hersteller = obj
      else if (/^UNIT-/.test(obj)) {
        linkedIds.unit ??= []
        if (!linkedIds.unit.includes(obj)) linkedIds.unit.push(obj)
      }
    } else if (Array.isArray(obj)) obj.forEach(search)
    else if (obj && typeof obj === "object") Object.values(obj).forEach(search)
  }

  search(data)
  return linkedIds
}

export async function getDataById(id: string): Promise<DataServiceResponse> {
  const cacheKey = `data_${id}`

  // Check cache first
  const cachedResponse = dataCache.get<DataServiceResponse>(cacheKey)
  if (cachedResponse) {
    console.log(`Cache hit for ${id}`)
    return {
      ...cachedResponse,
      source: "cache",
      cached: true,
    }
  }

  const dataSource = getDataSource()

  if (dataSource === "local") {
    // Local data logic
    let actualId = id
    let data = demoData[id]

    if (!data) {
      const similar = Object.keys(demoData).find((k) => k.toLowerCase().includes(id.toLowerCase()))
      if (similar) {
        actualId = similar
        data = demoData[similar]
      }
    }

    const linkedIds = data ? extractLinkedIds(data) : undefined

    const response: DataServiceResponse = {
      data,
      error: data ? null : `ID "${id}" not found in local data`,
      source: "local",
      timestamp: new Date().toISOString(),
      correctedId: actualId !== id ? actualId : undefined,
      linkedIds,
    }

    // Cache successful responses
    if (data) {
      dataCache.set(cacheKey, response, 10 * 60 * 1000) // 10 minutes
    }

    return response
  }

  // API MODE
  try {
    const response = await apiClient.searchData(id)

    if (response.error) {
      return {
        data: null,
        error: response.error,
        source: "api",
        timestamp: response.timestamp,
      }
    }

    const apiData = response.data
    const correctedId = apiData?.correctedId || apiData?.corrected_id
    const linkedIds = apiData ? extractLinkedIds(apiData) : undefined

    const finalResponse: DataServiceResponse = {
      data: apiData,
      error: null,
      source: "api",
      timestamp: response.timestamp,
      cached: response.source === "cache",
      correctedId: correctedId !== id ? correctedId : undefined,
      linkedIds,
    }

    // Cache successful API responses
    if (apiData) {
      dataCache.set(cacheKey, finalResponse, 5 * 60 * 1000) // 5 minutes for API data
    }

    return finalResponse
  } catch (err) {
    return {
      data: null,
      error: err instanceof Error ? err.message : "Unknown API error",
      source: "api",
      timestamp: new Date().toISOString(),
    }
  }
}

export function isValidId(id: string): boolean {
  const dataSource = getDataSource()

  if (dataSource === "local") return id in demoData
  return /^(MED-|HERSTELLER-|UNIT-).+/.test(id)
}

export function getAvailableIds(): string[] {
  return Object.keys(demoData)
}

// Preload common data
export async function preloadCommonData(): Promise<void> {
  const commonIds = ["MED-1", "HERSTELLER-1", "UNIT-1"]

  const promises = commonIds.map(async (id) => {
    if (!dataCache.has(`data_${id}`)) {
      try {
        await getDataById(id)
        console.log(`Preloaded data for ${id}`)
      } catch (error) {
        console.warn(`Failed to preload ${id}:`, error)
      }
    }
  })

  await Promise.all(promises)
}

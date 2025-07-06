// Service für das Caching von Sucheingaben und Autokompletion

interface SearchEntry {
  query: string
  type: "simple" | "key-value"
  timestamp: number
  resultCount: number
  successful: boolean
  frequency: number
}

interface SearchSuggestion {
  query: string
  type: "simple" | "key-value"
  frequency: number
  lastUsed: number
  resultCount: number
  category: "recent" | "frequent" | "suggested"
}

interface KeyValuePattern {
  key: string
  values: string[]
  frequency: number
  lastUsed: number
}

class SearchCacheService {
  private searchHistory: SearchEntry[] = []
  private keyValuePatterns: Map<string, KeyValuePattern> = new Map()
  private maxHistorySize = 100
  private maxSuggestions = 10

  constructor() {
    if (typeof window !== "undefined") {
      this.loadFromStorage()
    }
  }

  // Sucheingabe hinzufügen
  addSearch(query: string, resultCount = 0, successful = true): void {
    const trimmedQuery = query.trim()
    if (!trimmedQuery) return

    const type = this.detectSearchType(trimmedQuery)
    const now = Date.now()

    // Prüfe ob bereits vorhanden
    const existingIndex = this.searchHistory.findIndex(
      (entry) => entry.query.toLowerCase() === trimmedQuery.toLowerCase() && entry.type === type,
    )

    if (existingIndex >= 0) {
      // Update existing entry
      const existing = this.searchHistory[existingIndex]
      existing.frequency++
      existing.timestamp = now
      existing.resultCount = resultCount
      existing.successful = successful

      // Move to front
      this.searchHistory.splice(existingIndex, 1)
      this.searchHistory.unshift(existing)
    } else {
      // Add new entry
      const newEntry: SearchEntry = {
        query: trimmedQuery,
        type,
        timestamp: now,
        resultCount,
        successful,
        frequency: 1,
      }

      this.searchHistory.unshift(newEntry)

      // Limit history size
      if (this.searchHistory.length > this.maxHistorySize) {
        this.searchHistory = this.searchHistory.slice(0, this.maxHistorySize)
      }
    }

    // Extrahiere Key-Value Patterns
    if (type === "key-value") {
      this.extractKeyValuePattern(trimmedQuery)
    }

    this.saveToStorage()
  }

  // Erkenne Suchtyp
  private detectSearchType(query: string): "simple" | "key-value" {
    return query.includes(":") ? "key-value" : "simple"
  }

  // Extrahiere Key-Value Pattern für Autokompletion
  private extractKeyValuePattern(query: string): void {
    const match = query.match(/^(.+?):(.+)$/)
    if (!match) return

    const [, key, value] = match
    const normalizedKey = key.trim().toLowerCase()
    const normalizedValue = value.trim()

    if (!normalizedKey || !normalizedValue) return

    const existing = this.keyValuePatterns.get(normalizedKey)
    if (existing) {
      if (!existing.values.includes(normalizedValue)) {
        existing.values.push(normalizedValue)
      }
      existing.frequency++
      existing.lastUsed = Date.now()
    } else {
      this.keyValuePatterns.set(normalizedKey, {
        key: normalizedKey,
        values: [normalizedValue],
        frequency: 1,
        lastUsed: Date.now(),
      })
    }
  }

  // Autokompletion-Vorschläge generieren
  getSuggestions(currentQuery: string, limit: number = this.maxSuggestions): SearchSuggestion[] {
    const trimmedQuery = currentQuery.trim().toLowerCase()
    if (!trimmedQuery) {
      return this.getRecentSearches(limit)
    }

    const suggestions: SearchSuggestion[] = []

    // 1. Exakte Matches aus History
    const exactMatches = this.searchHistory
      .filter((entry) => entry.query.toLowerCase().startsWith(trimmedQuery))
      .slice(0, 3)
      .map((entry) => ({
        query: entry.query,
        type: entry.type,
        frequency: entry.frequency,
        lastUsed: entry.timestamp,
        resultCount: entry.resultCount,
        category: "recent" as const,
      }))

    suggestions.push(...exactMatches)

    // 2. Key-Value Autokompletion
    if (trimmedQuery.includes(":")) {
      const keyValueSuggestions = this.getKeyValueSuggestions(trimmedQuery)
      suggestions.push(...keyValueSuggestions)
    } else {
      // 3. Key-Vorschläge (wenn noch kein : eingegeben)
      const keySuggestions = this.getKeySuggestions(trimmedQuery)
      suggestions.push(...keySuggestions)
    }

    // 4. Häufige Suchen
    const frequentSearches = this.searchHistory
      .filter(
        (entry) =>
          entry.frequency > 2 &&
          entry.query.toLowerCase().includes(trimmedQuery) &&
          !suggestions.some((s) => s.query === entry.query),
      )
      .slice(0, 2)
      .map((entry) => ({
        query: entry.query,
        type: entry.type,
        frequency: entry.frequency,
        lastUsed: entry.timestamp,
        resultCount: entry.resultCount,
        category: "frequent" as const,
      }))

    suggestions.push(...frequentSearches)

    // Sortiere und limitiere
    return suggestions
      .sort((a, b) => {
        // Priorität: recent > frequent > suggested
        const categoryPriority = { recent: 3, frequent: 2, suggested: 1 }
        const aPriority = categoryPriority[a.category]
        const bPriority = categoryPriority[b.category]

        if (aPriority !== bPriority) return bPriority - aPriority

        // Innerhalb der Kategorie: Häufigkeit und Aktualität
        return b.frequency * 0.7 + (b.lastUsed / 1000000) * 0.3 - (a.frequency * 0.7 + (a.lastUsed / 1000000) * 0.3)
      })
      .slice(0, limit)
  }

  // Key-Value Autokompletion
  private getKeyValueSuggestions(query: string): SearchSuggestion[] {
    const match = query.match(/^(.+?):(.*)$/)
    if (!match) return []

    const [, key, partialValue] = match
    const normalizedKey = key.trim().toLowerCase()
    const partialValueLower = partialValue.toLowerCase()

    const pattern = this.keyValuePatterns.get(normalizedKey)
    if (!pattern) return []

    return pattern.values
      .filter((value) => value.toLowerCase().startsWith(partialValueLower))
      .slice(0, 5)
      .map((value) => ({
        query: `${key}:${value}`,
        type: "key-value" as const,
        frequency: pattern.frequency,
        lastUsed: pattern.lastUsed,
        resultCount: 0,
        category: "suggested" as const,
      }))
  }

  // Key-Vorschläge
  private getKeySuggestions(query: string): SearchSuggestion[] {
    const matchingKeys = Array.from(this.keyValuePatterns.keys())
      .filter((key) => key.startsWith(query))
      .slice(0, 3)

    return matchingKeys.map((key) => {
      const pattern = this.keyValuePatterns.get(key)!
      const mostCommonValue = pattern.values[0] || ""

      return {
        query: `${key}:${mostCommonValue}`,
        type: "key-value" as const,
        frequency: pattern.frequency,
        lastUsed: pattern.lastUsed,
        resultCount: 0,
        category: "suggested" as const,
      }
    })
  }

  // Letzte Suchen
  getRecentSearches(limit = 5): SearchSuggestion[] {
    return this.searchHistory
      .filter((entry) => entry.successful)
      .slice(0, limit)
      .map((entry) => ({
        query: entry.query,
        type: entry.type,
        frequency: entry.frequency,
        lastUsed: entry.timestamp,
        resultCount: entry.resultCount,
        category: "recent" as const,
      }))
  }

  // Häufigste Keys für Autokompletion
  getPopularKeys(): string[] {
    return Array.from(this.keyValuePatterns.entries())
      .sort(([, a], [, b]) => b.frequency - a.frequency)
      .slice(0, 10)
      .map(([key]) => key)
  }

  // Häufigste Values für einen Key
  getPopularValuesForKey(key: string): string[] {
    const pattern = this.keyValuePatterns.get(key.toLowerCase())
    return pattern ? pattern.values.slice(0, 5) : []
  }

  // Suchverlauf löschen
  clearHistory(): void {
    this.searchHistory = []
    this.keyValuePatterns.clear()
    this.saveToStorage()
  }

  // Einzelne Suche entfernen
  removeSearch(query: string): void {
    this.searchHistory = this.searchHistory.filter((entry) => entry.query !== query)
    this.saveToStorage()
  }

  // Statistiken
  getStats() {
    const totalSearches = this.searchHistory.reduce((sum, entry) => sum + entry.frequency, 0)
    const successfulSearches = this.searchHistory.filter((entry) => entry.successful).length
    const keyValueSearches = this.searchHistory.filter((entry) => entry.type === "key-value").length

    return {
      totalEntries: this.searchHistory.length,
      totalSearches,
      successfulSearches,
      keyValueSearches,
      simpleSearches: this.searchHistory.length - keyValueSearches,
      uniqueKeys: this.keyValuePatterns.size,
      successRate: this.searchHistory.length > 0 ? (successfulSearches / this.searchHistory.length) * 100 : 0,
    }
  }

  // Storage
  private saveToStorage(): void {
    if (typeof window === "undefined") return

    try {
      const data = {
        searchHistory: this.searchHistory,
        keyValuePatterns: Array.from(this.keyValuePatterns.entries()),
      }
      localStorage.setItem("pharmalink_search_cache", JSON.stringify(data))
    } catch (error) {
      console.warn("Failed to save search cache:", error)
    }
  }

  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem("pharmalink_search_cache")
      if (stored) {
        const data = JSON.parse(stored)
        this.searchHistory = data.searchHistory || []
        this.keyValuePatterns = new Map(data.keyValuePatterns || [])

        // Cleanup old entries (older than 30 days)
        const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000
        this.searchHistory = this.searchHistory.filter((entry) => entry.timestamp > thirtyDaysAgo)
      }
    } catch (error) {
      console.warn("Failed to load search cache:", error)
    }
  }
}

export const searchCache = new SearchCacheService()

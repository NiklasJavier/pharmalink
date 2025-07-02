"use client"
import { EnhancedJsonDisplay } from "@/components/passport/enhanced-json-display"
import { useMemoizedJsonDisplay } from "@/hooks/use-memoized-json-display"
import { useCachedComponent } from "@/hooks/use-cached-component"

interface CachedJsonDisplayProps {
  data: any
  maxDepth?: number
  productId?: string
  globalExpanded: boolean
  searchTerm: string
  searchResults: any[]
  currentResultIndex: number
  onSearchResultsChange: (results: any[]) => void
  onSearchTermChange: (term: string) => void
  onHistoryOpen?: (keyPath: string, keyName: string, currentValue: string, iconElement: HTMLElement) => void
}

export function CachedJsonDisplay(props: CachedJsonDisplayProps) {
  const { data, maxDepth = 6, globalExpanded, searchTerm, searchResults, currentResultIndex } = props

  // Memoize die Daten-Struktur
  const { memoizedData, cacheKey } = useMemoizedJsonDisplay(data, {
    maxDepth,
    searchTerm,
    globalExpanded,
  })

  // Cache die gesamte JSON-Display Komponente
  return useCachedComponent(
    "EnhancedJsonDisplay",
    {
      data: memoizedData,
      maxDepth,
      globalExpanded,
      searchTerm,
      searchResults: searchResults.slice(), // Shallow copy für Stabilität
      currentResultIndex,
      productId: props.productId,
    },
    (cachedProps) => (
      <EnhancedJsonDisplay
        {...cachedProps}
        onSearchResultsChange={props.onSearchResultsChange}
        onSearchTermChange={props.onSearchTermChange}
        onHistoryOpen={props.onHistoryOpen}
      />
    ),
    {
      ttl: 5 * 60 * 1000, // 5 Minuten
      cacheKey,
      dependencies: [
        searchTerm, // Re-cache bei Suchänderung
        globalExpanded, // Re-cache bei Expand-Änderung
        currentResultIndex, // Re-cache bei Navigation
      ],
      enabled: !!memoizedData, // Nur cachen wenn Daten vorhanden
    },
  )
}

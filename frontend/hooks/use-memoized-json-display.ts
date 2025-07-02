"use client"

import { useMemo } from "react"

interface JsonDisplayMemoOptions {
  maxDepth?: number
  searchTerm?: string
  globalExpanded?: boolean
}

// Spezieller Hook für JSON-Display Komponenten
export function useMemoizedJsonDisplay(
  data: any,
  options: JsonDisplayMemoOptions = {},
): {
  memoizedData: any
  shouldRerender: boolean
  cacheKey: string
} {
  const { maxDepth = 6, searchTerm = "", globalExpanded = true } = options

  // Erstelle stabilen Cache-Key
  const cacheKey = useMemo(() => {
    const dataHash = JSON.stringify(data).slice(0, 32)
    const optionsHash = `${maxDepth}_${searchTerm}_${globalExpanded}`
    return `json_display_${dataHash}_${optionsHash}`
  }, [data, maxDepth, searchTerm, globalExpanded])

  // Memoize die Daten-Struktur
  const memoizedData = useMemo(() => {
    if (!data) return null

    // Tiefe Kopie für Stabilität
    return JSON.parse(JSON.stringify(data))
  }, [JSON.stringify(data)])

  // Bestimme ob Re-Render nötig ist
  const shouldRerender = useMemo(() => {
    // Re-render bei Datenänderung, Suchterm oder Expand-Status
    return true
  }, [memoizedData, searchTerm, globalExpanded, maxDepth])

  return {
    memoizedData,
    shouldRerender,
    cacheKey,
  }
}

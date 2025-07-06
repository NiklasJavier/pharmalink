"use client"

import { useState, useEffect, useMemo } from "react"
import { searchCache } from "@/lib/search-cache-service"

interface UseSearchSuggestionsOptions {
  maxSuggestions?: number
  debounceMs?: number
  minQueryLength?: number
}

export function useSearchSuggestions(query: string, options: UseSearchSuggestionsOptions = {}) {
  const { maxSuggestions = 8, debounceMs = 150, minQueryLength = 0 } = options

  const [suggestions, setSuggestions] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)

  // Debounced query
  const [debouncedQuery, setDebouncedQuery] = useState(query)

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query)
    }, debounceMs)

    return () => clearTimeout(timer)
  }, [query, debounceMs])

  // Generate suggestions
  useEffect(() => {
    if (debouncedQuery.length < minQueryLength) {
      setSuggestions(searchCache.getRecentSearches(maxSuggestions))
      return
    }

    setIsLoading(true)

    // Simulate async operation for smooth UX
    const timer = setTimeout(() => {
      const newSuggestions = searchCache.getSuggestions(debouncedQuery, maxSuggestions)
      setSuggestions(newSuggestions)
      setIsLoading(false)
    }, 50)

    return () => {
      clearTimeout(timer)
      setIsLoading(false)
    }
  }, [debouncedQuery, maxSuggestions, minQueryLength])

  // Helper functions
  const addSearch = (searchQuery: string, resultCount = 0, successful = true) => {
    searchCache.addSearch(searchQuery, resultCount, successful)
  }

  const clearHistory = () => {
    searchCache.clearHistory()
    setSuggestions([])
  }

  const removeSearch = (searchQuery: string) => {
    searchCache.removeSearch(searchQuery)
    // Refresh suggestions
    const newSuggestions = searchCache.getSuggestions(debouncedQuery, maxSuggestions)
    setSuggestions(newSuggestions)
  }

  const getPopularKeys = () => searchCache.getPopularKeys()

  const getPopularValuesForKey = (key: string) => searchCache.getPopularValuesForKey(key)

  const stats = useMemo(() => searchCache.getStats(), [suggestions])

  return {
    suggestions,
    isLoading,
    addSearch,
    clearHistory,
    removeSearch,
    getPopularKeys,
    getPopularValuesForKey,
    stats,
  }
}

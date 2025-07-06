"use client"

import { useState, useRef, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Clock, TrendingUp, Lightbulb, X, ArrowUpRight, Hash, Search, Key, Trash2 } from "lucide-react"
import { cn } from "@/lib/utils"

interface SearchSuggestion {
  query: string
  type: "simple" | "key-value"
  frequency: number
  lastUsed: number
  resultCount: number
  category: "recent" | "frequent" | "suggested"
}

interface SearchSuggestionsProps {
  suggestions: SearchSuggestion[]
  isVisible: boolean
  onSelect: (query: string) => void
  onRemove?: (query: string) => void
  onClearAll?: () => void
  currentQuery: string
  isLoading?: boolean
  maxHeight?: string
}

export function SearchSuggestions({
  suggestions,
  isVisible,
  onSelect,
  onRemove,
  onClearAll,
  currentQuery,
  isLoading = false,
  maxHeight = "300px",
}: SearchSuggestionsProps) {
  const [hoveredIndex, setHoveredIndex] = useState(-1)
  const containerRef = useRef<HTMLDivElement>(null)

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isVisible || suggestions.length === 0) return

      switch (e.key) {
        case "ArrowDown":
          e.preventDefault()
          setHoveredIndex((prev) => (prev + 1) % suggestions.length)
          break
        case "ArrowUp":
          e.preventDefault()
          setHoveredIndex((prev) => (prev - 1 + suggestions.length) % suggestions.length)
          break
        case "Enter":
          e.preventDefault()
          if (hoveredIndex >= 0 && hoveredIndex < suggestions.length) {
            onSelect(suggestions[hoveredIndex].query)
          }
          break
        case "Escape":
          setHoveredIndex(-1)
          break
      }
    }

    if (isVisible) {
      document.addEventListener("keydown", handleKeyDown)
      return () => document.removeEventListener("keydown", handleKeyDown)
    }
  }, [isVisible, suggestions, hoveredIndex, onSelect])

  // Reset hover when suggestions change
  useEffect(() => {
    setHoveredIndex(-1)
  }, [suggestions])

  if (!isVisible || (suggestions.length === 0 && !isLoading)) {
    return null
  }

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case "recent":
        return <Clock className="h-3 w-3" />
      case "frequent":
        return <TrendingUp className="h-3 w-3" />
      case "suggested":
        return <Lightbulb className="h-3 w-3" />
      default:
        return <Search className="h-3 w-3" />
    }
  }

  const getCategoryColor = (category: string) => {
    switch (category) {
      case "recent":
        return "text-blue-600 bg-blue-50 border-blue-200"
      case "frequent":
        return "text-emerald-600 bg-emerald-50 border-emerald-200"
      case "suggested":
        return "text-amber-600 bg-amber-50 border-amber-200"
      default:
        return "text-gray-600 bg-gray-50 border-gray-200"
    }
  }

  const getTypeIcon = (type: string) => {
    return type === "key-value" ? <Key className="h-3 w-3" /> : <Hash className="h-3 w-3" />
  }

  const highlightQuery = (text: string, query: string) => {
    if (!query.trim()) return text

    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi")
    const parts = text.split(regex)

    return parts.map((part, index) =>
      regex.test(part) ? (
        <mark key={index} className="bg-yellow-200 px-0.5 rounded">
          {part}
        </mark>
      ) : (
        part
      ),
    )
  }

  const formatTimestamp = (timestamp: number) => {
    const now = Date.now()
    const diff = now - timestamp
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))

    if (minutes < 1) return "gerade eben"
    if (minutes < 60) return `vor ${minutes}min`
    if (hours < 24) return `vor ${hours}h`
    return `vor ${days}d`
  }

  // Group suggestions by category
  const groupedSuggestions = suggestions.reduce(
    (acc, suggestion) => {
      if (!acc[suggestion.category]) {
        acc[suggestion.category] = []
      }
      acc[suggestion.category].push(suggestion)
      return acc
    },
    {} as Record<string, SearchSuggestion[]>,
  )

  const categoryOrder = ["recent", "frequent", "suggested"]
  const categoryLabels = {
    recent: "Kürzlich gesucht",
    frequent: "Häufig gesucht",
    suggested: "Vorschläge",
  }

  return (
    <div
      ref={containerRef}
      className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 animate-in fade-in slide-in-from-top-2 duration-200"
      style={{ maxHeight }}
    >
      {isLoading ? (
        <div className="p-4 text-center">
          <div className="flex items-center justify-center gap-2 text-gray-500">
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-gray-300 border-t-emerald-600"></div>
            <span className="text-sm">Lade Vorschläge...</span>
          </div>
        </div>
      ) : (
        <ScrollArea className="max-h-full">
          <div className="p-2">
            {/* Header mit Clear-Button */}
            {suggestions.length > 0 && (
              <div className="flex items-center justify-between px-2 py-1 mb-2">
                <span className="text-xs font-medium text-gray-600">
                  {suggestions.length} Vorschlag{suggestions.length !== 1 ? "e" : ""}
                </span>
                {onClearAll && (
                  <Button
                    onClick={onClearAll}
                    variant="ghost"
                    size="sm"
                    className="h-6 px-2 text-xs text-gray-500 hover:text-red-600 hover:bg-red-50"
                  >
                    <Trash2 className="h-3 w-3 mr-1" />
                    Löschen
                  </Button>
                )}
              </div>
            )}

            {/* Grouped Suggestions */}
            {categoryOrder.map((category) => {
              const categorySuggestions = groupedSuggestions[category]
              if (!categorySuggestions || categorySuggestions.length === 0) return null

              return (
                <div key={category} className="mb-3 last:mb-0">
                  {/* Category Header */}
                  <div className="flex items-center gap-2 px-2 py-1 mb-1">
                    <div className={cn("p-1 rounded", getCategoryColor(category))}>{getCategoryIcon(category)}</div>
                    <span className="text-xs font-medium text-gray-700">{categoryLabels[category]}</span>
                  </div>

                  {/* Category Suggestions */}
                  <div className="space-y-1">
                    {categorySuggestions.map((suggestion, index) => {
                      const globalIndex = suggestions.indexOf(suggestion)
                      const isHovered = hoveredIndex === globalIndex

                      return (
                        <div
                          key={`${suggestion.query}-${suggestion.category}`}
                          className={cn(
                            "group flex items-center justify-between p-2 rounded-md cursor-pointer transition-all duration-150",
                            isHovered
                              ? "bg-emerald-50 border border-emerald-200"
                              : "hover:bg-gray-50 border border-transparent",
                          )}
                          onMouseEnter={() => setHoveredIndex(globalIndex)}
                          onMouseLeave={() => setHoveredIndex(-1)}
                          onClick={() => onSelect(suggestion.query)}
                        >
                          <div className="flex items-center gap-3 flex-1 min-w-0">
                            {/* Type Icon */}
                            <div className="flex-shrink-0 text-gray-400">{getTypeIcon(suggestion.type)}</div>

                            {/* Query Text */}
                            <div className="flex-1 min-w-0">
                              <div className="text-sm font-medium text-gray-900 truncate">
                                {highlightQuery(suggestion.query, currentQuery)}
                              </div>

                              {/* Metadata */}
                              <div className="flex items-center gap-2 mt-0.5">
                                {suggestion.frequency > 1 && (
                                  <Badge variant="secondary" className="text-xs px-1 py-0 h-auto">
                                    {suggestion.frequency}x
                                  </Badge>
                                )}

                                {suggestion.resultCount > 0 && (
                                  <Badge variant="outline" className="text-xs px-1 py-0 h-auto">
                                    {suggestion.resultCount} Ergebnisse
                                  </Badge>
                                )}

                                <span className="text-xs text-gray-500">{formatTimestamp(suggestion.lastUsed)}</span>
                              </div>
                            </div>
                          </div>

                          {/* Actions */}
                          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            {onRemove && (
                              <Button
                                onClick={(e) => {
                                  e.stopPropagation()
                                  onRemove(suggestion.query)
                                }}
                                variant="ghost"
                                size="sm"
                                className="h-6 w-6 p-0 text-gray-400 hover:text-red-600 hover:bg-red-50"
                              >
                                <X className="h-3 w-3" />
                              </Button>
                            )}

                            <ArrowUpRight className="h-3 w-3 text-gray-400" />
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}

            {/* No suggestions message */}
            {suggestions.length === 0 && (
              <div className="p-4 text-center text-gray-500">
                <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">Keine Suchvorschläge verfügbar</p>
                <p className="text-xs mt-1">Beginnen Sie zu tippen für Vorschläge</p>
              </div>
            )}
          </div>
        </ScrollArea>
      )}

      {/* Keyboard hint */}
      {suggestions.length > 0 && (
        <div className="border-t border-gray-100 px-3 py-2 bg-gray-50 rounded-b-lg">
          <div className="flex items-center justify-between text-xs text-gray-500">
            <span>↑↓ navigieren • Enter auswählen • Esc schließen</span>
            <Badge variant="outline" className="text-xs px-1 py-0 h-auto">
              {hoveredIndex + 1}/{suggestions.length}
            </Badge>
          </div>
        </div>
      )}
    </div>
  )
}

"use client"

import type React from "react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  ChevronDown,
  ChevronRight,
  Hash,
  Calendar,
  CheckCircle,
  XCircle,
  Braces,
  Brackets,
  ChevronUp,
  Search,
  X,
  ArrowUp,
  ArrowDown,
  ExternalLink,
  MousePointer,
  History,
} from "lucide-react"
import type { ReactNode } from "react"
import { useState, useRef, useEffect } from "react"
import { cn } from "@/lib/utils"
import { useRouter, useSearchParams } from "next/navigation"
import { buildKeyIdentifier } from "@/lib/history-service"

type JsonValue = string | number | boolean | null | JsonObject | JsonArray
type JsonObject = { [key: string]: JsonValue }
type JsonArray = JsonValue[]

type EnhancedJsonDisplayProps = {
  data: JsonValue
  maxDepth?: number
  productId?: string
  // Neue Props für externe State-Kontrolle
  globalExpanded: boolean
  searchTerm: string
  searchResults: SearchResult[]
  currentResultIndex: number
  onSearchResultsChange: (results: SearchResult[]) => void
  onSearchTermChange: (term: string) => void
}

type JsonItemProps = {
  keyName: string
  value: JsonValue
  depth: number
  maxDepth: number
  isLast?: boolean
  isArrayItem?: boolean
  arrayIndex?: number
  path: string[]
  searchTerm: string
  searchResults: SearchResult[]
  currentResultIndex: number
  globalExpanded: boolean
  onSearchTermChange: (term: string) => void
  onNavigateToId: (id: string) => void
  productId?: string
  onHistoryOpen?: (keyPath: string, keyName: string, currentValue: string, iconElement: HTMLElement) => void
}

type SearchResult = {
  path: string[]
  type: "key" | "value"
  matchedText: string
  fullKey: string
  fullValue: string
}

function getTypeIcon(value: JsonValue): { icon: ReactNode; color: string; bgColor: string } {
  if (value === null)
    return {
      icon: <XCircle className="h-3 w-3" />,
      color: "text-gray-500",
      bgColor: "bg-gray-50",
    }

  if (typeof value === "boolean")
    return {
      icon: <CheckCircle className="h-3 w-3" />,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
    }

  if (typeof value === "number")
    return {
      icon: <Hash className="h-3 w-3" />,
      color: "text-purple-600",
      bgColor: "bg-purple-50",
    }

  if (typeof value === "string") {
    if (/^\d{2}\.\d{2}\.\d{4}$|^\d{4}-\d{2}-\d{2}$|^\d{2}\/\d{4}$/.test(value)) {
      return {
        icon: <Calendar className="h-3 w-3" />,
        color: "text-green-600",
        bgColor: "bg-green-50",
      }
    }
    return {
      icon: <span className="text-xs font-bold">T</span>,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
    }
  }

  if (Array.isArray(value))
    return {
      icon: <Brackets className="h-3 w-3" />,
      color: "text-indigo-600",
      bgColor: "bg-indigo-50",
    }

  return {
    icon: <Braces className="h-3 w-3" />,
    color: "text-slate-600",
    bgColor: "bg-slate-50",
  }
}

function getValueTypeLabel(value: JsonValue): string {
  if (value === null) return "null"
  if (Array.isArray(value)) return "Array"
  if (typeof value === "object") return "Object"
  return typeof value
}

function formatValue(value: JsonValue): string {
  if (value === null) return "null"
  if (typeof value === "boolean") return value ? "true" : "false"
  if (typeof value === "string") return value
  if (typeof value === "number") return value.toLocaleString()
  return ""
}

// Neue Funktion zur Erkennung von URLs und IDs
function isUrl(text: string): boolean {
  return /^https?:\/\/.+/i.test(text)
}

function isProductId(text: string): boolean {
  return /^(MED-|HERSTELLER-|UNIT-).+/i.test(text)
}

function searchInJson(data: JsonValue, searchTerm: string): SearchResult[] {
  const results: SearchResult[] = []

  if (!searchTerm.trim()) return results

  const searchLower = searchTerm.toLowerCase()

  // Check if search term contains key:value pattern
  const keyValueMatch = searchTerm.match(/^(.+?):(.+)$/)

  if (keyValueMatch) {
    const [, searchKey, searchValue] = keyValueMatch
    const searchKeyLower = searchKey.trim().toLowerCase()
    const searchValueLower = searchValue.trim().toLowerCase()

    function searchKeyValue(obj: JsonValue, path: string[]) {
      if (typeof obj === "object" && obj !== null && !Array.isArray(obj)) {
        Object.entries(obj as JsonObject).forEach(([key, value]) => {
          const newPath = [...path, key]

          // Check if key matches and value matches
          if (key.toLowerCase().includes(searchKeyLower)) {
            const valueStr = formatValue(value).toLowerCase()
            if (valueStr.includes(searchValueLower)) {
              results.push({
                path: newPath,
                type: "key",
                matchedText: `${key}:${formatValue(value)}`,
                fullKey: key,
                fullValue: formatValue(value),
              })
            }
          }

          // Recurse into nested objects
          if (typeof value === "object" && value !== null) {
            searchKeyValue(value, newPath)
          }
        })
      } else if (Array.isArray(obj)) {
        obj.forEach((item, index) => {
          searchKeyValue(item, [...path, index.toString()])
        })
      }
    }

    searchKeyValue(data, [])
  } else {
    // Regular search in both keys and values
    function searchRecursive(obj: JsonValue, path: string[]) {
      if (typeof obj === "object" && obj !== null && !Array.isArray(obj)) {
        Object.entries(obj as JsonObject).forEach(([key, value]) => {
          const newPath = [...path, key]

          // Search in key
          if (key.toLowerCase().includes(searchLower)) {
            results.push({
              path: newPath,
              type: "key",
              matchedText: key,
              fullKey: key,
              fullValue: formatValue(value),
            })
          }

          // Search in value
          const valueStr = formatValue(value)
          if (valueStr.toLowerCase().includes(searchLower)) {
            results.push({
              path: newPath,
              type: "value",
              matchedText: valueStr,
              fullKey: key,
              fullValue: valueStr,
            })
          }

          // Recurse into nested objects
          if (typeof value === "object" && value !== null) {
            searchRecursive(value, newPath)
          }
        })
      } else if (Array.isArray(obj)) {
        obj.forEach((item, index) => {
          searchRecursive(item, [...path, index.toString()])
        })
      }
    }

    searchRecursive(data, [])
  }

  return results
}

function JsonItem({
  keyName,
  value,
  depth,
  maxDepth,
  isLast = false,
  isArrayItem = false,
  arrayIndex,
  path,
  searchTerm,
  searchResults,
  currentResultIndex,
  globalExpanded,
  onSearchTermChange,
  onNavigateToId,
  productId,
  onHistoryOpen,
}: JsonItemProps) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [isExpanded, setIsExpanded] = useState(depth < 2)
  const itemRef = useRef<HTMLDivElement>(null)

  const isExpandable = (typeof value === "object" && value !== null) || Array.isArray(value)
  const hasContent =
    isExpandable &&
    ((Array.isArray(value) && value.length > 0) ||
      (typeof value === "object" && Object.keys(value as JsonObject).length > 0))

  const typeInfo = getTypeIcon(value)
  const displayKey = isArrayItem ? `[${arrayIndex}]` : keyName
  const currentPath = [...path, isArrayItem ? arrayIndex!.toString() : keyName]
  const pathString = currentPath.join(".")

  // Check if this item matches search results
  const matchingResults = searchResults.filter((result) => result.path.join(".") === pathString)

  const isCurrentSearchResult = matchingResults.some(
    (_, index) => searchResults.indexOf(matchingResults[index]) === currentResultIndex,
  )

  const hasSearchMatch = matchingResults.length > 0

  // Update expansion based on global state
  useEffect(() => {
    if (hasContent) {
      setIsExpanded(globalExpanded)
    }
  }, [globalExpanded, hasContent])

  // Auto-expand if this path contains search results
  useEffect(() => {
    if (hasSearchMatch && searchTerm) {
      setIsExpanded(true)
    }
  }, [hasSearchMatch, searchTerm])

  // Scroll to current search result
  useEffect(() => {
    if (isCurrentSearchResult && itemRef.current) {
      itemRef.current.scrollIntoView({
        behavior: "smooth",
        block: "center",
      })
    }
  }, [isCurrentSearchResult])

  if (depth >= maxDepth) {
    return (
      <div className="flex items-center gap-3 py-2 px-4 bg-gray-50 rounded-lg border border-gray-200">
        <div className="text-sm text-gray-500">... (Maximum depth reached)</div>
      </div>
    )
  }

  const highlightText = (text: string, isKey: boolean) => {
    if (!searchTerm || !hasSearchMatch) return text

    const matchingResult = matchingResults.find((r) => (isKey && r.type === "key") || (!isKey && r.type === "value"))

    if (!matchingResult) return text

    // Handle key:value search pattern
    const keyValueMatch = searchTerm.match(/^(.+?):(.+)$/)
    if (keyValueMatch) {
      const [, searchKey, searchValue] = keyValueMatch
      const searchPattern = isKey ? searchKey.trim() : searchValue.trim()
      const regex = new RegExp(`(${searchPattern.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi")

      return text.split(regex).map((part, index) =>
        regex.test(part) ? (
          <mark key={index} className="bg-yellow-200 px-1 rounded">
            {part}
          </mark>
        ) : (
          part
        ),
      )
    } else {
      // Regular search highlighting
      const regex = new RegExp(`(${searchTerm.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi")
      return text.split(regex).map((part, index) =>
        regex.test(part) ? (
          <mark key={index} className="bg-yellow-200 px-1 rounded">
            {part}
          </mark>
        ) : (
          part
        ),
      )
    }
  }

  // History Button Handler - now passes the button element for positioning
  const handleHistoryClick = (key: string, value: string, event: React.MouseEvent<HTMLButtonElement>) => {
    const keyPath = buildKeyIdentifier(path, key)
    const buttonElement = event.currentTarget
    onHistoryOpen?.(keyPath, key, formatValue(value), buttonElement)
  }

  // Neue Funktion für interaktive Werte mit History-Button - jetzt für alle Datentypen
  const renderInteractiveValue = (text: string) => {
    const formattedText = formatValue(value)

    // URL-Erkennung
    if (isUrl(formattedText)) {
      return (
        <div className="flex items-center gap-2 group">
          <a
            href={formattedText}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 hover:underline transition-colors group"
            title={`Öffne ${formattedText}`}
          >
            <span>{highlightText(formattedText, false)}</span>
            <ExternalLink className="h-3 w-3 opacity-60 group-hover:opacity-100 transition-opacity" />
          </a>
          <Button
            onClick={(e) => handleHistoryClick(keyName, formattedText, e)}
            variant="ghost"
            size="sm"
            className="h-5 w-5 p-0 opacity-0 group-hover:opacity-100 hover:bg-gray-100 rounded transition-all duration-200"
            title={`Historie für ${keyName}`}
          >
            <History className="h-2.5 w-2.5 text-gray-400 hover:text-gray-600" />
          </Button>
        </div>
      )
    }

    // Produkt-ID-Erkennung - Navigation ohne type Parameter
    if (isProductId(formattedText)) {
      return (
        <div className="flex items-center gap-2 group">
          <button
            onClick={() => onNavigateToId(formattedText)}
            className="inline-flex items-center gap-1 text-emerald-600 hover:text-emerald-800 hover:underline transition-colors group font-mono"
            title={`Navigiere zu ${formattedText}`}
          >
            <span>{highlightText(formattedText, false)}</span>
            <MousePointer className="h-3 w-3 opacity-60 group-hover:opacity-100 transition-opacity" />
          </button>
          <Button
            onClick={(e) => handleHistoryClick(keyName, formattedText, e)}
            variant="ghost"
            size="sm"
            className="h-5 w-5 p-0 opacity-0 group-hover:opacity-100 hover:bg-gray-100 rounded transition-all duration-200"
            title={`Historie für ${keyName}`}
          >
            <History className="h-2.5 w-2.5 text-gray-400 hover:text-gray-600" />
          </Button>
        </div>
      )
    }

    // Standard-Text mit Highlighting und History-Button für ALLE Datentypen
    return (
      <div className="flex items-center gap-2 group">
        <span>{highlightText(formattedText, false)}</span>
        <Button
          onClick={(e) => handleHistoryClick(keyName, formattedText, e)}
          variant="ghost"
          size="sm"
          className="h-5 w-5 p-0 opacity-0 group-hover:opacity-100 hover:bg-gray-100 rounded transition-all duration-200"
          title={`Historie für ${keyName}`}
        >
          <History className="h-2.5 w-2.5 text-gray-400 hover:text-gray-600" />
        </Button>
      </div>
    )
  }

  return (
    <div ref={itemRef} className={cn("relative", depth > 0 && "ml-6")}>
      {/* Connection lines */}
      {depth > 0 && (
        <>
          <div className="absolute -left-6 top-0 bottom-0 w-px bg-gray-200" />
          <div className="absolute -left-6 top-6 w-4 h-px bg-gray-200" />
          {!isLast && <div className="absolute -left-6 top-12 bottom-0 w-px bg-gray-200" />}
        </>
      )}

      <div
        className={cn(
          "rounded-lg border shadow-sm hover:shadow-md transition-all duration-200",
          depth === 0 && "mb-2",
          depth > 0 && "mb-1.5",
          isCurrentSearchResult
            ? "border-emerald-500 bg-emerald-50 ring-2 ring-emerald-200"
            : hasSearchMatch
              ? "border-emerald-300 bg-emerald-25"
              : "border-gray-200 bg-white",
        )}
      >
        <div className="p-2 md:p-3">
          <div className="flex items-start gap-2">
            {/* Expand/Collapse Button */}
            {isExpandable && hasContent ? (
              <button
                onClick={() => setIsExpanded(!isExpanded)}
                className="flex-shrink-0 p-0.5 hover:bg-gray-100 rounded-md transition-colors mt-0.5"
              >
                {isExpanded ? (
                  <ChevronDown className="h-3 w-3 text-gray-600" />
                ) : (
                  <ChevronRight className="h-3 w-3 text-gray-600" />
                )}
              </button>
            ) : (
              <div className="w-4 h-4 flex-shrink-0" />
            )}

            {/* Type Icon */}
            <div
              className={cn(
                "flex-shrink-0 w-4 h-4 rounded-md flex items-center justify-center mt-0.5",
                typeInfo.bgColor,
              )}
            >
              <div className={typeInfo.color}>{typeInfo.icon}</div>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-1.5 mb-1">
                <span className="font-semibold text-gray-900 text-xs">{highlightText(displayKey, true)}</span>
                <Badge
                  variant="secondary"
                  className="text-xs px-1 py-0 h-auto bg-gray-100 text-gray-600 hover:bg-gray-200"
                >
                  {getValueTypeLabel(value)}
                </Badge>
              </div>

              {/* Value display for primitives */}
              {!isExpandable && (
                <div className="text-gray-800 font-medium break-words text-sm">
                  {renderInteractiveValue(formatValue(value))}
                </div>
              )}

              {/* Preview for collapsed complex values */}
              {isExpandable && !isExpanded && hasContent && (
                <div className="text-sm text-gray-500">
                  {Array.isArray(value)
                    ? `${value.length} ${value.length === 1 ? "item" : "items"}`
                    : `${Object.keys(value as JsonObject).length} ${Object.keys(value as JsonObject).length === 1 ? "property" : "properties"}`}
                </div>
              )}
            </div>
          </div>

          {/* Expanded content */}
          {isExpandable && isExpanded && hasContent && (
            <div className="mt-2 pl-6">
              <div className="space-y-2">
                {Array.isArray(value)
                  ? // Array rendering
                    value.map((item, index) => (
                      <JsonItem
                        key={`${keyName}[${index}]`}
                        keyName={`item`}
                        value={item}
                        depth={depth + 1}
                        maxDepth={maxDepth}
                        isLast={index === value.length - 1}
                        isArrayItem={true}
                        arrayIndex={index}
                        path={currentPath}
                        searchTerm={searchTerm}
                        searchResults={searchResults}
                        currentResultIndex={currentResultIndex}
                        globalExpanded={globalExpanded}
                        onSearchTermChange={onSearchTermChange}
                        onNavigateToId={onNavigateToId}
                        productId={productId}
                        onHistoryOpen={onHistoryOpen}
                      />
                    ))
                  : // Object rendering
                    Object.entries(value as JsonObject).map(([subKey, subValue], index, array) => (
                      <JsonItem
                        key={subKey}
                        keyName={subKey}
                        value={subValue}
                        depth={depth + 1}
                        maxDepth={maxDepth}
                        isLast={index === array.length - 1}
                        path={currentPath}
                        searchTerm={searchTerm}
                        searchResults={searchResults}
                        currentResultIndex={currentResultIndex}
                        globalExpanded={globalExpanded}
                        onSearchTermChange={onSearchTermChange}
                        onNavigateToId={onNavigateToId}
                        productId={productId}
                        onHistoryOpen={onHistoryOpen}
                      />
                    ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function OperationsControls({
  globalExpanded,
  onExpandAll,
  onCollapseAll,
  searchTerm,
  onSearchChange,
  onClearSearch,
  searchResults,
  currentResultIndex,
  onNavigateSearch,
}: {
  globalExpanded: boolean
  onExpandAll: () => void
  onCollapseAll: () => void
  searchTerm: string
  onSearchChange: (value: string) => void
  onClearSearch: () => void
  searchResults: SearchResult[]
  currentResultIndex: number
  onNavigateSearch: (direction: "next" | "prev") => void
}) {
  return (
    <div className="flex items-center gap-2 p-2 bg-gray-50 rounded-lg border">
      {/* Navigation Controls */}
      <div className="flex items-center gap-1">
        <Button
          onClick={onCollapseAll}
          variant="outline"
          size="sm"
          className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent"
          title="Alle einklappen"
        >
          <ChevronUp className="h-4 w-4" />
        </Button>

        <Button
          onClick={onExpandAll}
          variant="outline"
          size="sm"
          className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent"
          title="Alle ausklappen"
        >
          <ChevronDown className="h-4 w-4" />
        </Button>
      </div>

      <div className="h-4 w-px bg-gray-300" />

      {/* Search Controls */}
      <div className="flex items-center gap-2">
        <div className="relative">
          <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3 w-3 text-gray-400" />
          <Input
            type="text"
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Suche: 'key:value' oder 'text'"
            className="pl-7 pr-7 h-8 w-80 text-sm"
          />
          {searchTerm && (
            <Button
              onClick={onClearSearch}
              variant="ghost"
              size="sm"
              className="absolute right-1 top-1/2 -translate-y-1/2 h-5 w-5 p-0"
              title="Suche löschen"
            >
              <X className="h-2 w-2" />
            </Button>
          )}
        </div>

        {searchResults.length > 0 && (
          <div className="flex items-center gap-1">
            <span
              className="text-xs text-gray-600 px-2"
              title={`Suchergebnis ${currentResultIndex + 1} von ${searchResults.length}`}
            >
              {currentResultIndex + 1}/{searchResults.length}
            </span>
            <Button
              onClick={() => onNavigateSearch("prev")}
              variant="outline"
              size="sm"
              className="h-6 w-6 p-0"
              title="Vorheriges Suchergebnis"
            >
              <ArrowUp className="h-2 w-2" />
            </Button>
            <Button
              onClick={() => onNavigateSearch("next")}
              variant="outline"
              size="sm"
              className="h-6 w-6 p-0"
              title="Nächstes Suchergebnis"
            >
              <ArrowDown className="h-2 w-2" />
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}

export function EnhancedJsonDisplay({
  data,
  maxDepth = 6,
  globalExpanded,
  searchTerm,
  searchResults,
  currentResultIndex,
  onSearchResultsChange,
  onSearchTermChange,
  productId,
  onHistoryOpen,
}: EnhancedJsonDisplayProps & {
  onHistoryOpen?: (keyPath: string, keyName: string, currentValue: string, iconElement: HTMLElement) => void
}) {
  const router = useRouter()

  // Update search results when search term changes
  useEffect(() => {
    if (searchTerm.trim()) {
      const results = searchInJson(data, searchTerm)
      onSearchResultsChange(results)
    } else {
      onSearchResultsChange([])
    }
  }, [searchTerm, data])

  // Navigation zu einer bestimmten ID - ohne type Parameter
  const handleNavigateToId = (id: string) => {
    router.push(`/${id}`)
  }

  return (
    <div className="space-y-4">
      {/* JSON Display */}
      <div className="space-y-4">
        {typeof data === "object" && data !== null ? (
          Array.isArray(data) ? (
            // Root is array
            data.map((item, index) => (
              <JsonItem
                key={`root-${index}`}
                keyName="item"
                value={item}
                depth={0}
                maxDepth={maxDepth}
                isLast={index === data.length - 1}
                isArrayItem={true}
                arrayIndex={index}
                path={[]}
                searchTerm={searchTerm}
                searchResults={searchResults}
                currentResultIndex={currentResultIndex}
                globalExpanded={globalExpanded}
                onSearchTermChange={onSearchTermChange}
                onNavigateToId={handleNavigateToId}
                productId={productId}
                onHistoryOpen={onHistoryOpen}
              />
            ))
          ) : (
            // Root is object
            Object.entries(data as JsonObject).map(([key, value], index, array) => (
              <JsonItem
                key={key}
                keyName={key}
                value={value}
                depth={0}
                maxDepth={maxDepth}
                isLast={index === array.length - 1}
                path={[]}
                searchTerm={searchTerm}
                searchResults={searchResults}
                currentResultIndex={currentResultIndex}
                globalExpanded={globalExpanded}
                onSearchTermChange={onSearchTermChange}
                onNavigateToId={handleNavigateToId}
                productId={productId}
                onHistoryOpen={onHistoryOpen}
              />
            ))
          )
        ) : (
          // Root is primitive
          <JsonItem
            keyName="value"
            value={data}
            depth={0}
            maxDepth={maxDepth}
            path={[]}
            searchTerm={searchTerm}
            searchResults={searchResults}
            currentResultIndex={currentResultIndex}
            globalExpanded={globalExpanded}
            onSearchTermChange={onSearchTermChange}
            onNavigateToId={handleNavigateToId}
            productId={productId}
            onHistoryOpen={onHistoryOpen}
          />
        )}
      </div>
    </div>
  )
}

// Export the OperationsControls for use in PageHeader
export { OperationsControls }

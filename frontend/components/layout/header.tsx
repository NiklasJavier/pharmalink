"use client"

import type React from "react"
import { useState } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { Search, Building, Pill, Box, ChevronDown, Copy, Check } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { PassportTabs, type Tab } from "@/components/passport/passport-tabs"

const searchTypes = {
  hersteller: { label: "Hersteller", icon: <Building className="mr-2 h-4 w-4" />, prefix: "HERSTELLER-" },
  medikament: { label: "Medikament", icon: <Pill className="mr-2 h-4 w-4" />, prefix: "MED-" },
  unit: { label: "Unit", icon: <Box className="mr-2 h-4 w-4" />, prefix: "UNIT-" },
}

type HeaderProps = {
  activeTab?: Tab
  onTabChange?: (tab: Tab) => void
  showTabs?: boolean
  linkedIds?: {
    medikament?: string
    hersteller?: string
    unit?: string[]
  }
  onSearch?: (query: string, type: string) => void
  // Neue Props für Produktinformationen
  productId?: string
  originalId?: string
  correctedId?: string
}

// Funktion zur Erkennung von Präfixen im Suchterm
function detectPrefixInQuery(query: string): {
  hasPrefix: boolean
  detectedType?: keyof typeof searchTypes
  cleanQuery: string
} {
  const trimmedQuery = query.trim()

  if (trimmedQuery.startsWith("HERSTELLER-")) {
    return { hasPrefix: true, detectedType: "hersteller", cleanQuery: trimmedQuery }
  }
  if (trimmedQuery.startsWith("MED-")) {
    return { hasPrefix: true, detectedType: "medikament", cleanQuery: trimmedQuery }
  }
  if (trimmedQuery.startsWith("UNIT-")) {
    return { hasPrefix: true, detectedType: "unit", cleanQuery: trimmedQuery }
  }

  return { hasPrefix: false, cleanQuery: trimmedQuery }
}

// Funktion zur Bestimmung der finalen Suche
function buildFinalSearch(query: string, selectedType: keyof typeof searchTypes): string {
  const detection = detectPrefixInQuery(query)

  if (detection.hasPrefix) {
    // Präfix im Suchterm vorhanden - verwende direkt
    return detection.cleanQuery
  } else {
    // Kein Präfix im Suchterm → Dropdown-Präfix hinzufügen
    const prefix = searchTypes[selectedType].prefix
    return `${prefix}${detection.cleanQuery}`
  }
}

// Funktion für Produktinformationen
function getProductInfo(id: string) {
  if (id.startsWith("MED-")) {
    return {
      title: "Medikament",
      subtitle: "Medikamenten-ID:",
      icon: <Pill className="h-3 w-3 text-black" />,
    }
  } else if (id.startsWith("HERSTELLER-")) {
    return {
      title: "Hersteller",
      subtitle: "Hersteller-ID:",
      icon: <Building className="h-3 w-3 text-black" />,
    }
  } else if (id.startsWith("UNIT-")) {
    return {
      title: "Unit",
      subtitle: "Unit-ID:",
      icon: <Box className="h-3 w-3 text-black" />,
    }
  } else {
    return {
      title: "Unbekannt",
      subtitle: "ID:",
      icon: <Box className="h-3 w-3 text-black" />,
    }
  }
}

export function Header({
  activeTab,
  onTabChange,
  showTabs = false,
  linkedIds,
  onSearch,
  productId,
  originalId,
  correctedId,
}: HeaderProps = {}) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [copied, setCopied] = useState(false)

  const [query, setQuery] = useState("")
  const [type, setType] = useState<keyof typeof searchTypes>("hersteller")

  const handleSearch = (searchQuery: string, searchType: string) => {
    const finalQuery = buildFinalSearch(searchQuery, searchType as keyof typeof searchTypes)
    // Nur der Pfad, keine Query-Parameter
    router.push(`/${finalQuery}`)
  }

  const handleFormSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (!query.trim()) {
      // Wenn leer, zur Homepage navigieren
      router.push("/")
      return
    }

    const finalQuery = buildFinalSearch(query, type)
    // Nur der Pfad, keine Query-Parameter - konsistent mit anderen Suchfunktionen
    router.push(`/${finalQuery}`)
  }

  const copyToClipboard = async () => {
    try {
      const urlToCopy = window.location.href
      await navigator.clipboard.writeText(urlToCopy)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error("Failed to copy: ", err)
    }
  }

  const displayId = correctedId || productId
  const productInfo = displayId ? getProductInfo(displayId) : null

  return (
    <>
      {/* Sticky Navigation Header mit verstärktem Glasmorphismus - kompakter */}
      {showTabs && activeTab && onTabChange ? (
        <div className="sticky top-0 z-30 py-2 bg-gray-50/30 backdrop-blur-xl border-b border-white/20">
          {/* Zusätzlicher Blur-Overlay für bessere Trennung */}
          <div className="absolute inset-0 bg-gradient-to-b from-gray-50/60 via-gray-50/40 to-gray-50/20 backdrop-blur-2xl"></div>

          <div className="relative w-full max-w-7xl mx-auto px-4 md:px-8">
            <div className="flex items-center justify-between">
              {/* Left side - Product Information mit verstärktem Glaseffekt - kompakter */}
              {productInfo && (
                <div className="flex items-center gap-2 bg-white/80 backdrop-blur-xl border border-white/40 rounded-lg shadow-2xl px-3 py-2 ring-1 ring-black/5">
                  {/* Innerer Glanz-Effekt */}
                  <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>

                  <div className="relative flex items-center gap-2">
                    <div className="flex-shrink-0">{productInfo.icon}</div>
                    <h1 className="text-sm font-bold text-black drop-shadow-sm">{productInfo.title}</h1>
                    <Button
                      onClick={copyToClipboard}
                      variant="ghost"
                      size="sm"
                      className="h-auto p-0.5 hover:bg-white/60 rounded-md transition-all duration-200 group backdrop-blur-sm"
                      title="Vollständige URL mit allen Parametern kopieren"
                    >
                      <span className="font-mono text-gray-800 group-hover:text-emerald-600 transition-colors text-xs font-semibold drop-shadow-sm">
                        {displayId}
                      </span>
                      <div className="ml-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                        {copied ? (
                          <Check className="h-3 w-3 text-emerald-600 drop-shadow-sm" />
                        ) : (
                          <Copy className="h-3 w-3 text-gray-500" />
                        )}
                      </div>
                    </Button>

                    {/* Zeige ursprüngliche ID falls korrigiert - in derselben Zeile */}
                    {originalId && originalId !== displayId && (
                      <div className="text-xs text-gray-600">
                        (urspr.: <code className="bg-white/60 px-1 rounded backdrop-blur-sm text-xs">{originalId}</code>
                        )
                      </div>
                    )}

                    {/* Kopiert-Bestätigung - in derselben Zeile */}
                    {copied && (
                      <div className="text-xs text-emerald-600 font-medium animate-in fade-in duration-200 drop-shadow-sm">
                        ✓ Kopiert!
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Right side - Passport Tabs mit verstärktem Glaseffekt */}
              <div className="relative">
                <PassportTabs
                  activeTab={activeTab}
                  onTabChange={onTabChange}
                  compact={true}
                  onSearch={onSearch || handleSearch}
                  linkedIds={linkedIds}
                />
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* Fallback Search Bar für Seiten ohne Tabs mit Glaseffekt - kompakter */
        <div className="sticky top-0 z-30 bg-gray-50/30 backdrop-blur-xl border-b border-white/20 py-2">
          {/* Zusätzlicher Blur-Overlay */}
          <div className="absolute inset-0 bg-gradient-to-b from-gray-50/60 via-gray-50/40 to-gray-50/20 backdrop-blur-2xl"></div>

          <div className="relative flex justify-end px-4 md:px-6 lg:px-8">
            <form
              onSubmit={handleFormSearch}
              className="flex w-full max-w-sm md:max-w-lg items-center rounded-lg border border-white/40 bg-white/80 backdrop-blur-xl shadow-2xl focus-within:ring-2 focus-within:ring-emerald-400/50 transition-all duration-300 ring-1 ring-black/5"
            >
              {/* Innerer Glanz-Effekt */}
              <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>

              <div className="relative flex w-full">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      className="flex h-8 flex-shrink-0 items-center gap-1 rounded-l-lg px-2 text-xs font-normal text-gray-700 hover:bg-white/60 backdrop-blur-sm"
                    >
                      {searchTypes[type].icon}
                      <span className="hidden sm:inline-block">{searchTypes[type].label}</span>
                      <ChevronDown className="h-3 w-3" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56 bg-white/95 backdrop-blur-xl border-white/40" align="start">
                    {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                      <DropdownMenuItem
                        key={key}
                        className="text-sm py-2 hover:bg-white/60"
                        onSelect={() => setType(key as keyof typeof searchTypes)}
                      >
                        {icon}
                        <span>{label}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>

                <div className="h-4 w-px bg-gray-300/50" />

                <div className="relative flex flex-grow items-center">
                  <Search className="absolute left-2 h-3 w-3 text-gray-500" />
                  <Input
                    type="search"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Suche nach Code oder ID..."
                    className="h-8 w-full rounded-r-lg border-none bg-transparent pl-7 text-xs placeholder:text-gray-500 focus:ring-0 focus-visible:ring-offset-0"
                  />
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}

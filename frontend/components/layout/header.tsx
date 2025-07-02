"use client"

import type React from "react"
import { useRouter } from "next/navigation"
import { Search, Building, Pill, Box, ChevronDown } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { PassportTabs, type Tab } from "@/components/passport/passport-tabs"
import { useState, useEffect, useRef } from "react"

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
    return detection.cleanQuery
  } else {
    const prefix = searchTypes[selectedType].prefix
    return `${prefix}${detection.cleanQuery}`
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
  const [query, setQuery] = useState("")
  const [type, setType] = useState<keyof typeof searchTypes>("hersteller")
  const [isMounted, setIsMounted] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  // Verhindere Hydration-Probleme
  useEffect(() => {
    setIsMounted(true)
  }, [])

  const handleSearch = (searchQuery: string, searchType: string) => {
    const finalQuery = buildFinalSearch(searchQuery, searchType as keyof typeof searchTypes)
    router.push(`/${finalQuery}`)
  }

  const executeSearch = () => {
    if (!query.trim()) {
      router.push("/")
      return
    }

    const finalQuery = buildFinalSearch(query, type)
    router.push(`/${finalQuery}`)
  }

  const handleFormSearch = (e: React.FormEvent) => {
    e.preventDefault()
    executeSearch()
  }

  const handleSearchButtonClick = () => {
    executeSearch()
  }

  // Einfache Input-Änderung ohne automatische Suche
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value)
  }

  const handleTypeChange = (newType: keyof typeof searchTypes) => {
    setType(newType)
    // Fokus zurück auf Input nach Typ-Änderung
    setTimeout(() => {
      inputRef.current?.focus()
    }, 100)
  }

  // Placeholder für PassportTabs um Layout-Shift zu verhindern
  const TabsPlaceholder = () => (
      <div className="flex items-center bg-white/80 backdrop-blur-xl border border-white/40 rounded-xl shadow-2xl p-1 ring-1 ring-black/5 opacity-0">
        <div className="flex items-center gap-1">
          {/* Placeholder für 3 Tabs mit Separatoren */}
          <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
          <div className="w-5 h-5 rounded-full bg-gray-200/50"></div>
          <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
          <div className="w-5 h-5 rounded-full bg-gray-200/50"></div>
          <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
        </div>
      </div>
  )

  return (
      <div className="sticky top-0 z-30 py-2 bg-gray-50/30 backdrop-blur-xl border-b border-white/20">
        <div className="absolute inset-0 bg-gradient-to-b from-gray-50/60 via-gray-50/40 to-gray-50/20 backdrop-blur-2xl"></div>

        <div className="relative w-full max-w-7xl mx-auto px-4 md:px-8">
          {/* Desktop Layout: Horizontal */}
          <div className="hidden md:flex items-center justify-between">
            {/* Left side - Passport Tabs oder Placeholder */}
            <div className="relative">
              {showTabs ? (
                  isMounted && activeTab && onTabChange ? (
                      <div className="animate-in fade-in duration-200">
                        <PassportTabs
                            activeTab={activeTab}
                            onTabChange={onTabChange}
                            compact={true}
                            onSearch={onSearch || handleSearch}
                            linkedIds={linkedIds}
                        />
                      </div>
                  ) : (
                      <TabsPlaceholder />
                  )
              ) : (
                  <div className="w-0"></div>
              )}
            </div>

            {/* Right side - Search Bar - DIREKT INLINE */}
            <div className="relative ml-auto">
              <form
                  onSubmit={handleFormSearch}
                  className="flex w-full max-w-sm md:max-w-lg items-center rounded-lg border border-white/40 bg-white/80 backdrop-blur-xl shadow-2xl focus-within:ring-2 focus-within:ring-emerald-400/50 transition-all duration-300 ring-1 ring-black/5"
              >
                <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>

                <div className="relative flex w-full">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                          type="button"
                          variant="ghost"
                          className="flex h-9 flex-shrink-0 items-center gap-1 rounded-l-lg px-3 text-sm font-medium text-emerald-600 hover:bg-white/60 hover:text-emerald-700 backdrop-blur-sm transition-colors"
                      >
                        <div className="text-emerald-600">{searchTypes[type].icon}</div>
                        <span className="hidden sm:inline-block text-emerald-600">{searchTypes[type].label}</span>
                        <ChevronDown className="h-3 w-3 text-emerald-600" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-56 bg-white/95 backdrop-blur-xl border-white/40" align="start">
                      {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                          <DropdownMenuItem
                              key={key}
                              className="text-sm py-2 hover:bg-white/60"
                              onSelect={() => handleTypeChange(key as keyof typeof searchTypes)}
                          >
                            {icon}
                            <span>{label}</span>
                          </DropdownMenuItem>
                      ))}
                    </DropdownMenuContent>
                  </DropdownMenu>

                  <div className="h-5 w-px bg-gray-300/50" />

                  <div className="relative flex flex-grow items-center">
                    <Input
                        ref={inputRef}
                        type="text"
                        value={query}
                        onChange={handleInputChange}
                        placeholder="Suche nach Code oder ID..."
                        className="h-9 w-full border-none bg-transparent pl-3 pr-10 text-sm placeholder:text-gray-500 focus:ring-0 focus-visible:ring-offset-0 rounded-none"
                        autoComplete="off"
                        spellCheck="false"
                    />

                    {/* Such-Button */}
                    <Button
                        type="submit"
                        onClick={handleSearchButtonClick}
                        variant="ghost"
                        size="sm"
                        className="absolute right-1 h-7 w-7 p-0 hover:bg-emerald-100 text-emerald-600 hover:text-emerald-700 rounded-md transition-colors"
                        title="Suche starten (Enter)"
                    >
                      <Search className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </form>
            </div>
          </div>

          {/* Mobile Layout: Vertical Stack */}
          <div className="md:hidden space-y-3">
            {/* Passport Tabs oben */}
            {showTabs && (
                <div className="flex justify-start">
                  {isMounted && activeTab && onTabChange ? (
                      <div className="animate-in fade-in duration-200">
                        <PassportTabs
                            activeTab={activeTab}
                            onTabChange={onTabChange}
                            compact={true}
                            onSearch={onSearch || handleSearch}
                            linkedIds={linkedIds}
                        />
                      </div>
                  ) : (
                      <TabsPlaceholder />
                  )}
                </div>
            )}

            {/* Suchleiste darunter - DIREKT INLINE */}
            <div className="flex justify-start">
              <form
                  onSubmit={handleFormSearch}
                  className="flex w-full max-w-sm md:max-w-lg items-center rounded-lg border border-white/40 bg-white/80 backdrop-blur-xl shadow-2xl focus-within:ring-2 focus-within:ring-emerald-400/50 transition-all duration-300 ring-1 ring-black/5"
              >
                <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>

                <div className="relative flex w-full">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                          type="button"
                          variant="ghost"
                          className="flex h-9 flex-shrink-0 items-center gap-1 rounded-l-lg px-3 text-sm font-medium text-emerald-600 hover:bg-white/60 hover:text-emerald-700 backdrop-blur-sm transition-colors"
                      >
                        <div className="text-emerald-600">{searchTypes[type].icon}</div>
                        <span className="hidden sm:inline-block text-emerald-600">{searchTypes[type].label}</span>
                        <ChevronDown className="h-3 w-3 text-emerald-600" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-56 bg-white/95 backdrop-blur-xl border-white/40" align="start">
                      {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                          <DropdownMenuItem
                              key={key}
                              className="text-sm py-2 hover:bg-white/60"
                              onSelect={() => handleTypeChange(key as keyof typeof searchTypes)}
                          >
                            {icon}
                            <span>{label}</span>
                          </DropdownMenuItem>
                      ))}
                    </DropdownMenuContent>
                  </DropdownMenu>

                  <div className="h-5 w-px bg-gray-300/50" />

                  <div className="relative flex flex-grow items-center">
                    <Input
                        ref={inputRef}
                        type="text"
                        value={query}
                        onChange={handleInputChange}
                        placeholder="Suche nach Code oder ID..."
                        className="h-9 w-full border-none bg-transparent pl-3 pr-10 text-sm placeholder:text-gray-500 focus:ring-0 focus-visible:ring-offset-0 rounded-none"
                        autoComplete="off"
                        spellCheck="false"
                    />

                    {/* Such-Button */}
                    <Button
                        type="submit"
                        onClick={handleSearchButtonClick}
                        variant="ghost"
                        size="sm"
                        className="absolute right-1 h-7 w-7 p-0 hover:bg-emerald-100 text-emerald-600 hover:text-emerald-700 rounded-md transition-colors"
                        title="Suche starten (Enter)"
                    >
                      <Search className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
  )
}

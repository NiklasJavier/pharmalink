"use client"
import { useRouter } from "next/navigation"
import { Building, Pill, Box, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { PassportTabs, type Tab } from "@/components/passport/passport-tabs"
import { EnhancedSearchInput } from "@/components/search/enhanced-search-input"
import { useState, useEffect } from "react"

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

function buildFinalSearch(query: string, selectedType: keyof typeof searchTypes): string {
  const detection = detectPrefixInQuery(query)

  if (detection.hasPrefix) {
    return detection.cleanQuery
  } else {
    const prefix = searchTypes[selectedType].prefix
    return `${prefix}${detection.cleanQuery}`
  }
}

function getSearchTypeFromProductId(productId?: string): keyof typeof searchTypes {
  if (!productId) return "medikament"

  if (productId.startsWith("MED-")) return "medikament"
  if (productId.startsWith("HERSTELLER-")) return "hersteller"
  if (productId.startsWith("UNIT-")) return "unit"

  return "medikament"
}

function removePrefix(productId: string): string {
  if (productId.startsWith("MED-")) return productId.replace("MED-", "")
  if (productId.startsWith("HERSTELLER-")) return productId.replace("HERSTELLER-", "")
  if (productId.startsWith("UNIT-")) return productId.replace("UNIT-", "")
  return productId
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
  const [type, setType] = useState<keyof typeof searchTypes>("medikament")
  const [isMounted, setIsMounted] = useState(false)
  const [hasInitialized, setHasInitialized] = useState(false)

  useEffect(() => {
    setIsMounted(true)
  }, [])

  // Smart initialization
  useEffect(() => {
    if (isMounted && !hasInitialized && productId) {
      const detectedType = getSearchTypeFromProductId(productId)
      setType(detectedType)

      const currentId = correctedId || productId
      const cleanId = removePrefix(currentId)
      setQuery(cleanId)

      setHasInitialized(true)
    }
  }, [isMounted, hasInitialized, productId, correctedId])

  // Update search type when productId changes
  useEffect(() => {
    if (isMounted && hasInitialized && productId) {
      const detectedType = getSearchTypeFromProductId(productId)
      setType(detectedType)

      const currentId = correctedId || productId
      const cleanId = removePrefix(currentId)
      if (query !== cleanId) {
        setQuery(cleanId)
      }
    }
  }, [productId, correctedId, isMounted, hasInitialized])

  const handleSearch = (searchQuery: string, searchType: string) => {
    const finalQuery = buildFinalSearch(searchQuery, searchType as keyof typeof searchTypes)
    router.push(`/${finalQuery}`)
  }

  const handleSearchSubmit = (searchQuery: string) => {
    if (!searchQuery.trim()) {
      router.push("/")
      return
    }

    const finalQuery = buildFinalSearch(searchQuery, type)
    router.push(`/${finalQuery}`)
  }

  const handleTypeChange = (newType: keyof typeof searchTypes) => {
    setType(newType)
  }

  // Placeholder für PassportTabs
  const TabsPlaceholder = () => (
    <div className="flex items-center bg-white border border-gray-200 rounded-xl p-1 opacity-0">
      <div className="flex items-center gap-1">
        <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
        <div className="w-5 h-5 rounded-full bg-gray-200/50"></div>
        <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
        <div className="w-5 h-5 rounded-full bg-gray-200/50"></div>
        <div className="w-8 h-8 rounded-lg bg-gray-200/50"></div>
      </div>
    </div>
  )

  return (
    <div className="fixed top-0 left-0 right-0 z-50 py-1 bg-gray-50">
      <div className="w-full max-w-7xl mx-auto px-4 md:px-6">
        {/* Desktop Layout */}
        <div className="hidden md:flex items-center justify-between">
          {/* Left side - Passport Tabs */}
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

          {/* Right side - Enhanced Search */}
          <div className="relative ml-auto">
            <div className="flex w-full max-w-sm md:max-w-lg items-center rounded-lg border border-gray-200 bg-white focus-within:ring-2 focus-within:ring-emerald-400/50 transition-all duration-300">
              <div className="relative flex w-full">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      type="button"
                      variant="ghost"
                      className="flex h-9 flex-shrink-0 items-center gap-1 rounded-l-lg px-3 text-sm font-medium text-emerald-600 hover:bg-gray-50 hover:text-emerald-700 transition-colors"
                    >
                      <div className="text-emerald-600">{searchTypes[type].icon}</div>
                      <span className="hidden sm:inline-block text-emerald-600">{searchTypes[type].label}</span>
                      <ChevronDown className="h-3 w-3 text-emerald-600" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56 bg-white border-gray-200" align="start">
                    {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                      <DropdownMenuItem
                        key={key}
                        className="text-sm py-2 hover:bg-gray-50"
                        onSelect={() => handleTypeChange(key as keyof typeof searchTypes)}
                      >
                        {icon}
                        <span>{label}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>

                <div className="h-5 w-px bg-gray-300" />

                <div className="relative flex flex-grow items-center">
                  <EnhancedSearchInput
                    value={query}
                    onChange={setQuery}
                    onSubmit={handleSearchSubmit}
                    placeholder="z.B. Aspirin, name:aspirin, MED-1..."
                    className="h-9 w-full border-none bg-transparent focus:ring-0 focus-visible:ring-offset-0 rounded-none"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Mobile Layout */}
        <div className="md:hidden space-y-2">
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

          {/* Enhanced Search darunter */}
          <div className="flex justify-start">
            <div className="flex w-full max-w-sm items-center rounded-lg border border-gray-200 bg-white focus-within:ring-2 focus-within:ring-emerald-400/50 transition-all duration-300">
              <div className="relative flex w-full">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      type="button"
                      variant="ghost"
                      className="flex h-9 flex-shrink-0 items-center gap-1 rounded-l-lg px-3 text-sm font-medium text-emerald-600 hover:bg-gray-50 hover:text-emerald-700 transition-colors"
                    >
                      <div className="text-emerald-600">{searchTypes[type].icon}</div>
                      <span className="hidden sm:inline-block text-emerald-600">{searchTypes[type].label}</span>
                      <ChevronDown className="h-3 w-3 text-emerald-600" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56 bg-white border-gray-200" align="start">
                    {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                      <DropdownMenuItem
                        key={key}
                        className="text-sm py-2 hover:bg-gray-50"
                        onSelect={() => handleTypeChange(key as keyof typeof searchTypes)}
                      >
                        {icon}
                        <span>{label}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>

                <div className="h-5 w-px bg-gray-300" />

                <div className="relative flex flex-grow items-center">
                  <EnhancedSearchInput
                    value={query}
                    onChange={setQuery}
                    onSubmit={handleSearchSubmit}
                    placeholder="z.B. Aspirin, name:aspirin..."
                    className="h-9 w-full border-none bg-transparent focus:ring-0 focus-visible:ring-offset-0 rounded-none"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

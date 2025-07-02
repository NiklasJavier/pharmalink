"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Building, Pill, Box, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Footer } from "@/components/layout/footer"
import { ResponsiveContainer } from "@/components/layout/responsive-container"
import { EnhancedSearchInput } from "@/components/search/enhanced-search-input"
import { useMobileLayout } from "@/components/layout/mobile-layout-provider"
import { useOptimizedConfig } from "@/hooks/use-optimized-config"
import { preloader } from "@/lib/preloader-service"
import { cn } from "@/lib/utils"

const searchTypes = {
  hersteller: {
    label: "Hersteller",
    icon: <Building className="mr-2 h-5 w-5" />,
    prefix: "HERSTELLER-",
  },
  medikament: {
    label: "Medikament",
    icon: <Pill className="mr-2 h-5 w-5" />,
    prefix: "MED-",
  },
  unit: {
    label: "Unit",
    icon: <Box className="mr-2 h-5 w-5" />,
    prefix: "UNIT-",
  },
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

export default function SearchPage() {
  const router = useRouter()
  const { config } = useOptimizedConfig()
  const { isMobile, isTablet, orientation } = useMobileLayout()
  const [searchQuery, setSearchQuery] = useState("")
  const [searchType, setSearchType] = useState("medikament")

  // Start preloading when component mounts
  useEffect(() => {
    preloader.startPreloading()
  }, [])

  // Preload on search input change (debounced)
  useEffect(() => {
    if (searchQuery.trim()) {
      const timeoutId = setTimeout(() => {
        const finalQuery = buildFinalSearch(searchQuery, searchType)
        preloader.queuePreload(finalQuery)
      }, 500)

      return () => clearTimeout(timeoutId)
    }
  }, [searchQuery, searchType])

  const handleSearchSubmit = (query: string) => {
    if (!query.trim()) return

    const finalQuery = buildFinalSearch(query, searchType)
    router.push(`/${finalQuery}`)
  }

  const appName = config?.app.name || "PharmaLink Explorer"
  const appDescription = config?.app.description || "Digitale Informationen für die pharmazeutische Lieferkette"

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <div className="flex flex-col items-center justify-center flex-1">
        <ResponsiveContainer maxWidth="lg" padding="lg" centerContent={false} className="text-center">
          <h1
            className={cn(
              "font-bold text-gray-800 mb-2 transition-all duration-300",
              isMobile ? "text-2xl sm:text-3xl" : "text-4xl",
            )}
          >
            {appName}
          </h1>

          <p
            className={cn(
              "text-gray-500 mb-8 transition-all duration-300",
              isMobile ? "text-sm mb-6 px-2" : "mb-10",
              isTablet && orientation === "landscape" ? "mb-6" : "",
            )}
          >
            {appDescription}
          </p>

          <div
            className={cn(
              "flex items-center bg-white shadow-lg rounded-2xl border border-gray-200 focus-within:ring-2 focus-within:ring-emerald-400/50 p-1 transition-all gap-1",
              isMobile ? "flex-col sm:flex-row w-full" : "flex-row max-w-4xl mx-auto",
            )}
          >
            <div className="flex-shrink-0 basis-1/4">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    className={cn(
                      "flex-shrink-0 text-base px-3 md:px-4 rounded-xl border-gray-300 hover:bg-gray-50 bg-transparent touch-target",
                      isMobile ? "w-full h-12 justify-between sm:w-auto sm:justify-center" : "h-12",
                    )}
                  >
                    <div className="flex items-center">
                      {searchTypes[searchType].icon}
                      <span className={cn(isMobile ? "block sm:hidden md:inline" : "hidden md:inline")}>
                        {searchTypes[searchType].label}
                      </span>
                    </div>
                    <ChevronDown className="ml-2 h-5 w-5 text-gray-500" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-56" align="start">
                  {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                    <DropdownMenuItem
                      key={key}
                      className="text-base py-3 touch-target"
                      onSelect={() => setSearchType(key)}
                    >
                      {icon}
                      <span>{label}</span>
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>

            <div className={cn("relative basis-3/4", isMobile ? "w-full sm:flex-1" : "flex-1")}>
              <EnhancedSearchInput
                value={searchQuery}
                onChange={setSearchQuery}
                onSubmit={handleSearchSubmit}
                placeholder="z.B. Aspirin, name:aspirin, MED-1..."
                className={cn(
                  "w-full bg-white border-gray-300 rounded-xl shadow-none focus:ring-1 focus:ring-emerald-500 focus-visible:ring-offset-0 mobile-input",
                  isMobile ? "h-12 text-base" : "h-12 text-sm md:text-base",
                )}
                autoFocus={!isMobile}
              />
            </div>
          </div>
        </ResponsiveContainer>
      </div>

      <Footer />
    </div>
  )
}

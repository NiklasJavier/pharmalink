"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Search, Building, Pill, Box, ChevronDown } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Footer } from "@/components/layout/footer"
import { useConfig } from "@/hooks/use-config"

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

export default function SearchPage() {
  const router = useRouter()
  const { config } = useConfig()
  const [searchQuery, setSearchQuery] = useState("")
  const [searchType, setSearchType] = useState("medikament")

  const handleSearch = (e) => {
    e.preventDefault()
    if (!searchQuery.trim()) return

    const finalQuery = buildFinalSearch(searchQuery, searchType)
    // Nur der Pfad, keine Query-Parameter
    router.push(`/${finalQuery}`)
  }

  const appName = config?.app.name || "PharmaLink Explorer"
  const appDescription = config?.app.description || "Digitale Informationen für die pharmazeutische Lieferkette"

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <div className="flex flex-col items-center justify-center flex-1 p-4">
        <div className="w-full max-w-3xl text-center">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">{appName}</h1>
          <p className="text-gray-500 mb-10">{appDescription}</p>

          <form
            onSubmit={handleSearch}
            className="flex items-center bg-white shadow-lg rounded-2xl border border-gray-200 focus-within:ring-2 focus-within:ring-emerald-400/50 p-1 transition-all gap-1"
          >
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="outline"
                  className="flex-shrink-0 h-12 text-base px-3 md:px-4 rounded-xl border-gray-300 hover:bg-gray-50 bg-transparent"
                >
                  {searchTypes[searchType].icon}
                  <span className="hidden md:inline">{searchTypes[searchType].label}</span>
                  <ChevronDown className="ml-2 h-5 w-5 text-gray-500" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56" align="start">
                {Object.entries(searchTypes).map(([key, { label, icon }]) => (
                  <DropdownMenuItem key={key} className="text-base py-2" onSelect={() => setSearchType(key)}>
                    {icon}
                    <span>{label}</span>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            <div className="relative flex-grow">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
              <Input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="z.B. MED-1, UNIT-1..."
                className="w-full h-12 text-sm md:text-base bg-white border-gray-300 rounded-xl shadow-none focus:ring-1 focus:ring-emerald-500 focus-visible:ring-offset-0 pl-12"
                autoFocus
              />
            </div>

            <Button
              type="submit"
              size="icon"
              className="h-12 w-12 rounded-xl bg-emerald-600 hover:bg-emerald-700 flex-shrink-0"
            >
              <Search className="h-5 w-5 text-white" />
              <span className="sr-only">Suchen</span>
            </Button>
          </form>
        </div>
      </div>

      <Footer />
    </div>
  )
}

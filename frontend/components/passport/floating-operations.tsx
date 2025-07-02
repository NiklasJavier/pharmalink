"use client"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ChevronDown, ChevronUp, Search, X, ArrowUp, ArrowDown, Download, Check } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { cn } from "@/lib/utils"

type FloatingOperationsProps = {
  globalExpanded: boolean
  onExpandAll: () => void
  onCollapseAll: () => void
  searchTerm: string
  onSearchChange: (value: string) => void
  onClearSearch: () => void
  searchResults: any[]
  currentResultIndex: number
  onNavigateSearch: (direction: "next" | "prev") => void
  // Neue Props für Download
  jsonData?: any
  dataId?: string
  // Neue Prop für erzwungene Minimierung
  forceMinimized?: boolean
}

export function FloatingOperations({
                                     globalExpanded,
                                     onExpandAll,
                                     onCollapseAll,
                                     searchTerm,
                                     onSearchChange,
                                     onClearSearch,
                                     searchResults,
                                     currentResultIndex,
                                     onNavigateSearch,
                                     jsonData,
                                     dataId,
                                     forceMinimized,
                                   }: FloatingOperationsProps) {
  // Geänderte Initialisierung: Beginne erweitert statt minimiert
  const [isMinimized, setIsMinimized] = useState(false) // Geändert von true zu false
  const [isAnimating, setIsAnimating] = useState(false)
  const [showContent, setShowContent] = useState(false)
  const searchInputRef = useRef<HTMLInputElement>(null)

  // Entferne die automatische Desktop/Mobile-Unterscheidung
  useEffect(() => {
    // Wenn forceMinimized true ist, immer minimieren
    if (forceMinimized) {
      setIsMinimized(true)
      setShowContent(false)
      return
    }

    // Content nur anzeigen wenn nicht minimiert und nicht animierend
    if (!isMinimized && !isAnimating) {
      setShowContent(true)
    } else {
      setShowContent(false)
    }
  }, [forceMinimized, isMinimized, isAnimating])

  // Strg+F Handler
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Strg+F oder Cmd+F (für Mac)
      if ((event.ctrlKey || event.metaKey) && event.key === "f") {
        event.preventDefault()
        event.stopPropagation()

        // Zeige das Panel falls minimiert
        if (isMinimized) {
          handleToggle()
        }

        // Fokussiere das Suchfeld nach Animation
        setTimeout(
            () => {
              searchInputRef.current?.focus()
              searchInputRef.current?.select()
            },
            isMinimized ? 400 : 100,
        )
      }

      // Escape zum Schließen der Suche
      if (event.key === "Escape" && document.activeElement === searchInputRef.current) {
        onClearSearch()
        searchInputRef.current?.blur()
      }
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [isMinimized, onClearSearch])

  // Download-Funktion
  const [downloadStatus, setDownloadStatus] = useState<"idle" | "downloading" | "success">("idle")

  const handleDownloadJson = async () => {
    if (!jsonData) return

    try {
      setDownloadStatus("downloading")

      const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, "-")
      const filename = dataId ? `${dataId}_${timestamp}.json` : `pharmalink_data_${timestamp}.json`

      const jsonString = JSON.stringify(jsonData, null, 2)
      const blob = new Blob([jsonString], { type: "application/json" })
      const url = URL.createObjectURL(blob)

      const link = document.createElement("a")
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()

      document.body.removeChild(link)
      URL.revokeObjectURL(url)

      setDownloadStatus("success")
      setTimeout(() => setDownloadStatus("idle"), 2000)
    } catch (error) {
      console.error("Download failed:", error)
      setDownloadStatus("idle")
    }
  }

  // Erweiterte Toggle-Funktion mit Animation-Handling
  const handleToggle = () => {
    if (forceMinimized && !isMinimized) {
      // Wenn forceMinimized aktiv ist, erlaube nur das Minimieren
      return
    }

    const newMinimizedState = !isMinimized

    if (newMinimizedState) {
      // Minimieren: Content sofort verstecken, dann animieren
      setShowContent(false)
      setIsAnimating(true)

      setTimeout(() => {
        setIsMinimized(true)
        setIsAnimating(false)
      }, 50)
    } else {
      // Erweitern: Erst animieren, dann Content anzeigen
      setIsAnimating(true)
      setIsMinimized(false)

      // Content erst nach Abschluss der Animation anzeigen
      setTimeout(() => {
        setShowContent(true)
        setIsAnimating(false)
      }, 300)
    }
  }

  return (
      <div className="fixed bottom-4 right-4 z-50">
        <div
            className={cn(
                "bg-white/95 backdrop-blur-md border border-gray-200/80 rounded-xl shadow-2xl transition-all duration-300 hover:shadow-3xl",
                // Responsive Breite: Mobile schmaler, Desktop breiter
                isMinimized ? "w-12" : "w-80 sm:w-80 md:w-80 lg:w-80",
            )}
        >
          {/* Header - kompakter */}
          <div className="flex items-center justify-between p-2 border-b border-gray-100/50">
            {!isMinimized && (
                <div className="flex items-center gap-1.5 w-full">
                  <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></div>
                  <span className="text-xs font-semibold text-gray-700">JSON-Operationen</span>
                  <Badge variant="outline" className="text-xs px-1 py-0 h-auto hidden sm:inline-flex">
                    Strg+F
                  </Badge>
                </div>
            )}
          </div>

          {/* Content - nur anzeigen wenn showContent true ist */}
          {!isMinimized && showContent && (
              <div className="p-3 space-y-3 animate-in fade-in duration-200">
                {/* Drill Section - beide Buttons nebeneinander - kompakter */}
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-medium text-gray-600 min-w-0 flex-shrink-0">Drill:</span>
                  <div className="flex items-center gap-1">
                    <Button
                        onClick={onCollapseAll}
                        variant="outline"
                        size="sm"
                        className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent border-gray-300"
                        title="Alle Ebenen einklappen - zur Übersicht"
                    >
                      <ChevronUp className="h-3 w-3" />
                    </Button>
                    <Button
                        onClick={onExpandAll}
                        variant="outline"
                        size="sm"
                        className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent border-gray-300"
                        title="Alle Ebenen ausklappen - Details anzeigen"
                    >
                      <ChevronDown className="h-3 w-3" />
                    </Button>
                  </div>
                </div>

                {/* Export Section - kompakter */}
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-medium text-gray-600 min-w-0 flex-shrink-0">Export:</span>
                  <div className="flex items-center gap-1">
                    <Button
                        onClick={handleDownloadJson}
                        disabled={!jsonData || downloadStatus === "downloading"}
                        variant="outline"
                        size="sm"
                        className={cn(
                            "flex items-center justify-center w-8 h-8 p-0 border-gray-300 transition-all duration-200",
                            downloadStatus === "success"
                                ? "bg-emerald-100 border-emerald-300 text-emerald-700 hover:bg-emerald-200"
                                : "hover:bg-gray-100 bg-transparent",
                            !jsonData && "opacity-50 cursor-not-allowed",
                        )}
                        title={
                          !jsonData
                              ? "Keine Daten zum Download verfügbar"
                              : downloadStatus === "downloading"
                                  ? "Download läuft..."
                                  : downloadStatus === "success"
                                      ? "Download erfolgreich!"
                                      : "JSON-Daten als .json-Datei herunterladen"
                        }
                    >
                      {downloadStatus === "downloading" ? (
                          <div className="animate-spin">
                            <Download className="h-3 w-3" />
                          </div>
                      ) : downloadStatus === "success" ? (
                          <Check className="h-3 w-3" />
                      ) : (
                          <Download className="h-3 w-3" />
                      )}
                    </Button>
                  </div>
                  {downloadStatus === "success" && (
                      <Badge
                          variant="secondary"
                          className="text-xs px-1 py-0 h-auto bg-emerald-100 text-emerald-700 animate-in fade-in duration-200"
                      >
                        Gespeichert
                      </Badge>
                  )}
                </div>

                {/* Search Section - kompakter */}
                <div className="space-y-2">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-medium text-gray-600 min-w-0 flex-shrink-0">Suche:</span>
                    <Badge variant="secondary" className="text-xs px-1 py-0 h-auto bg-emerald-100 text-emerald-700">
                      Live
                    </Badge>
                  </div>

                  <div className="relative">
                    <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3 w-3 text-gray-400" />
                    <Input
                        ref={searchInputRef}
                        type="text"
                        value={searchTerm}
                        onChange={(e) => onSearchChange(e.target.value)}
                        placeholder="'key:value' oder 'text'"
                        className="pl-7 pr-7 h-8 text-xs border-gray-300 focus:border-emerald-500 focus:ring-emerald-500/20 rounded-lg"
                    />
                    {searchTerm && (
                        <Button
                            onClick={onClearSearch}
                            variant="ghost"
                            size="sm"
                            className="absolute right-1 top-1/2 -translate-y-1/2 h-6 w-6 p-0 hover:bg-gray-100 rounded-md"
                            title="Suche löschen"
                        >
                          <X className="h-2 w-2" />
                        </Button>
                    )}
                  </div>

                  {/* Search Results Navigation - kompakter */}
                  {searchResults.length > 0 && (
                      <div className="flex items-center justify-between bg-emerald-50/80 rounded-lg p-1.5 border border-emerald-200/50">
                        <div className="flex items-center gap-1.5">
                          <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full"></div>
                          <span className="text-xs font-medium text-emerald-800">
                      {currentResultIndex + 1} von {searchResults.length}
                    </span>
                        </div>

                        <div className="flex items-center gap-0.5">
                          <Button
                              onClick={() => onNavigateSearch("prev")}
                              variant="outline"
                              size="sm"
                              className="h-6 w-6 p-0 border-emerald-300 hover:bg-emerald-100 text-emerald-700"
                              title="Vorheriges Suchergebnis"
                          >
                            <ArrowUp className="h-2 w-2" />
                          </Button>
                          <Button
                              onClick={() => onNavigateSearch("next")}
                              variant="outline"
                              size="sm"
                              className="h-6 w-6 p-0 border-emerald-300 hover:bg-emerald-100 text-emerald-700"
                              title="Nächstes Suchergebnis"
                          >
                            <ArrowDown className="h-2 w-2" />
                          </Button>
                        </div>
                      </div>
                  )}

                  {/* Search Help - kompakter und responsive */}
                  {!searchTerm && (
                      <div className="text-xs text-gray-500 bg-gray-50/80 rounded-lg p-1.5 border border-gray-200/50">
                        <div className="font-medium mb-0.5">Suchtipps:</div>
                        <div className="space-y-0.5">
                          <div>
                            • <code className="bg-white px-1 rounded text-emerald-600 text-xs">name:aspirin</code> -
                            Schlüssel:Wert
                          </div>
                          <div>
                            • <code className="bg-white px-1 rounded text-blue-600 text-xs">500mg</code> - Freitext
                          </div>
                          <div className="hidden sm:block">
                            • <kbd className="bg-white px-1 rounded border text-gray-600 text-xs">Strg+F</kbd> - Fokus
                          </div>
                        </div>
                      </div>
                  )}
                </div>
              </div>
          )}

          {/* Minimized State - kompakter mit verbesserter Touch-Interaktion */}
          {isMinimized && (
              <div className="p-2 flex flex-col items-center gap-1.5">
                <Button
                    onClick={handleToggle}
                    variant="ghost"
                    size="sm"
                    className="w-8 h-8 p-0 hover:bg-emerald-100 rounded-lg text-emerald-600 touch-manipulation"
                    title="JSON-Operationen erweitern"
                >
                  <Search className="h-3 w-3" />
                </Button>

                {/* Download Button im minimierten Zustand - kompakter */}
                {jsonData && (
                    <Button
                        onClick={handleDownloadJson}
                        disabled={downloadStatus === "downloading"}
                        variant="ghost"
                        size="sm"
                        className={cn(
                            "w-8 h-8 p-0 rounded-lg transition-all duration-200 touch-manipulation",
                            downloadStatus === "success"
                                ? "bg-emerald-100 text-emerald-600 hover:bg-emerald-200"
                                : "hover:bg-gray-100 text-gray-600",
                        )}
                        title={
                          downloadStatus === "downloading"
                              ? "Download läuft..."
                              : downloadStatus === "success"
                                  ? "Download erfolgreich!"
                                  : "JSON herunterladen"
                        }
                    >
                      {downloadStatus === "downloading" ? (
                          <div className="animate-spin">
                            <Download className="h-3 w-3" />
                          </div>
                      ) : downloadStatus === "success" ? (
                          <Check className="h-3 w-3" />
                      ) : (
                          <Download className="h-3 w-3" />
                      )}
                    </Button>
                )}

                {searchResults.length > 0 && (
                    <Badge variant="secondary" className="text-xs px-1 py-0 h-auto bg-emerald-100 text-emerald-700">
                      {searchResults.length}
                    </Badge>
                )}
              </div>
          )}

          {/* Floating Indicator - kleiner */}
          <div className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-emerald-500 rounded-full animate-pulse shadow-lg"></div>
        </div>
      </div>
  )
}
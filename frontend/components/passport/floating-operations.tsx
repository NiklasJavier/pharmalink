"use client"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  ChevronDown,
  ChevronUp,
  Search,
  X,
  ArrowUp,
  ArrowDown,
  Download,
  Check,
  Bell,
  Truck,
  RotateCcw,
  Clock,
  MapPin,
  CheckCircle2,
  Package,
} from "lucide-react"
import { useState, useRef, useEffect, useCallback } from "react"
import { cn } from "@/lib/utils"
import type { MetaPopup } from "@/lib/meta-popup-service"
import type { DeliveryChain } from "@/lib/delivery-service"
import { dismissPopup, getDismissedPopups, clearDismissedPopups } from "@/lib/meta-popup-service"
import { formatDeliveryTimestamp, getDeliveryStatus } from "@/lib/delivery-service"
import { useSearchParams } from "next/navigation"

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
  // Neue Props für Popups und Deliveries
  popups?: MetaPopup[]
  deliveryChains?: DeliveryChain[]
  onPopupExpandChange?: (isExpanded: boolean) => void
  onDeliveryExpandChange?: (isExpanded: boolean) => void
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
  popups = [],
  deliveryChains = [],
  onPopupExpandChange,
  onDeliveryExpandChange,
}: FloatingOperationsProps) {
  const searchParams = useSearchParams()
  // Standardmäßig eingeklappt (true = minimiert)
  const [isMinimized, setIsMinimized] = useState(true)
  const [showContent, setShowContent] = useState(false)
  const [hasAutoFocused, setHasAutoFocused] = useState(false)
  const [activeSection, setActiveSection] = useState<"json" | "notifications" | "deliveries">("json")
  const searchInputRef = useRef<HTMLInputElement>(null)

  // Popup state management
  const [dismissedIds, setDismissedIds] = useState<string[]>([])

  // Initialize dismissed popups from localStorage
  useEffect(() => {
    setDismissedIds(getDismissedPopups())
  }, [])

  // URL parameter handling for sections - NUR auf spezifische Parameter reagieren
  useEffect(() => {
    const meldungenParam = searchParams.get("meldungen")
    const lieferungenParam = searchParams.get("lieferungen")

    if (meldungenParam === "open") {
      setActiveSection("notifications")
      setIsMinimized(false) // Nur dann erweitern
      onPopupExpandChange?.(true)
    } else if (lieferungenParam === "open") {
      setActiveSection("deliveries")
      setIsMinimized(false) // Nur dann erweitern
      onDeliveryExpandChange?.(true)
    }
    // Wenn keine URL-Parameter vorhanden sind, bleibt es eingeklappt
  }, [
    // NUR auf diese spezifischen Parameter reagieren
    searchParams.get("meldungen"),
    searchParams.get("lieferungen"),
    // Entferne die Callback-Dependencies um Re-Renders zu vermeiden
  ])

  useEffect(() => {
    if (forceMinimized) {
      setIsMinimized(true)
      setShowContent(false)
      return
    }

    // Direktes Setzen ohne Animation-Delays
    setShowContent(!isMinimized)
  }, [forceMinimized, isMinimized])

  useEffect(() => {
    if (!hasAutoFocused && showContent && !isMinimized && searchInputRef.current && activeSection === "json") {
      // Reduziere Timeout für schnellere Response
      const timeoutId = setTimeout(() => {
        searchInputRef.current?.focus()
        setHasAutoFocused(true)
      }, 50) // Reduziert von 100ms auf 50ms

      return () => clearTimeout(timeoutId)
    }
  }, [showContent, isMinimized, hasAutoFocused, activeSection])

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key === "f") {
        event.preventDefault()
        event.stopPropagation()

        // Verwende die handleSectionChange Funktion für konsistente Logik
        handleSectionChange("json")
      }

      if (event.key === "Escape" && document.activeElement === searchInputRef.current) {
        onClearSearch()
        searchInputRef.current?.blur()
      }
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [onClearSearch]) // Entferne isMinimized aus den Dependencies

  // Download functionality
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

  const handleToggle = () => {
    if (forceMinimized && !isMinimized) {
      return
    }

    const newMinimizedState = !isMinimized

    if (newMinimizedState) {
      // Beim Minimieren: URL-Parameter für aktive Sections zurücksetzen
      if (activeSection === "notifications") {
        updateUrlParams({ meldungen: null })
        onPopupExpandChange?.(false)
      } else if (activeSection === "deliveries") {
        updateUrlParams({ lieferungen: null })
        onDeliveryExpandChange?.(false)
      }
    }

    // Direkte State-Änderung ohne komplexe Timeouts
    setIsMinimized(newMinimizedState)
    setShowContent(!newMinimizedState)

    // Fokus nur bei Expansion und JSON-Section
    if (!newMinimizedState && activeSection === "json") {
      // Kurzer Timeout nur für Fokus, nicht für Animation
      setTimeout(() => {
        searchInputRef.current?.focus()
      }, 150)
    }
  }

  // URL parameter update helper
  const updateUrlParams = (updates: Record<string, string | null>) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()))
    Object.entries(updates).forEach(([k, v]) => {
      if (!v) current.delete(k)
      else current.set(k, v)
    })
    const q = current.toString()
    window.history.replaceState({}, "", `${window.location.pathname}${q ? `?${q}` : ""}`)
  }

  // Popup functions
  const handleDismiss = (popupId: string) => {
    const popup = popups.find((p) => p.id === popupId)
    if (popup?.dismissible !== false) {
      dismissPopup(popupId)
      setDismissedIds((prev) => [...prev, popupId])
    }
  }

  const handleRestoreAll = () => {
    clearDismissedPopups()
    setDismissedIds([])
  }

  // Calculate statistics
  const activePopups = popups.filter((popup) => !dismissedIds.includes(popup.id))
  const dismissedPopups = popups.filter((popup) => dismissedIds.includes(popup.id))
  const activePopupCount = activePopups.length
  const totalDeliveries = deliveryChains.reduce((sum, chain) => sum + chain.totalDeliveries, 0)

  // Section switching - Verbesserte Logik
  const handleSectionChange = useCallback(
    (section: "json" | "notifications" | "deliveries") => {
      // Setze die aktive Sektion sofort
      setActiveSection(section)

      // Update URL-Parameter entsprechend
      if (section === "notifications") {
        updateUrlParams({ meldungen: "open", lieferungen: null })
        onPopupExpandChange?.(true)
        onDeliveryExpandChange?.(false)
      } else if (section === "deliveries") {
        updateUrlParams({ lieferungen: "open", meldungen: null })
        onDeliveryExpandChange?.(true)
        onPopupExpandChange?.(false)
      } else {
        // JSON-Sektion: Entferne alle Modal-Parameter
        updateUrlParams({ meldungen: null, lieferungen: null })
        onPopupExpandChange?.(false)
        onDeliveryExpandChange?.(false)
      }

      // Erweitere das Panel falls minimiert
      if (isMinimized) {
        setIsMinimized(false)
        setShowContent(true)
      }

      // Fokussiere Suchfeld bei JSON-Sektion
      if (section === "json") {
        setTimeout(() => {
          searchInputRef.current?.focus()
        }, 100)
      }
    },
    [isMinimized, onPopupExpandChange, onDeliveryExpandChange],
  )

  const getPositionStyle = () => {
    return {
      position: "fixed" as const,
      bottom: "1rem",
      right: "1rem",
      zIndex: 50,
    }
  }

  return (
    <div
      style={getPositionStyle()}
      className="transform-gpu" // Hardware-Beschleunigung
    >
      <div
        className={cn(
          "bg-white/95 backdrop-blur-md border border-gray-200/80 rounded-xl shadow-2xl transition-all duration-200 ease-out hover:shadow-3xl transform-gpu will-change-transform",
          isMinimized ? "w-12" : "w-72 sm:w-80 md:w-80 lg:w-80", // Kompaktere Breite
        )}
      >
        {/* Minimized State */}
        {isMinimized && (
          <div className="p-2 flex flex-col items-center gap-1.5">
            {/* Deliveries Button */}
            {totalDeliveries > 0 && (
              <Button
                onClick={() => handleSectionChange("deliveries")}
                variant="ghost"
                size="sm"
                className="w-8 h-8 p-0 rounded-lg relative transition-colors hover:bg-blue-100 text-blue-600 touch-manipulation"
                title={`${totalDeliveries} Lieferung${totalDeliveries !== 1 ? "en" : ""}`}
              >
                <Truck className="h-3 w-3" />
                <Badge
                  variant="secondary"
                  className="absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[16px] flex items-center justify-center border bg-blue-100 text-blue-800 border-blue-300"
                >
                  {totalDeliveries}
                </Badge>
              </Button>
            )}

            {/* Notifications Button */}
            <Button
              onClick={() => handleSectionChange("notifications")}
              variant="ghost"
              size="sm"
              className={`w-8 h-8 p-0 rounded-lg relative transition-colors touch-manipulation ${
                activePopupCount > 0 ? "hover:bg-amber-100 text-amber-600" : "hover:bg-gray-100 text-gray-600"
              }`}
              title={
                activePopupCount > 0
                  ? `${activePopupCount} aktive Meldung${activePopupCount !== 1 ? "en" : ""}`
                  : "Keine aktiven Meldungen"
              }
            >
              <Bell className="h-3 w-3" />
              <Badge
                variant="secondary"
                className={`absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[16px] flex items-center justify-center border ${
                  activePopupCount > 0
                    ? "bg-amber-100 text-amber-800 border-amber-300"
                    : "bg-gray-100 text-gray-600 border-gray-300"
                }`}
              >
                {activePopupCount}
              </Badge>
            </Button>

            {/* JSON Operations Button */}
            <Button
              onClick={() => handleSectionChange("json")}
              variant="ghost"
              size="sm"
              className="w-8 h-8 p-0 hover:bg-emerald-100 rounded-lg text-emerald-600 touch-manipulation"
              title="JSON-Operationen erweitern"
            >
              <Search className="h-3 w-3" />
            </Button>

            {/* Download Button */}
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

        {/* Expanded State */}
        {!isMinimized && showContent && (
          <>
            {/* Header with Section Tabs */}
            <div className="flex items-center justify-between p-2 border-b border-gray-100/50">
              <div className="flex items-center gap-1 flex-1 min-w-0">
                {/* JSON Tab - nur Icon */}
                <Button
                  onClick={() => handleSectionChange("json")}
                  variant="ghost"
                  size="sm"
                  className={cn(
                    "h-7 w-7 p-0 text-xs transition-colors flex-shrink-0 rounded-md",
                    activeSection === "json"
                      ? "bg-emerald-100 text-emerald-700 hover:bg-emerald-200"
                      : "hover:bg-gray-100 text-gray-600",
                  )}
                  title="JSON-Operationen"
                >
                  <Search className="h-3 w-3" />
                </Button>

                {/* Notifications Tab - nur Icon mit Badge */}
                <Button
                  onClick={() => handleSectionChange("notifications")}
                  variant="ghost"
                  size="sm"
                  className={cn(
                    "h-7 w-7 p-0 text-xs transition-colors flex-shrink-0 rounded-md relative",
                    activeSection === "notifications"
                      ? "bg-amber-100 text-amber-700 hover:bg-amber-200"
                      : "hover:bg-gray-100 text-gray-600",
                  )}
                  title="Meldungen"
                >
                  <Bell className="h-3 w-3" />
                  {activePopupCount > 0 && (
                    <Badge
                      variant="secondary"
                      className="absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[14px] flex items-center justify-center bg-amber-200 text-amber-800 border border-amber-300"
                    >
                      {activePopupCount}
                    </Badge>
                  )}
                </Button>

                {/* Deliveries Tab - nur Icon mit Badge */}
                {totalDeliveries > 0 && (
                  <Button
                    onClick={() => handleSectionChange("deliveries")}
                    variant="ghost"
                    size="sm"
                    className={cn(
                      "h-7 w-7 p-0 text-xs transition-colors flex-shrink-0 rounded-md relative",
                      activeSection === "deliveries"
                        ? "bg-blue-100 text-blue-700 hover:bg-blue-200"
                        : "hover:bg-gray-100 text-gray-600",
                    )}
                    title="Lieferungen"
                  >
                    <Truck className="h-3 w-3" />
                    <Badge
                      variant="secondary"
                      className="absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[14px] flex items-center justify-center bg-blue-200 text-blue-800 border border-blue-300"
                    >
                      {totalDeliveries}
                    </Badge>
                  </Button>
                )}
              </div>

              {/* Minimize Button */}
              <Button
                onClick={handleToggle}
                variant="ghost"
                size="sm"
                className="w-6 h-6 p-0 hover:bg-gray-100 rounded-md flex-shrink-0 ml-2"
                title="Minimieren"
              >
                <ChevronDown className="h-3 w-3" />
              </Button>
            </div>

            {/* Content based on active section */}
            <div className="max-h-[60vh] overflow-y-auto">
              {activeSection === "json" && (
                <div className="p-3 space-y-3 animate-in fade-in duration-200">
                  {/* Drill Section */}
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-medium text-gray-600 min-w-0 flex-shrink-0">Drill:</span>
                    <div className="flex items-center gap-1">
                      <Button
                        onClick={onCollapseAll}
                        variant="outline"
                        size="sm"
                        className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent border-gray-300"
                        title="Alle Ebenen einklappen"
                      >
                        <ChevronUp className="h-3 w-3" />
                      </Button>
                      <Button
                        onClick={onExpandAll}
                        variant="outline"
                        size="sm"
                        className="flex items-center justify-center w-8 h-8 p-0 hover:bg-gray-100 bg-transparent border-gray-300"
                        title="Alle Ebenen ausklappen"
                      >
                        <ChevronDown className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>

                  {/* Export Section */}
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

                  {/* Search Section */}
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

                    {/* Search Results Navigation */}
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

                    {/* Search Help */}
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

              {activeSection === "notifications" && (
                <div className="p-3 space-y-3 animate-in fade-in duration-200">
                  {/* Active Notifications */}
                  {activePopups.length > 0 && (
                    <div className="space-y-2">
                      <div className="text-xs font-medium text-gray-600">Aktive Meldungen</div>
                      {activePopups.map((popup) => (
                        <div
                          key={popup.id}
                          className="rounded-lg border border-amber-200/80 bg-amber-50/95 p-3 shadow-md transition-all duration-200 hover:shadow-lg relative backdrop-blur-sm"
                        >
                          {popup.dismissible !== false && (
                            <Button
                              onClick={() => handleDismiss(popup.id)}
                              variant="ghost"
                              size="sm"
                              className="absolute top-2 right-2 w-5 h-5 p-0 hover:bg-amber-100/80 rounded-sm opacity-60 hover:opacity-100"
                              title="Schließen"
                            >
                              <X className="h-2.5 w-2.5" />
                            </Button>
                          )}

                          <div className="pr-6">
                            <div className="text-xs font-medium mb-1 text-amber-900">{popup.author}:</div>
                            <div className="text-xs leading-relaxed text-amber-800">{popup.message}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Dismissed Notifications */}
                  {dismissedPopups.length > 0 && (
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-medium text-gray-600">Geschlossene Meldungen</div>
                        <Button
                          onClick={handleRestoreAll}
                          variant="ghost"
                          size="sm"
                          className="h-6 px-2 text-xs hover:bg-gray-100 text-gray-600 hover:text-gray-800"
                          title="Alle Meldungen wiederherstellen"
                        >
                          <RotateCcw className="h-3 w-3 mr-1" />
                          Alle zeigen
                        </Button>
                      </div>
                      {dismissedPopups.map((popup) => (
                        <div
                          key={popup.id}
                          className="rounded-lg border border-gray-200/80 bg-gray-50/95 p-3 shadow-sm transition-all duration-200 relative backdrop-blur-sm opacity-75"
                        >
                          <div>
                            <div className="text-xs font-medium mb-1 text-gray-700">{popup.author}:</div>
                            <div className="text-xs leading-relaxed text-gray-600">{popup.message}</div>
                          </div>
                          <div className="absolute top-2 right-2">
                            <Badge
                              variant="secondary"
                              className="text-xs px-1.5 py-0.5 h-auto bg-gray-200 text-gray-600 border border-gray-300"
                            >
                              Geschlossen
                            </Badge>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* No Notifications */}
                  {popups.length === 0 && (
                    <div className="rounded-lg border border-gray-200/80 bg-gray-50/95 p-4 shadow-sm backdrop-blur-sm text-center">
                      <div className="flex flex-col items-center gap-3">
                        <div className="p-2 bg-gray-100/80 rounded-lg">
                          <Bell className="h-4 w-4 text-gray-500" />
                        </div>
                        <div>
                          <div className="text-xs font-medium text-gray-700 mb-1">Keine Meldungen</div>
                          <div className="text-xs text-gray-600">Aktuell sind keine Meldungen verfügbar</div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {activeSection === "deliveries" && (
                <ScrollArea className="h-full">
                  <div className="p-3 space-y-4 animate-in fade-in duration-200">
                    {deliveryChains.map((chain) => (
                      <div key={chain.id} className="space-y-2">
                        {/* Chain Header */}
                        <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
                          <Package className="h-3 w-3 text-blue-600" />
                          <span className="text-sm font-medium text-gray-800">{chain.title}</span>
                          <Badge variant="outline" className="text-xs px-1.5 py-0.5 h-auto">
                            {chain.entries.length}
                          </Badge>
                        </div>

                        {/* Delivery Entries */}
                        <div className="space-y-2">
                          {chain.entries.map((entry, index) => {
                            const status = getDeliveryStatus(entry.timestamp)
                            const formattedTime = formatDeliveryTimestamp(entry.timestamp)
                            const isLatest = index === 0

                            return (
                              <div
                                key={entry.id}
                                className={cn(
                                  "rounded-lg border p-3 shadow-sm transition-all duration-200 hover:shadow-md relative backdrop-blur-sm",
                                  status.bgColor,
                                  isLatest ? "border-blue-300 ring-1 ring-blue-200" : "border-gray-200",
                                )}
                              >
                                {/* Status Icon */}
                                <div className="absolute top-2 right-2">
                                  {status.status === "delivered" && (
                                    <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                                  )}
                                  {status.status === "in-transit" && <Truck className="h-3 w-3 text-amber-600" />}
                                  {status.status === "pending" && <Clock className="h-3 w-3 text-blue-600" />}
                                </div>

                                {/* Content */}
                                <div className="pr-6">
                                  {/* Recipient */}
                                  <div className="flex items-center gap-2 mb-1">
                                    <span className="text-sm font-medium text-gray-900">{entry.recipient}</span>
                                    {isLatest && (
                                      <Badge
                                        variant="secondary"
                                        className="bg-blue-100 text-blue-800 text-xs px-2 py-0.5 h-auto border border-blue-300"
                                      >
                                        Aktuell
                                      </Badge>
                                    )}
                                  </div>

                                  {/* Timestamp */}
                                  <div className="flex items-center gap-1 text-xs text-gray-600 mb-1">
                                    <Clock className="h-3 w-3" />
                                    {formattedTime}
                                  </div>

                                  {/* Location (if available) */}
                                  {entry.location && (
                                    <div className="flex items-center gap-1 text-xs text-gray-600">
                                      <MapPin className="h-3 w-3" />
                                      {entry.location}
                                    </div>
                                  )}

                                  {/* Status Text */}
                                  <div className={cn("text-xs font-medium mt-1", status.color)}>
                                    {status.status === "delivered" && "Zugestellt"}
                                    {status.status === "in-transit" && "Unterwegs"}
                                    {status.status === "pending" && "Geplant"}
                                  </div>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              )}
            </div>
          </>
        )}

        {/* Floating Indicator */}
        <div className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-emerald-500 rounded-full animate-pulse shadow-lg"></div>
      </div>
    </div>
  )
}

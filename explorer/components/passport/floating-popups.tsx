"use client"

import { useState, useEffect, useRef } from "react"
import { useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { X, ChevronDown, Bell, RotateCcw } from "lucide-react"
import type { MetaPopup } from "@/lib/meta-popup-service"
import { dismissPopup, getDismissedPopups, clearDismissedPopups } from "@/lib/meta-popup-service"

interface FloatingPopupsProps {
  popups: MetaPopup[]
  // Props werden beibehalten für Kompatibilität, aber nicht mehr für Positionierung verwendet
  jsonOperationsMinimized: boolean
  historyModalOpen: boolean
  onExpandChange?: (isExpanded: boolean) => void // Neue Prop
}

export function FloatingPopups({ popups, onExpandChange }: FloatingPopupsProps) {
  const searchParams = useSearchParams()
  const [dismissedIds, setDismissedIds] = useState<string[]>([])
  const [isCollapsed, setIsCollapsed] = useState(true) // Start collapsed
  const autoHideTimeouts = useRef<Map<string, NodeJS.Timeout>>(new Map())
  const modalRef = useRef<HTMLDivElement>(null)

  // Initialisiere dismissed popups aus localStorage
  useEffect(() => {
    setDismissedIds(getDismissedPopups())
  }, [])

  // URL-Parameter für Meldungen-Zustand lesen und anwenden
  useEffect(() => {
    const meldungenParam = searchParams.get("meldungen")
    const shouldBeOpen = meldungenParam === "open"

    if (shouldBeOpen !== !isCollapsed) {
      setIsCollapsed(!shouldBeOpen)
      onExpandChange?.(shouldBeOpen)
    }
  }, [searchParams, isCollapsed, onExpandChange])

  // Cleanup timeouts on unmount
  useEffect(() => {
    return () => {
      autoHideTimeouts.current.forEach((timeout) => clearTimeout(timeout))
    }
  }, [])

  // Click outside detection - einklappt das Modal wenn außerhalb geklickt wird
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!isCollapsed && modalRef.current && !modalRef.current.contains(event.target as Node)) {
        handleCollapse()
      }
    }

    if (!isCollapsed) {
      // Kleine Verzögerung um zu verhindern, dass der Öffnen-Klick sofort das Modal schließt
      const timeoutId = setTimeout(() => {
        document.addEventListener("mousedown", handleClickOutside)
      }, 100)

      return () => {
        clearTimeout(timeoutId)
        document.removeEventListener("mousedown", handleClickOutside)
      }
    }
  }, [isCollapsed])

  // Hilfsfunktion zum Aktualisieren der URL-Parameter
  const updateUrlParams = (updates: Record<string, string | null>) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()))
    Object.entries(updates).forEach(([k, v]) => {
      if (!v) current.delete(k)
      else current.set(k, v)
    })
    const q = current.toString()
    window.history.replaceState({}, "", `${window.location.pathname}${q ? `?${q}` : ""}`)
  }

  const handleExpand = () => {
    setIsCollapsed(false)
    onExpandChange?.(true)
    updateUrlParams({ meldungen: "open" })
  }

  const handleCollapse = () => {
    setIsCollapsed(true)
    onExpandChange?.(false)
    updateUrlParams({ meldungen: null }) // Entfernt den Parameter
  }

  const handleDismiss = (popupId: string) => {
    const popup = popups.find((p) => p.id === popupId)
    if (popup?.dismissible !== false) {
      dismissPopup(popupId)
      setDismissedIds((prev) => [...prev, popupId])

      // Clear auto-hide timeout
      const timeout = autoHideTimeouts.current.get(popupId)
      if (timeout) {
        clearTimeout(timeout)
        autoHideTimeouts.current.delete(popupId)
      }
    }
  }

  const handleRestoreAll = () => {
    clearDismissedPopups()
    setDismissedIds([])
  }

  const handleToggle = () => {
    if (isCollapsed) {
      handleExpand()
    } else {
      handleCollapse()
    }
  }

  // Berechne aktive und geschlossene Meldungen
  const activePopups = popups.filter((popup) => !dismissedIds.includes(popup.id))
  const dismissedPopups = popups.filter((popup) => dismissedIds.includes(popup.id))
  const totalPopups = popups.length
  const activeCount = activePopups.length
  const dismissedCount = dismissedPopups.length

  // Ändere die getPositionStyle Funktion für bessere Stapelung
  const getPositionStyle = () => {
    return {
      position: "fixed" as const,
      bottom: "5rem", // Über dem JSON-Operations Panel (4rem Höhe + 1rem Abstand)
      right: "1rem",
      zIndex: 30,
    }
  }

  // Modal wird immer angezeigt, auch wenn keine Popups vorhanden sind
  return (
    <>
      {/* Blur Overlay - nur wenn erweitert */}
      {!isCollapsed && (
        <div className="fixed inset-0 z-25 bg-black/5 backdrop-blur-sm transition-all duration-300 animate-in fade-in" />
      )}

      <div style={getPositionStyle()}>
        <div className="animate-in slide-in-from-right-4 duration-300">
          {isCollapsed ? (
            /* Collapsed State - schmaler vertikaler Strip wie JSON-Operations */
            <div className="bg-white/95 backdrop-blur-md border border-gray-200/80 rounded-xl shadow-2xl transition-all duration-300 hover:shadow-3xl w-12">
              {/* Floating Indicator - angepasst für Status */}
              <div
                className={`absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full animate-pulse shadow-lg ${
                  activeCount > 0 ? "bg-amber-500" : "bg-gray-400"
                }`}
              ></div>

              <div className="p-2 flex flex-col items-center gap-2">
                {/* Bell Icon mit Badge - zeigt immer an */}
                <Button
                  onClick={handleExpand}
                  variant="ghost"
                  size="sm"
                  className={`w-8 h-8 p-0 rounded-lg relative transition-colors ${
                    activeCount > 0 ? "hover:bg-amber-100 text-amber-600" : "hover:bg-gray-100 text-gray-600"
                  }`}
                  title={
                    activeCount > 0
                      ? `${activeCount} aktive Meldung${activeCount !== 1 ? "en" : ""}`
                      : totalPopups > 0
                        ? `Alle ${totalPopups} Meldungen geschlossen`
                        : "Keine Meldungen"
                  }
                >
                  <Bell className="h-3 w-3" />

                  {/* Badge zeigt immer die Anzahl der aktiven Meldungen */}
                  <Badge
                    variant="secondary"
                    className={`absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[16px] flex items-center justify-center border ${
                      activeCount > 0
                        ? "bg-amber-100 text-amber-800 border-amber-300"
                        : "bg-gray-100 text-gray-600 border-gray-300"
                    }`}
                  >
                    {activeCount}
                  </Badge>
                </Button>
              </div>
            </div>
          ) : (
            /* Expanded State - gleiche Breite wie JSON-Operations Panel */
            <div
              ref={modalRef}
              className="bg-white/95 backdrop-blur-xl border border-gray-200/80 rounded-xl shadow-2xl ring-1 ring-black/5 transition-all duration-300 relative min-w-80 max-w-[calc(100vw-2rem)]"
            >
              {/* Floating Indicator - verstärkt und statusabhängig */}
              <div
                className={`absolute -top-1 -right-1 w-3 h-3 rounded-full animate-pulse shadow-lg z-10 ring-2 ring-white/50 ${
                  activeCount > 0 ? "bg-amber-400" : "bg-gray-400"
                }`}
              ></div>

              {/* Header */}
              <div className="flex items-center justify-between p-3 border-b border-gray-100/50">
                <div className="flex items-center gap-2">
                  <div
                    className={`w-2 h-2 rounded-full animate-pulse ${activeCount > 0 ? "bg-amber-400" : "bg-gray-400"}`}
                  ></div>
                  <span className="text-xs font-semibold text-gray-800">Meldungen</span>
                  <Badge
                    variant="outline"
                    className="text-xs px-2 py-0.5 h-auto text-gray-700 border-gray-300 bg-white/60"
                  >
                    {activeCount} aktiv
                  </Badge>
                  {dismissedCount > 0 && (
                    <Badge
                      variant="outline"
                      className="text-xs px-2 py-0.5 h-auto text-gray-500 border-gray-200 bg-gray-50/60"
                    >
                      {dismissedCount} geschlossen
                    </Badge>
                  )}
                </div>

                <Button
                  onClick={handleCollapse}
                  variant="ghost"
                  size="sm"
                  className="w-6 h-6 p-0 hover:bg-gray-100 rounded-md"
                  title="Minimieren"
                >
                  <ChevronDown className="h-3 w-3" />
                </Button>
              </div>

              {/* Content - zeigt immer alle Meldungen */}
              <div className="max-h-[60vh] overflow-y-auto">
                <div className="p-3 space-y-3">
                  {/* Aktive Meldungen */}
                  {activePopups.length > 0 && (
                    <div className="space-y-2">
                      <div className="text-xs font-medium text-gray-600">Aktive Meldungen</div>
                      {activePopups.map((popup) => (
                        <div
                          key={popup.id}
                          className="rounded-lg border border-amber-200/80 bg-amber-50/95 p-3 shadow-md transition-all duration-200 hover:shadow-lg relative backdrop-blur-sm"
                        >
                          {/* Close Button */}
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

                          {/* Key-Value Content */}
                          <div className="pr-6">
                            {/* Author (Key) */}
                            <div className="text-xs font-medium mb-1 text-amber-900">{popup.author}:</div>

                            {/* Message (Value) */}
                            <div className="text-xs leading-relaxed text-amber-800">{popup.message}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Geschlossene Meldungen */}
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
                          {/* Key-Value Content - ohne Close Button */}
                          <div>
                            {/* Author (Key) */}
                            <div className="text-xs font-medium mb-1 text-gray-700">{popup.author}:</div>

                            {/* Message (Value) */}
                            <div className="text-xs leading-relaxed text-gray-600">{popup.message}</div>
                          </div>

                          {/* Geschlossen-Indikator */}
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

                  {/* Keine Meldungen vorhanden */}
                  {totalPopups === 0 && (
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
              </div>

              {/* Innerer Glanz-Effekt */}
              <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

"use client"

import { useState, useEffect, useRef } from "react"
import { useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { ChevronDown, Truck, Clock, MapPin, CheckCircle2, Package } from "lucide-react"
import type { DeliveryChain } from "@/lib/delivery-service"
import { formatDeliveryTimestamp, getDeliveryStatus } from "@/lib/delivery-service"
import { cn } from "@/lib/utils"

interface FloatingDeliveriesProps {
  deliveryChains: DeliveryChain[]
  jsonOperationsMinimized: boolean
  historyModalOpen: boolean
  onExpandChange?: (isExpanded: boolean) => void
  // Neue Prop für Meldungen-Modal Status
  meldungenModalExpanded?: boolean
}

export function FloatingDeliveries({
  deliveryChains,
  jsonOperationsMinimized,
  historyModalOpen,
  onExpandChange,
  meldungenModalExpanded = false, // Default: nicht erweitert
}: FloatingDeliveriesProps) {
  const searchParams = useSearchParams()
  const [isCollapsed, setIsCollapsed] = useState(true) // Start collapsed
  const modalRef = useRef<HTMLDivElement>(null)

  // URL-Parameter für Lieferungen-Zustand lesen und anwenden
  useEffect(() => {
    const lieferungenParam = searchParams.get("lieferungen")
    const shouldBeOpen = lieferungenParam === "open"

    if (shouldBeOpen !== !isCollapsed) {
      setIsCollapsed(!shouldBeOpen)
      onExpandChange?.(shouldBeOpen)
    }
  }, [searchParams, isCollapsed, onExpandChange])

  // Click outside detection
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!isCollapsed && modalRef.current && !modalRef.current.contains(event.target as Node)) {
        handleCollapse()
      }
    }

    if (!isCollapsed) {
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
    updateUrlParams({ lieferungen: "open" })
  }

  const handleCollapse = () => {
    setIsCollapsed(true)
    onExpandChange?.(false)
    updateUrlParams({ lieferungen: null })
  }

  const handleToggle = () => {
    if (isCollapsed) {
      handleExpand()
    } else {
      handleCollapse()
    }
  }

  // Berechne Statistiken
  const totalDeliveries = deliveryChains.reduce((sum, chain) => sum + chain.totalDeliveries, 0)
  const totalChains = deliveryChains.length

  // Ändere die getPositionStyle Funktion für Stapelung über Meldungen
  const getPositionStyle = () => {
    // Wenn Meldungen-Modal erweitert ist, positioniere darüber
    if (meldungenModalExpanded) {
      return {
        position: "fixed" as const,
        bottom: "calc(5rem + 20rem + 0.5rem)", // Über dem erweiterten Meldungen-Modal
        right: "1rem",
        zIndex: 30,
      }
    } else {
      return {
        position: "fixed" as const,
        bottom: "calc(5rem + 3rem + 0.5rem)", // Über dem minimierten Meldungen-Button
        right: "1rem",
        zIndex: 30,
      }
    }
  }

  // Entferne die meldungenModalExpanded Bedingung aus der Sichtbarkeit
  // Zeige nur wenn Lieferketten vorhanden sind
  if (totalChains === 0) return null

  return (
    <>
      {/* Blur Overlay - nur wenn erweitert */}
      {!isCollapsed && (
        <div className="fixed inset-0 z-25 bg-black/5 backdrop-blur-sm transition-all duration-300 animate-in fade-in" />
      )}

      <div style={getPositionStyle()}>
        <div className="animate-in slide-in-from-right-4 duration-300">
          {isCollapsed ? (
            /* Collapsed State */
            <div className="bg-white/95 backdrop-blur-md border border-gray-200/80 rounded-xl shadow-2xl transition-all duration-300 hover:shadow-3xl w-12">
              {/* Floating Indicator */}
              <div className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-blue-500 rounded-full animate-pulse shadow-lg"></div>

              <div className="p-2 flex flex-col items-center gap-2">
                {/* Truck Icon mit Badge */}
                <Button
                  onClick={handleExpand}
                  variant="ghost"
                  size="sm"
                  className="w-8 h-8 p-0 rounded-lg relative transition-colors hover:bg-blue-100 text-blue-600"
                  title={`${totalDeliveries} Lieferung${totalDeliveries !== 1 ? "en" : ""} in ${totalChains} Kette${totalChains !== 1 ? "n" : ""}`}
                >
                  <Truck className="h-3 w-3" />

                  {/* Badge zeigt Anzahl der Lieferungen */}
                  <Badge
                    variant="secondary"
                    className="absolute -top-1 -right-1 text-xs px-1 py-0 h-auto min-w-[16px] flex items-center justify-center border bg-blue-100 text-blue-800 border-blue-300"
                  >
                    {totalDeliveries}
                  </Badge>
                </Button>
              </div>
            </div>
          ) : (
            /* Expanded State */
            <div
              ref={modalRef}
              className="bg-white/95 backdrop-blur-xl border border-gray-200/80 rounded-xl shadow-2xl ring-1 ring-black/5 transition-all duration-300 relative min-w-80 max-w-[calc(100vw-2rem)]"
            >
              {/* Floating Indicator */}
              <div className="absolute -top-1 -right-1 w-3 h-3 bg-blue-400 rounded-full animate-pulse shadow-lg z-10 ring-2 ring-white/50"></div>

              {/* Header */}
              <div className="flex items-center justify-between p-3 border-b border-gray-100/50">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-blue-400 rounded-full animate-pulse"></div>
                  <span className="text-xs font-semibold text-gray-800">Lieferungen</span>
                  <Badge
                    variant="outline"
                    className="text-xs px-2 py-0.5 h-auto text-gray-700 border-gray-300 bg-white/60"
                  >
                    {totalChains} Kette{totalChains !== 1 ? "n" : ""}
                  </Badge>
                  <Badge
                    variant="outline"
                    className="text-xs px-2 py-0.5 h-auto text-blue-700 border-blue-300 bg-blue-50/60"
                  >
                    {totalDeliveries} Lieferung{totalDeliveries !== 1 ? "en" : ""}
                  </Badge>
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

              {/* Content */}
              <div className="max-h-[60vh] overflow-y-auto">
                <ScrollArea className="h-full">
                  <div className="p-3 space-y-4">
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

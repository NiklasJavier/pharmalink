"use client"

import { useState, useEffect, useRef } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { History, Clock, AlertCircle, CheckCircle2, Loader2, X, Copy, Check } from "lucide-react"
import { fetchKeyHistory, type HistoryResponse } from "@/lib/history-service"
import { cn } from "@/lib/utils"

interface HistoryModalProps {
  isOpen: boolean
  onClose: () => void
  productId: string
  keyPath: string
  keyName: string
  currentValue: string
  triggerElement?: HTMLElement | null
  // Neue Prop für den Zustand des JSON-Operations Panels
  jsonOperationsPanelMinimized?: boolean
}

export function HistoryModal({
  isOpen,
  onClose,
  productId,
  keyPath,
  keyName,
  currentValue,
  triggerElement,
  jsonOperationsPanelMinimized = true, // Default: minimiert
}: HistoryModalProps) {
  const [historyData, setHistoryData] = useState<HistoryResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [copiedValue, setCopiedValue] = useState<string | null>(null) // Für individuelle Werte
  const modalRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (isOpen && keyPath) {
      setLoading(true)
      setError(null)

      fetchKeyHistory(productId, keyPath)
        .then((data) => {
          setHistoryData(data)
        })
        .catch((err) => {
          setError(err.message || "Fehler beim Laden der History-Daten")
        })
        .finally(() => {
          setLoading(false)
        })
    }
  }, [isOpen, productId, keyPath])

  // Click outside detection
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (isOpen && modalRef.current && !modalRef.current.contains(event.target as Node)) {
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside)
      return () => {
        document.removeEventListener("mousedown", handleClickOutside)
      }
    }
  }, [isOpen, onClose])

  // ESC-Taste zum Schließen
  useEffect(() => {
    const handleEscapeKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && isOpen) {
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener("keydown", handleEscapeKey)
      return () => {
        document.removeEventListener("keydown", handleEscapeKey)
      }
    }
  }, [isOpen, onClose])

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp)
    return date.toLocaleString("de-DE", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  const copyKeyPath = async () => {
    try {
      await navigator.clipboard.writeText(keyPath)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error("Failed to copy key path:", err)
    }
  }

  // Neue Funktion zum Kopieren von History-Werten
  const copyHistoryValue = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value)
      setCopiedValue(value)
      setTimeout(() => setCopiedValue(null), 2000)
    } catch (err) {
      console.error("Failed to copy history value:", err)
    }
  }

  const getChangeIcon = (index: number, total: number) => {
    if (index === 0) {
      return <CheckCircle2 className="h-3 w-3 text-emerald-600" />
    }
    return <Clock className="h-3 w-3 text-blue-600" />
  }

  const getChangeColor = (index: number, total: number) => {
    if (index === 0) {
      return "border-emerald-200 bg-emerald-50"
    }
    return "border-blue-200 bg-blue-50"
  }

  // Funktion zum Kürzen des Key-Pfads
  const formatKeyPath = (path: string) => {
    const maxLength = 30
    if (path.length <= maxLength) return path

    const parts = path.split(".")
    if (parts.length <= 2) {
      // Wenn nur 1-2 Teile, einfach kürzen
      return path.length > maxLength ? `${path.substring(0, maxLength - 3)}...` : path
    }

    // Zeige ersten und letzten Teil mit ... dazwischen
    const first = parts[0]
    const last = parts[parts.length - 1]
    const shortened = `${first}...${last}`

    return shortened.length > maxLength ? `${shortened.substring(0, maxLength - 3)}...` : shortened
  }

  // Ändere die getPositionStyle Funktion für bessere Positionierung neben den gestapelten Modals
  const getPositionStyle = () => {
    if (jsonOperationsPanelMinimized) {
      // JSON-Operations Panel ist minimiert - positioniere links neben dem Stack
      return {
        bottom: "1rem",
        right: "calc(1rem + 3rem + 0.5rem)", // Rechts neben dem minimierten JSON-Panel
        maxHeight: "70vh",
      }
    } else {
      // JSON-Operations Panel ist erweitert - positioniere links neben dem erweiterten Panel
      return {
        bottom: "1rem",
        right: "calc(1rem + 20rem + 0.5rem)", // Rechts neben dem erweiterten JSON-Panel
        maxHeight: "70vh",
      }
    }
  }

  if (!isOpen) return null

  return (
    <>
      {/* Remove or comment out this entire blur overlay section: */}
      {/* Blur Overlay für bessere Hervorhebung */}
      {/* <div className="fixed inset-0 z-45 bg-black/10 backdrop-blur-sm transition-all duration-300 animate-in fade-in" /> */}

      {/* Dynamische Positionierung basierend auf JSON-Operations Panel Zustand */}
      <div className="fixed z-50 w-96 max-w-[calc(100vw-6rem)]" style={getPositionStyle()}>
        <div ref={modalRef} className="animate-in slide-in-from-left-4 duration-300">
          {/* Main Modal Content - verstärkt hervorgehoben mit abgerundeten Ecken */}
          <div className="bg-white/98 backdrop-blur-xl border border-gray-200/90 rounded-xl shadow-2xl ring-1 ring-black/10 transition-all duration-300 hover:shadow-3xl flex flex-col relative overflow-hidden">
            {/* Floating Indicator - verstärkt */}
            <div className="absolute -top-1 -right-1 w-3 h-3 bg-emerald-500 rounded-full animate-pulse shadow-lg z-10 ring-2 ring-white/60"></div>

            {/* Header - verstärkt mit weißem Hintergrund */}
            <div className="flex items-center justify-between p-3 border-b border-gray-100/60 bg-white/95 rounded-t-xl">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></div>
                <span className="text-xs font-semibold text-gray-800">Änderungshistorie</span>
                <Badge
                  variant="outline"
                  className="font-mono text-[0.6rem] px-1.5 py-0 h-auto bg-white/80 border-gray-300/80"
                >
                  {keyName}
                </Badge>
              </div>

              <Button
                onClick={onClose}
                variant="ghost"
                size="sm"
                className="w-8 h-8 p-0 hover:bg-gray-100 rounded-lg"
                title="Schließen (ESC)"
              >
                <X className="h-3 w-3" />
              </Button>
            </div>

            {/* Key Path Section - verstärkt */}
            <div className="px-3 py-2 bg-gray-50/90 border-b border-gray-100/60">
              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-gray-700 min-w-0 flex-shrink-0">Pfad:</span>
                <Button
                  onClick={copyKeyPath}
                  variant="ghost"
                  size="sm"
                  className="h-auto p-1.5 hover:bg-white/80 rounded-md transition-all duration-200 group min-w-0 flex-1"
                  title={`Klicken zum Kopieren: ${keyPath}`}
                >
                  <div className="flex items-center gap-1.5 min-w-0">
                    <span className="text-[0.7rem] text-gray-800 font-mono truncate group-hover:text-emerald-600 transition-colors">
                      {formatKeyPath(keyPath)}
                    </span>
                    <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex-shrink-0">
                      {copied ? (
                        <Check className="h-3 w-3 text-emerald-600" />
                      ) : (
                        <Copy className="h-3 w-3 text-gray-500" />
                      )}
                    </div>
                  </div>
                </Button>
                {copied && (
                  <Badge
                    variant="secondary"
                    className="text-xs px-1.5 py-0.5 h-auto bg-emerald-100 text-emerald-700 animate-in fade-in duration-200"
                  >
                    Kopiert
                  </Badge>
                )}
              </div>
            </div>

            {/* Content - verstärkt mit abgerundeten unteren Ecken */}
            <div className="relative flex-1 min-h-0 bg-white/95 rounded-b-xl">
              {loading && (
                <div className="flex items-center justify-center py-4">
                  <div className="flex items-center gap-2">
                    <Loader2 className="h-3 w-3 animate-spin text-emerald-600" />
                    <span className="text-xs text-gray-700">Lade Historie...</span>
                  </div>
                </div>
              )}

              {error && (
                <div className="p-3">
                  <div className="flex items-center gap-2 p-2 bg-red-50/90 border border-red-200/60 rounded-lg">
                    <AlertCircle className="h-3 w-3 text-red-600 flex-shrink-0" />
                    <div>
                      <p className="text-xs font-semibold text-red-900">Fehler beim Laden</p>
                      <p className="text-xs text-red-700">{error}</p>
                    </div>
                  </div>
                </div>
              )}

              {historyData && !loading && !error && (
                <div className="h-full rounded-b-xl overflow-hidden">
                  {historyData.history.length > 0 ? (
                    <div className="p-3">
                      <ScrollArea className="h-[45vh] min-h-[250px] max-h-[350px]">
                        <div className="relative pr-2">
                          {/* Timeline Line - verstärkt */}
                          <div className="absolute left-3 top-3 bottom-3 w-0.5 bg-gray-300"></div>

                          <div className="space-y-2 pb-3">
                            {historyData.history.map((entry, index) => {
                              const timestamp = formatTimestamp(entry.timestamp)
                              const isLatest = index === 0
                              const isCopied = copiedValue === entry.value

                              return (
                                <div key={index} className="relative flex gap-2">
                                  {/* Timeline Icon - verstärkt */}
                                  <div
                                    className={cn(
                                      "flex-shrink-0 w-6 h-6 rounded-full border-2 flex items-center justify-center bg-white shadow-md z-10",
                                      isLatest ? "border-emerald-400" : "border-blue-400",
                                    )}
                                  >
                                    {getChangeIcon(index, historyData.history.length)}
                                  </div>

                                  {/* Content Card - verstärkt */}
                                  <div
                                    className={cn(
                                      "flex-1 rounded-lg border-2 p-2 shadow-md backdrop-blur-sm",
                                      getChangeColor(index, historyData.history.length),
                                    )}
                                  >
                                    {/* Header - verstärkt */}
                                    <div className="flex items-center justify-between mb-1">
                                      <div className="flex items-center gap-1">
                                        <span className="text-xs font-medium text-gray-900">
                                          {isLatest
                                            ? "Aktuelle Version"
                                            : `Version ${historyData.history.length - index}`}
                                        </span>
                                        {isLatest && (
                                          <Badge
                                            variant="secondary"
                                            className="bg-emerald-100 text-emerald-800 text-xs px-2 py-0.5 h-auto border border-emerald-300"
                                          >
                                            Aktuell
                                          </Badge>
                                        )}
                                      </div>
                                    </div>

                                    {/* Timestamp - verstärkt */}
                                    <div className="flex items-center gap-1 text-xs text-gray-700 mb-1">
                                      <Clock className="h-3 w-3" />
                                      {timestamp}
                                    </div>

                                    {/* Value - verstärkt und klickbar */}
                                    <div className="relative group">
                                      <Button
                                        onClick={() => copyHistoryValue(entry.value)}
                                        variant="ghost"
                                        className={cn(
                                          "w-full h-auto p-2 text-left font-mono text-[0.65rem] rounded-lg border-2 transition-all duration-200 justify-start",
                                          isCopied
                                            ? "bg-emerald-100 border-emerald-400 text-emerald-900"
                                            : "bg-white/90 border-gray-300/80 hover:bg-gray-50 hover:border-gray-400",
                                        )}
                                        title="Klicken zum Kopieren"
                                      >
                                        <div className="flex items-center justify-between w-full gap-2">
                                          <span className="break-all flex-1 text-left">{entry.value}</span>
                                          <div
                                            className={cn(
                                              "flex-shrink-0 transition-opacity duration-200",
                                              isCopied ? "opacity-100" : "opacity-0 group-hover:opacity-100",
                                            )}
                                          >
                                            {isCopied ? (
                                              <Check className="h-3 w-3 text-emerald-600" />
                                            ) : (
                                              <Copy className="h-3 w-3 text-gray-500" />
                                            )}
                                          </div>
                                        </div>
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              )
                            })}
                          </div>
                        </div>
                      </ScrollArea>
                    </div>
                  ) : (
                    <div className="text-center py-4 px-3">
                      <div className="flex flex-col items-center gap-2">
                        <div className="p-2 bg-gray-100/90 rounded-lg shadow-sm border border-gray-200/80">
                          <History className="h-3 w-3 text-gray-600" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-gray-900">Keine Änderungshistorie</p>
                          <p className="text-xs text-gray-600">
                            Für diesen Schlüssel sind keine historischen Änderungen verfügbar.
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Innerer Glanz-Effekt */}
            <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-white/40 via-transparent to-transparent pointer-events-none"></div>
          </div>
        </div>
      </div>

      {/* Responsive Fallback für kleinere Bildschirme */}
      <style jsx>{`
        @media (max-width: 1400px) {
          .fixed[style*="right: calc(1rem + "] {
            right: 1rem !important;
            left: 1rem !important;
            width: auto !important;
            max-width: calc(100vw - 2rem) !important;
            bottom: 1rem !important;
          }
        }
      `}</style>
    </>
  )
}

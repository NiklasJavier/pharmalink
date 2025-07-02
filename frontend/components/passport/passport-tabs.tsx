"use client"

import { cn } from "@/lib/utils"
import { Building, Pill, Box, ArrowLeft } from "lucide-react"
import React, { useState } from "react"
import type { JSX } from "react/jsx-runtime"

export type Tab = "hersteller" | "medikament" | "einheit"

type PassportTabsProps = {
  activeTab: Tab
  onTabChange: (tab: Tab) => void
  compact?: boolean
  onSearch?: (query: string, type: string) => void
  linkedIds?: {
    medikament?: string
    hersteller?: string
    unit?: string[]
  }
}

const tabsConfig: Record<Tab, { label: string; icon: JSX.Element; description: string }> = {
  hersteller: {
    label: "Hersteller",
    icon: <Building className="h-3 w-3" />,
    description: "Hersteller Informationen - Produktionsstandort, Herstellungsdatum und Chargenfreigabe",
  },
  medikament: {
    label: "Medikament",
    icon: <Pill className="h-3 w-3" />,
    description: "Medikamenten Informationen - Wirkstoffe, Darreichungsform und Verfallsdatum",
  },
  einheit: {
    label: "Einheit",
    icon: <Box className="h-3 w-3" />,
    description: "Unit Informationen - Lieferkette, Transportwege und Dokumentation",
  },
}

const tabOrder: Tab[] = ["hersteller", "medikament", "einheit"]

export function PassportTabs({ activeTab, onTabChange, compact = false, onSearch, linkedIds }: PassportTabsProps) {
  const [hoveredTab, setHoveredTab] = useState<Tab | null>(null)

  const getTabIndex = (tab: Tab) => tabOrder.indexOf(tab)
  const activeIndex = getTabIndex(activeTab)

  // Navigation zu verknüpften IDs
  const handleTabClick = (tab: Tab) => {
    // Wenn es nicht der aktive Tab ist und eine verknüpfte ID existiert, navigiere direkt
    if (activeTab !== tab && onSearch && linkedIds) {
      if (tab === "medikament" && linkedIds.medikament) {
        onSearch(linkedIds.medikament, "")
        return
      } else if (tab === "hersteller" && linkedIds.hersteller) {
        onSearch(linkedIds.hersteller, "")
        return
      } else if (tab === "einheit" && linkedIds.unit && linkedIds.unit.length > 0) {
        // Navigiere zur ersten verknüpften Unit
        onSearch(linkedIds.unit[0], "")
        return
      }
    }

    // Wenn es der aktive Tab ist, mache nichts
    if (activeTab === tab) {
      return
    }

    // Fallback: normaler Tab-Wechsel (falls keine verknüpfte ID vorhanden)
    onTabChange(tab)
  }

  // Prüfe ob Tab verfügbar ist (hat verknüpfte ID)
  const isTabAvailable = (tab: Tab): boolean => {
    if (tab === activeTab) return true // Aktiver Tab ist immer verfügbar
    if (!linkedIds) return false

    switch (tab) {
      case "medikament":
        return !!linkedIds.medikament
      case "hersteller":
        return !!linkedIds.hersteller
      case "einheit":
        return !!(linkedIds.unit && linkedIds.unit.length > 0)
      default:
        return false
    }
  }

  // Funktion zur intelligenten Tooltip-Positionierung
  const getTooltipPosition = (tabKey: Tab, tabIndex: number) => {
    if (tabKey === "einheit") {
      return "absolute top-full right-0 mt-3 z-50"
    } else if (tabKey === "hersteller") {
      return "absolute top-full left-0 mt-3 z-50"
    } else {
      return "absolute top-full left-1/2 transform -translate-x-1/2 mt-3 z-50"
    }
  }

  // Funktion für Bubble-Pfeil-Positionierung
  const getBubbleArrowPosition = (tabKey: Tab) => {
    if (tabKey === "einheit") {
      return "absolute bottom-full right-6"
    } else if (tabKey === "hersteller") {
      return "absolute bottom-full left-6"
    } else {
      return "absolute bottom-full left-1/2 transform -translate-x-1/2"
    }
  }

  // Tooltip-Beschreibung
  const getTooltipDescription = (tabKey: Tab) => {
    const baseDescription = tabsConfig[tabKey].description
    const isActive = activeTab === tabKey

    if (isActive) {
      return `${baseDescription} - Aktuell angezeigte Daten`
    } else {
      return baseDescription
    }
  }

  return (
      <div className="relative">
        {/* Nur die Tabs, keine Suchfunktion */}
        <div className="flex items-center bg-white/80 backdrop-blur-xl border border-white/40 rounded-xl shadow-2xl transition-all duration-200 p-1 ring-1 ring-black/5">
          {/* Innerer Glanz-Effekt */}
          <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-white/30 via-transparent to-transparent pointer-events-none"></div>

          <div className="relative flex items-center gap-1">
            {/* Passport Tabs */}
            {tabOrder.map((key, index) => {
              const isAvailable = isTabAvailable(key)
              const isActive = activeTab === key

              return (
                  <React.Fragment key={key}>
                    <div className="relative">
                      <button
                          onClick={() => handleTabClick(key)}
                          onMouseEnter={() => setHoveredTab(key)}
                          onMouseLeave={() => setHoveredTab(null)}
                          className={cn(
                              "relative flex items-center justify-center rounded-lg border transition-all duration-200 group w-8 h-8 backdrop-blur-sm",
                              isActive
                                  ? "bg-emerald-500 border-emerald-500 text-white shadow-lg hover:bg-emerald-600 cursor-default"
                                  : isAvailable
                                      ? "bg-white/60 border-gray-300/60 text-black cursor-pointer hover:bg-white/80 hover:border-gray-400/60"
                                      : "bg-white/40 border-gray-300/40 text-gray-400 cursor-default",
                          )}
                          aria-current={isActive ? "page" : undefined}
                          disabled={!isActive && !isAvailable}
                          title={
                            !isActive && !isAvailable
                                ? "Nur zur Information - nicht für diese ID verfügbar"
                                : isActive
                                    ? "Aktuell angezeigte Daten"
                                    : "Verfügbar"
                          }
                      >
                        <div
                            className={cn("transition-transform", isActive ? "" : isAvailable ? "group-hover:scale-110" : "")}
                        >
                          {tabsConfig[key].icon}
                        </div>
                      </button>

                      {/* Tooltip */}
                      {hoveredTab === key && (
                          <div className={getTooltipPosition(key, index)}>
                            <div className="relative">
                              {/* Hauptblase */}
                              <div
                                  className={cn(
                                      "text-black text-sm rounded-2xl px-6 py-4 shadow-2xl w-64 max-w-md relative ring-1 backdrop-blur-xl",
                                      isActive
                                          ? "bg-white/90 border-2 border-emerald-200/60 ring-emerald-100/40"
                                          : isAvailable
                                              ? "bg-white/90 border-2 border-gray-200/60 ring-gray-100/40"
                                              : "bg-white/80 border-2 border-gray-200/60 ring-gray-100/40",
                                  )}
                              >
                                <div
                                    className={cn(
                                        "font-bold mb-3 text-base",
                                        isActive ? "text-emerald-700" : isAvailable ? "text-black" : "text-gray-600",
                                    )}
                                >
                                  {tabsConfig[key].label}
                                  {isActive && <span className="text-xs font-normal text-emerald-600 ml-2">(Aktiv)</span>}
                                  {!isActive && !isAvailable && (
                                      <span className="text-xs font-normal text-gray-500 ml-2">(Nur Info)</span>
                                  )}
                                  {isAvailable && !isActive && (
                                      <span className="text-xs font-normal text-gray-500 ml-2">(Verfügbar)</span>
                                  )}
                                </div>
                                <div
                                    className={cn(
                                        "text-sm leading-relaxed font-medium",
                                        isActive ? "text-gray-700" : isAvailable ? "text-black" : "text-gray-500",
                                    )}
                                >
                                  {getTooltipDescription(key)}
                                </div>

                                {/* Innerer Glanz-Effekt */}
                                <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-white/40 via-transparent to-transparent pointer-events-none"></div>
                              </div>

                              {/* Bubble-Pfeil */}
                              <div className={getBubbleArrowPosition(key)}>
                                {/* Äußerer Schatten */}
                                <div className="absolute -top-1 left-1/2 transform -translate-x-1/2">
                                  <div className="w-0 h-0 border-l-[14px] border-r-[14px] border-b-[16px] border-l-transparent border-r-transparent border-b-gray-400/30 blur-sm"></div>
                                </div>

                                {/* Hauptpfeil */}
                                <div className="relative">
                                  <div
                                      className={cn(
                                          "w-0 h-0 border-l-[10px] border-r-[10px] border-b-[12px] border-l-transparent border-r-transparent",
                                          isActive
                                              ? "border-b-emerald-200/60"
                                              : isAvailable
                                                  ? "border-b-gray-200/60"
                                                  : "border-b-gray-200/60",
                                      )}
                                  ></div>
                                </div>

                                {/* Innerer Pfeil */}
                                <div className="absolute top-0.5 left-1/2 transform -translate-x-1/2">
                                  <div
                                      className={cn(
                                          "w-0 h-0 border-l-[10px] border-r-[10px] border-b-[12px] border-l-transparent border-r-transparent",
                                          isActive || isAvailable ? "border-b-white/95" : "border-b-white/85",
                                      )}
                                  ></div>
                                </div>

                                {/* Glanz-Highlight */}
                                <div className="absolute top-1 left-1/2 transform -translate-x-1/2">
                                  <div className="w-0 h-0 border-l-[6px] border-r-[6px] border-b-[8px] border-l-transparent border-r-transparent border-b-white/70"></div>
                                </div>
                              </div>

                              {/* Pulsierender Indikator für verfügbare Tabs */}
                              {isAvailable && !isActive && (
                                  <div
                                      className={cn(
                                          "absolute w-3 h-3 rounded-full animate-pulse",
                                          key === "einheit"
                                              ? "bottom-full right-6 mb-2"
                                              : key === "hersteller"
                                                  ? "bottom-full left-6 mb-2"
                                                  : "bottom-full left-1/2 transform -translate-x-1/2 mb-2",
                                          "bg-gray-400/60",
                                      )}
                                  >
                                    <div className={cn("absolute inset-0 rounded-full animate-ping", "bg-gray-500/40")}></div>
                                  </div>
                              )}
                            </div>
                          </div>
                      )}
                    </div>

                    {/* Arrow Separators */}
                    {index < tabOrder.length - 1 && (
                        <div className="flex items-center justify-center">
                          <div
                              className={cn(
                                  "flex items-center justify-center rounded-full transition-all duration-200 w-5 h-5 backdrop-blur-sm",
                                  index < activeIndex
                                      ? "bg-emerald-100/80 text-emerald-600 shadow-sm"
                                      : "bg-white/60 text-gray-400 border border-gray-300/50",
                              )}
                          >
                            <ArrowLeft className="h-2.5 w-2.5" />
                          </div>
                        </div>
                    )}
                  </React.Fragment>
              )
            })}
          </div>
        </div>
      </div>
  )
}

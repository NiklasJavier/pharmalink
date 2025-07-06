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
    description: "Produktionsstandort & Herstellungsdaten",
  },
  medikament: {
    label: "Medikament",
    icon: <Pill className="h-3 w-3" />,
    description: "Wirkstoffe & Darreichungsform",
  },
  einheit: {
    label: "Einheit",
    icon: <Box className="h-3 w-3" />,
    description: "Lieferkette & Transportwege",
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
      // Für das letzte Element: immer nach links ausrichten um Abschneiden zu vermeiden
      return "absolute top-full right-0 mt-2 z-50"
    } else if (tabKey === "hersteller") {
      // Für das erste Element: nach links ausrichten
      return "absolute top-full left-0 mt-2 z-50"
    } else {
      // Für das mittlere Element: zentriert
      return "absolute top-full left-1/2 transform -translate-x-1/2 mt-2 z-50"
    }
  }

  // Funktion für Bubble-Pfeil-Positionierung
  const getBubbleArrowPosition = (tabKey: Tab) => {
    if (tabKey === "einheit") {
      // Pfeil weiter links positionieren da Tooltip rechts ausgerichtet ist
      return "absolute bottom-full right-6"
    } else if (tabKey === "hersteller") {
      // Pfeil weiter rechts positionieren da Tooltip links ausgerichtet ist
      return "absolute bottom-full left-6"
    } else {
      // Zentriert für mittleres Element
      return "absolute bottom-full left-1/2 transform -translate-x-1/2"
    }
  }

  return (
    <div className="relative">
      {/* Nur die Tabs, keine Suchfunktion */}
      <div className="flex items-center bg-white border border-gray-200 rounded-xl transition-all duration-200 p-1">
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
                      "relative flex items-center justify-center rounded-lg border transition-all duration-200 group w-8 h-8",
                      isActive
                        ? "bg-emerald-500 border-emerald-500 text-white hover:bg-emerald-600 cursor-default"
                        : isAvailable
                          ? "bg-white border-gray-300 text-black cursor-pointer hover:bg-gray-50 hover:border-gray-400"
                          : "bg-white border-gray-300 text-gray-400 cursor-default",
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

                  {/* Kompakter Tooltip */}
                  {hoveredTab === key && (
                    <div className={getTooltipPosition(key, index)}>
                      <div className="relative">
                        {/* Kompakte Hauptblase */}
                        <div
                          className={cn(
                            "text-gray-800 text-xs rounded-lg px-3 py-2 shadow-lg relative border bg-white",
                            // Responsive Breite: kleiner auf mobilen Geräten
                            "w-44 sm:w-48",
                            isActive ? "border-emerald-300" : isAvailable ? "border-gray-300" : "border-gray-200",
                          )}
                        >
                          <div
                            className={cn(
                              "font-semibold mb-1 text-xs",
                              isActive ? "text-emerald-700" : isAvailable ? "text-gray-800" : "text-gray-600",
                            )}
                          >
                            {tabsConfig[key].label}
                            {isActive && <span className="text-xs font-normal text-emerald-600 ml-1">(Aktiv)</span>}
                            {!isActive && !isAvailable && (
                              <span className="text-xs font-normal text-gray-500 ml-1">(Info)</span>
                            )}
                          </div>
                          <div
                            className={cn(
                              "text-xs leading-snug",
                              isActive ? "text-gray-700" : isAvailable ? "text-gray-700" : "text-gray-500",
                            )}
                          >
                            {tabsConfig[key].description}
                          </div>
                        </div>

                        {/* Kompakter Bubble-Pfeil */}
                        <div className={getBubbleArrowPosition(key)}>
                          {/* Hauptpfeil */}
                          <div className="relative">
                            <div
                              className={cn(
                                "w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent",
                                isActive
                                  ? "border-b-emerald-300"
                                  : isAvailable
                                    ? "border-b-gray-300"
                                    : "border-b-gray-200",
                              )}
                            ></div>
                          </div>

                          {/* Innerer Pfeil */}
                          <div className="absolute top-0.5 left-1/2 transform -translate-x-1/2">
                            <div className="w-0 h-0 border-l-[7px] border-r-[7px] border-b-[7px] border-l-transparent border-r-transparent border-b-white"></div>
                          </div>
                        </div>

                        {/* Kleiner Indikator für verfügbare Tabs */}
                        {isAvailable && !isActive && (
                          <div
                            className={cn(
                              "absolute w-2 h-2 rounded-full animate-pulse",
                              key === "einheit"
                                ? "bottom-full right-4 mb-1"
                                : key === "hersteller"
                                  ? "bottom-full left-4 mb-1"
                                  : "bottom-full left-1/2 transform -translate-x-1/2 mb-1",
                              "bg-emerald-400",
                            )}
                          >
                            <div className="absolute inset-0 rounded-full animate-ping bg-emerald-500/40"></div>
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
                        "flex items-center justify-center rounded-full transition-all duration-200 w-5 h-5",
                        index < activeIndex
                          ? "bg-emerald-100 text-emerald-600"
                          : "bg-white text-gray-400 border border-gray-300",
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

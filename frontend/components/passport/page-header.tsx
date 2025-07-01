"use client"

import type React from "react"
import { Building, Pill, Box, Copy, Check } from "lucide-react"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

type PageHeaderProps = {
  id: string
  originalId?: string // Falls ID korrigiert wurde
  children?: React.ReactNode
}

export function PageHeader({ id, originalId, children }: PageHeaderProps) {
  const [copied, setCopied] = useState(false)

  const getHeaderInfo = (id: string) => {
    if (id.startsWith("MED-")) {
      return {
        title: "Medikament",
        subtitle: "Medikamenten-ID:",
        icon: <Pill className="h-5 w-5 text-black" />,
        textColor: "text-black",
      }
    } else if (id.startsWith("HERSTELLER-")) {
      return {
        title: "Hersteller",
        subtitle: "Hersteller-ID:",
        icon: <Building className="h-5 w-5 text-black" />,
        textColor: "text-black",
      }
    } else if (id.startsWith("UNIT-")) {
      return {
        title: "Unit",
        subtitle: "Unit-ID:",
        icon: <Box className="h-5 w-5 text-black" />,
        textColor: "text-black",
      }
    } else {
      // Fallback für andere IDs
      return {
        title: "Unbekannt",
        subtitle: "ID:",
        icon: <Box className="h-5 w-5 text-black" />,
        textColor: "text-black",
      }
    }
  }

  const copyToClipboard = async () => {
    try {
      // Aktuelle URL mit allen Parametern kopieren
      const urlToCopy = window.location.href
      await navigator.clipboard.writeText(urlToCopy)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error("Failed to copy: ", err)
    }
  }

  const headerInfo = getHeaderInfo(id)

  return (
    <div className="mb-4 md:mb-6">
      {/* Header Card - nur halbe Breite */}
      <div className="bg-white border border-gray-200 rounded-lg p-4 md:p-6 max-w-2xl">
        <div className="flex items-start justify-between gap-2 md:gap-4">
          {/* Left side - Title and ID */}
          <div className="flex items-center gap-2 md:gap-3">
            <div className="flex-shrink-0">{headerInfo.icon}</div>
            <div className="flex-1">
              <Link href="/" className="group">
                <h1
                  className={`text-lg md:text-xl font-bold ${headerInfo.textColor} mb-1 group-hover:text-emerald-600 transition-colors cursor-pointer`}
                >
                  {headerInfo.title}
                </h1>
              </Link>
              <div className="flex items-center gap-1 md:gap-2 text-xs md:text-sm text-gray-600">
                <span className="font-medium">{headerInfo.subtitle}</span>
                <Button
                  onClick={copyToClipboard}
                  variant="ghost"
                  size="sm"
                  className="h-auto p-1 hover:bg-gray-100 rounded-md transition-all duration-200 group"
                  title="Vollständige URL mit allen Parametern kopieren"
                >
                  <span className="font-mono text-gray-800 group-hover:text-emerald-600 transition-colors text-xs md:text-sm">
                    {id}
                  </span>
                  <div className="ml-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                    {copied ? (
                      <Check className="h-3 w-3 text-emerald-600" />
                    ) : (
                      <Copy className="h-3 w-3 text-gray-500" />
                    )}
                  </div>
                </Button>
              </div>

              {/* Zeige ursprüngliche ID falls korrigiert */}
              {originalId && originalId !== id && (
                <div className="mt-1 text-xs text-gray-500">
                  Ursprünglich gesucht: <code className="bg-gray-100 px-1 rounded">{originalId}</code>
                </div>
              )}

              {copied && (
                <div className="mt-1 text-xs text-emerald-600 font-medium animate-in fade-in duration-200">
                  Vollständige URL kopiert! (inkl. Such- und Anzeigeoptionen)
                </div>
              )}
            </div>
          </div>

          {/* Right side - Operations */}
          {children && <div className="flex-shrink-0">{children}</div>}
        </div>
      </div>
    </div>
  )
}

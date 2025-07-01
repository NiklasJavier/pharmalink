"use client"

import { useSearchParams } from "next/navigation"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Link, Eye, Search, Hash, Bell, Truck } from "lucide-react"

export function UrlParamsDisplay() {
  const searchParams = useSearchParams()

  const params = Array.from(searchParams.entries()).filter(([key]) =>
    ["search", "expanded", "resultIndex", "history", "meldungen", "lieferungen"].includes(key),
  )

  if (params.length === 0) return null

  const getParamIcon = (key: string) => {
    switch (key) {
      case "search":
        return <Search className="h-3 w-3" />
      case "expanded":
        return <Eye className="h-3 w-3" />
      case "resultIndex":
        return <Hash className="h-3 w-3" />
      case "history":
        return <Link className="h-3 w-3" />
      case "meldungen":
        return <Bell className="h-3 w-3" />
      case "lieferungen":
        return <Truck className="h-3 w-3" />
      default:
        return <Link className="h-3 w-3" />
    }
  }

  const getParamDescription = (key: string, value: string) => {
    switch (key) {
      case "search":
        return `Suchbegriff: "${value}"`
      case "expanded":
        return `Alle ${value === "true" ? "ausgeklappt" : "eingeklappt"}`
      case "resultIndex":
        return `Suchergebnis #${Number.parseInt(value) + 1}`
      case "history":
        return `Historie: ${value}`
      case "meldungen":
        return `Meldungen ${value === "open" ? "geöffnet" : "geschlossen"}`
      case "lieferungen":
        return `Lieferungen ${value === "open" ? "geöffnet" : "geschlossen"}`
      default:
        return `${key}: ${value}`
    }
  }

  return (
    <Card className="mb-4 bg-white border-gray-200">
      <CardContent className="p-3">
        <div className="flex items-center gap-2 mb-2">
          <Link className="h-4 w-4 text-gray-700" />
          <span className="text-sm font-medium text-gray-900">Aktive URL-Parameter</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {params.map(([key, value]) => (
            <Badge
              key={key}
              variant="secondary"
              className="bg-gray-100 text-gray-900 hover:bg-gray-200 flex items-center gap-1"
              title={getParamDescription(key, value)}
            >
              {getParamIcon(key)}
              <span className="font-mono text-xs">
                {key}={value}
              </span>
            </Badge>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

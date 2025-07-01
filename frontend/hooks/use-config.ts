"use client"

import { useState, useEffect } from "react"
import type { ClientConfig } from "@/lib/config"

let configCache: ClientConfig | null = null

export function useConfig() {
  const [config, setConfig] = useState<ClientConfig | null>(configCache)
  const [loading, setLoading] = useState(!configCache)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (configCache) {
      setConfig(configCache)
      setLoading(false)
      return
    }

    fetch("/api/config")
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to fetch config")
        }
        return response.json()
      })
      .then((data: ClientConfig) => {
        configCache = data
        setConfig(data)
        setError(null)
      })
      .catch((err) => {
        console.error("Config fetch error:", err)
        setError(err.message)
        // Fallback config
        const fallback: ClientConfig = {
          app: {
            name: "PharmaLink",
            version: "v1.2.3",
            description: "Digitaler Produktpass für die pharmazeutische Lieferkette",
          },
          api: {
            documentation_url: "/api-docs",
            base_url: "/api/v1",
          },
          ui: {
            theme: {
              primary_color: "emerald",
              header_height: "20",
              mobile_header_height: "16",
            },
          },
          features: {
            qr_scanner: false,
            search_history: false,
            offline_mode: false,
          },
          links: {
            support_url: "https://vercel.com/help",
            privacy_url: "#",
            terms_url: "#",
          },
        }
        setConfig(fallback)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  return { config, loading, error }
}

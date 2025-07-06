"use client"

import { useState, useEffect } from "react"
import type { ClientConfig } from "@/lib/config"
import { configCache } from "@/lib/cache-service"

let configPromise: Promise<ClientConfig> | null = null

async function fetchConfig(): Promise<ClientConfig> {
  // Check cache first
  const cached = configCache.get<ClientConfig>("app_config")
  if (cached) {
    return cached
  }

  const response = await fetch("/api/config")
  if (!response.ok) {
    throw new Error("Failed to fetch config")
  }

  const config = await response.json()

  // Cache for 30 minutes
  configCache.set("app_config", config, 30 * 60 * 1000)

  return config
}

export function useOptimizedConfig() {
  const [config, setConfig] = useState<ClientConfig | null>(() => {
    // Try to get from cache immediately
    return configCache.get<ClientConfig>("app_config")
  })
  const [loading, setLoading] = useState(!config)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (config) {
      setLoading(false)
      return
    }

    // Use shared promise to avoid multiple requests
    if (!configPromise) {
      configPromise = fetchConfig()
    }

    configPromise
      .then((fetchedConfig) => {
        setConfig(fetchedConfig)
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
        configPromise = null // Reset for future requests
      })
  }, [config])

  return { config, loading, error }
}

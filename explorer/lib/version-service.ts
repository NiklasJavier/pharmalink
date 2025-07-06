"use client"

import React from "react"

import { getConfig } from "./config"

export interface BackendVersion {
  version: string
  timestamp: string
  status: "success" | "error"
}

let cachedVersion: BackendVersion | null = null
let lastFetch = 0
const CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

export async function fetchBackendVersion(): Promise<BackendVersion> {
  const now = Date.now()

  // Return cached version if still valid
  if (cachedVersion && now - lastFetch < CACHE_DURATION) {
    return cachedVersion
  }

  try {
    const config = getConfig()
    const response = await fetch(config.api.backend_version_url, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      // Add timeout
      signal: AbortSignal.timeout(5000),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    // Detect whether we should parse JSON or plain text
    const contentType = response.headers.get("content-type") ?? ""

    let raw: unknown
    if (contentType.includes("application/json")) {
      raw = await response.json().catch(() => undefined)
    } else {
      // Plain-text (or unknown) responses
      raw = await response.text()
      // Try to parse if it actually is JSON but had a wrong header
      if (typeof raw === "string") {
        try {
          raw = JSON.parse(raw)
        } catch {
          /* leave as string */
        }
      }
    }

    // Normalise the shape
    let version: string
    if (typeof raw === "string") {
      version = raw.trim()
    } else if (raw && typeof raw === "object" && "version" in (raw as any)) {
      version = (raw as any).version
    } else if (raw && typeof raw === "object" && "value" in (raw as any)) {
      version = (raw as any).value
    } else {
      throw new Error("Invalid response format for backend version")
    }

    cachedVersion = {
      version,
      timestamp: new Date().toISOString(),
      status: "success",
    }
    lastFetch = now

    return cachedVersion
  } catch (error) {
    console.error("Failed to fetch backend version:", error)

    const errorVersion: BackendVersion = {
      version: "unknown",
      timestamp: new Date().toISOString(),
      status: "error",
    }

    // Cache error for shorter duration
    if (!cachedVersion) {
      cachedVersion = errorVersion
      lastFetch = now - CACHE_DURATION + 30000 // Retry in 30 seconds
    }

    return cachedVersion || errorVersion
  }
}

// Hook for React components
export function useBackendVersion() {
  const [version, setVersion] = React.useState<BackendVersion | null>(null)
  const [loading, setLoading] = React.useState(true)

  React.useEffect(() => {
    fetchBackendVersion()
      .then(setVersion)
      .finally(() => setLoading(false))
  }, [])

  return { version, loading, refetch: () => fetchBackendVersion().then(setVersion) }
}

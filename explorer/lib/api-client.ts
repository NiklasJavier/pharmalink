"use client"

import type { ClientConfig } from "./config"

export interface ApiResponse<T> {
  data: T | null
  error: string | null
  source: "api" | "cache"
  timestamp: string
}

export interface ApiError {
  message: string
  status?: number
  code?: string
}

class ApiClient {
  private config: ClientConfig | null = null
  private cache = new Map<string, { data: any; timestamp: number }>()

  async initialize() {
    if (!this.config) {
      const response = await fetch("/api/config")
      this.config = await response.json()
    }
  }

  private async makeRequest<T>(url: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    await this.initialize()

    if (!this.config) {
      return {
        data: null,
        error: "Configuration not available",
        source: "api",
        timestamp: new Date().toISOString(),
      }
    }

    // Check cache first
    const cacheKey = `${url}:${JSON.stringify(options)}`
    const cached = this.cache.get(cacheKey)
    const now = Date.now()

    if (cached && now - cached.timestamp < this.config.data.cache_duration) {
      return {
        data: cached.data,
        error: null,
        source: "cache",
        timestamp: new Date(cached.timestamp).toISOString(),
      }
    }

    // Make API request with retry logic
    let lastError: ApiError | null = null

    for (let attempt = 1; attempt <= this.config.data.retry_attempts; attempt++) {
      try {
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), this.config!.data.timeout)

        const response = await fetch(url, {
          ...options,
          signal: controller.signal,
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...options.headers,
          },
        })

        clearTimeout(timeoutId)

        if (!response.ok) {
          throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status)
        }

        const data = await response.json()

        // Cache successful response
        this.cache.set(cacheKey, { data, timestamp: now })

        return {
          data,
          error: null,
          source: "api",
          timestamp: new Date().toISOString(),
        }
      } catch (error) {
        lastError = {
          message: error instanceof Error ? error.message : "Unknown error",
          status: error instanceof ApiError ? error.status : undefined,
        }

        // Wait before retry (exponential backoff)
        if (attempt < this.config.data.retry_attempts) {
          await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
        }
      }
    }

    return {
      data: null,
      error: lastError?.message || "Request failed",
      source: "api",
      timestamp: new Date().toISOString(),
    }
  }

  // Entferne getProductData und ersetze mit einheitlicher Search-Methode
  async searchData(query: string): Promise<ApiResponse<any>> {
    await this.initialize()

    if (!this.config) {
      return {
        data: null,
        error: "Configuration not available",
        source: "api",
        timestamp: new Date().toISOString(),
      }
    }

    // Einheitliche Search-URL für alle Abfragen (IDs oder Suchbegriffe)
    const url = `${this.config.api.search_url}?q=${encodeURIComponent(query)}`
    return this.makeRequest(url)
  }

  // Neue Methode für History-Abfragen
  async getHistory(id: string, keyPath: string): Promise<ApiResponse<any>> {
    await this.initialize()

    if (!this.config) {
      return {
        data: null,
        error: "Configuration not available",
        source: "api",
        timestamp: new Date().toISOString(),
      }
    }

    const url = `${this.config.api.history_url}?id=${encodeURIComponent(id)}&key=${encodeURIComponent(keyPath)}`
    return this.makeRequest(url)
  }

  clearCache() {
    this.cache.clear()
  }

  getCacheStats() {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys()),
    }
  }
}

class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
    public code?: string,
  ) {
    super(message)
    this.name = "ApiError"
  }
}

export const apiClient = new ApiClient()

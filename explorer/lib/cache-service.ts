// Erweiterte Cache-Service für bessere Performance

interface CacheEntry<T> {
  data: T
  timestamp: number
  expiresAt: number
}

interface CacheConfig {
  defaultTTL: number // Time to live in milliseconds
  maxSize: number // Maximum number of entries
  persistToStorage: boolean
}

class CacheService {
  private cache = new Map<string, CacheEntry<any>>()
  private config: CacheConfig

  constructor(config: Partial<CacheConfig> = {}) {
    this.config = {
      defaultTTL: 5 * 60 * 1000, // 5 minutes
      maxSize: 100,
      persistToStorage: true,
      ...config,
    }

    // Load from localStorage on initialization
    if (typeof window !== "undefined" && this.config.persistToStorage) {
      this.loadFromStorage()
    }
  }

  set<T>(key: string, data: T, ttl?: number): void {
    const now = Date.now()
    const expiresAt = now + (ttl || this.config.defaultTTL)

    // Remove oldest entries if cache is full
    if (this.cache.size >= this.config.maxSize) {
      const oldestKey = Array.from(this.cache.keys())[0]
      this.cache.delete(oldestKey)
    }

    this.cache.set(key, {
      data,
      timestamp: now,
      expiresAt,
    })

    // Persist to localStorage
    if (typeof window !== "undefined" && this.config.persistToStorage) {
      this.saveToStorage()
    }
  }

  get<T>(key: string): T | null {
    const entry = this.cache.get(key)

    if (!entry) {
      return null
    }

    // Check if expired
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key)
      return null
    }

    return entry.data as T
  }

  has(key: string): boolean {
    const entry = this.cache.get(key)
    if (!entry) return false

    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key)
      return false
    }

    return true
  }

  delete(key: string): boolean {
    const result = this.cache.delete(key)

    if (typeof window !== "undefined" && this.config.persistToStorage) {
      this.saveToStorage()
    }

    return result
  }

  clear(): void {
    this.cache.clear()

    if (typeof window !== "undefined" && this.config.persistToStorage) {
      localStorage.removeItem("pharmalink_cache")
    }
  }

  // Get cache statistics
  getStats() {
    const now = Date.now()
    const entries = Array.from(this.cache.entries())
    const validEntries = entries.filter(([, entry]) => now <= entry.expiresAt)
    const expiredEntries = entries.filter(([, entry]) => now > entry.expiresAt)

    return {
      totalEntries: this.cache.size,
      validEntries: validEntries.length,
      expiredEntries: expiredEntries.length,
      cacheHitRate: this.getCacheHitRate(),
      memoryUsage: this.getMemoryUsage(),
    }
  }

  // Cleanup expired entries
  cleanup(): void {
    const now = Date.now()
    const expiredKeys: string[] = []

    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        expiredKeys.push(key)
      }
    }

    expiredKeys.forEach((key) => this.cache.delete(key))

    if (expiredKeys.length > 0 && typeof window !== "undefined" && this.config.persistToStorage) {
      this.saveToStorage()
    }
  }

  // Preload data for common IDs
  async preload(ids: string[], dataFetcher: (id: string) => Promise<any>): Promise<void> {
    const promises = ids
      .filter((id) => !this.has(`data_${id}`))
      .map(async (id) => {
        try {
          const data = await dataFetcher(id)
          this.set(`data_${id}`, data, 10 * 60 * 1000) // 10 minutes for preloaded data
        } catch (error) {
          console.warn(`Failed to preload data for ${id}:`, error)
        }
      })

    await Promise.all(promises)
  }

  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem("pharmalink_cache")
      if (stored) {
        const parsed = JSON.parse(stored)
        const now = Date.now()

        // Only load non-expired entries
        for (const [key, entry] of Object.entries(parsed)) {
          const cacheEntry = entry as CacheEntry<any>
          if (now <= cacheEntry.expiresAt) {
            this.cache.set(key, cacheEntry)
          }
        }
      }
    } catch (error) {
      console.warn("Failed to load cache from storage:", error)
    }
  }

  private saveToStorage(): void {
    try {
      const cacheObject = Object.fromEntries(this.cache.entries())
      localStorage.setItem("pharmalink_cache", JSON.stringify(cacheObject))
    } catch (error) {
      console.warn("Failed to save cache to storage:", error)
      // If storage is full, clear old entries and try again
      this.cleanup()
      try {
        const cacheObject = Object.fromEntries(this.cache.entries())
        localStorage.setItem("pharmalink_cache", JSON.stringify(cacheObject))
      } catch (retryError) {
        console.error("Failed to save cache after cleanup:", retryError)
      }
    }
  }

  private getCacheHitRate(): number {
    // This would need to be tracked separately in a real implementation
    return 0
  }

  private getMemoryUsage(): number {
    // Rough estimate of memory usage
    return JSON.stringify(Object.fromEntries(this.cache.entries())).length
  }
}

// Global cache instance
export const globalCache = new CacheService({
  defaultTTL: 5 * 60 * 1000, // 5 minutes
  maxSize: 50,
  persistToStorage: true,
})

// Specialized caches for different data types
export const configCache = new CacheService({
  defaultTTL: 30 * 60 * 1000, // 30 minutes for config
  maxSize: 10,
  persistToStorage: true,
})

export const dataCache = new CacheService({
  defaultTTL: 10 * 60 * 1000, // 10 minutes for product data
  maxSize: 30,
  persistToStorage: true,
})

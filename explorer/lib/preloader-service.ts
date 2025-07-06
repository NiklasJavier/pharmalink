// Service für das Vorladen häufig verwendeter Daten

import { getDataById } from "./data-service"
import { dataCache } from "./cache-service"

interface PreloadConfig {
  commonIds: string[]
  linkedDataDepth: number
  preloadOnIdle: boolean
}

class PreloaderService {
  private config: PreloadConfig
  private preloadQueue: Set<string> = new Set()
  private isPreloading = false
  private preloadedIds = new Set<string>()

  constructor(config: Partial<PreloadConfig> = {}) {
    this.config = {
      commonIds: ["MED-1", "HERSTELLER-1", "UNIT-1"],
      linkedDataDepth: 1,
      preloadOnIdle: true,
      ...config,
    }

    if (typeof window !== "undefined") {
      this.initializePreloading()
    }
  }

  private initializePreloading(): void {
    // Start preloading when the page is idle
    if (this.config.preloadOnIdle) {
      if ("requestIdleCallback" in window) {
        requestIdleCallback(() => this.startPreloading(), { timeout: 5000 })
      } else {
        // Fallback for browsers without requestIdleCallback
        setTimeout(() => this.startPreloading(), 2000)
      }
    }

    // Preload on user interactions that suggest navigation
    this.setupInteractionPreloading()
  }

  private setupInteractionPreloading(): void {
    // Preload when user hovers over links
    document.addEventListener("mouseover", (event) => {
      const target = event.target as HTMLElement
      const link = target.closest("a[href], button[data-preload-id]")

      if (link) {
        const href = link.getAttribute("href")
        const preloadId = link.getAttribute("data-preload-id")

        if (href && href.startsWith("/") && href.length > 1) {
          const id = href.substring(1)
          this.queuePreload(id)
        } else if (preloadId) {
          this.queuePreload(preloadId)
        }
      }
    })

    // Preload on focus (keyboard navigation)
    document.addEventListener("focusin", (event) => {
      const target = event.target as HTMLElement
      const link = target.closest("a[href], button[data-preload-id]")

      if (link) {
        const href = link.getAttribute("href")
        const preloadId = link.getAttribute("data-preload-id")

        if (href && href.startsWith("/") && href.length > 1) {
          const id = href.substring(1)
          this.queuePreload(id)
        } else if (preloadId) {
          this.queuePreload(preloadId)
        }
      }
    })
  }

  async startPreloading(): Promise<void> {
    if (this.isPreloading) return

    this.isPreloading = true

    try {
      // Preload common IDs first
      await this.preloadCommonIds()

      // Process preload queue
      await this.processPreloadQueue()
    } catch (error) {
      console.warn("Preloading failed:", error)
    } finally {
      this.isPreloading = false
    }
  }

  private async preloadCommonIds(): Promise<void> {
    const promises = this.config.commonIds
      .filter((id) => !dataCache.has(`data_${id}`) && !this.preloadedIds.has(id))
      .map((id) => this.preloadSingleId(id))

    await Promise.all(promises)
  }

  private async processPreloadQueue(): Promise<void> {
    const queueArray = Array.from(this.preloadQueue)
    this.preloadQueue.clear()

    const promises = queueArray
      .filter((id) => !dataCache.has(`data_${id}`) && !this.preloadedIds.has(id))
      .slice(0, 5) // Limit concurrent preloads
      .map((id) => this.preloadSingleId(id))

    await Promise.all(promises)
  }

  private async preloadSingleId(id: string): Promise<void> {
    try {
      console.log(`Preloading data for ${id}`)

      const response = await getDataById(id)

      if (response.data) {
        // Cache the main data
        dataCache.set(`data_${id}`, response, 10 * 60 * 1000) // 10 minutes
        this.preloadedIds.add(id)

        // Preload linked data if configured
        if (this.config.linkedDataDepth > 0 && response.linkedIds) {
          await this.preloadLinkedData(response.linkedIds)
        }
      }
    } catch (error) {
      console.warn(`Failed to preload ${id}:`, error)
    }
  }

  private async preloadLinkedData(linkedIds: any): Promise<void> {
    const idsToPreload: string[] = []

    if (linkedIds.medikament) idsToPreload.push(linkedIds.medikament)
    if (linkedIds.hersteller) idsToPreload.push(linkedIds.hersteller)
    if (linkedIds.unit) idsToPreload.push(...linkedIds.unit.slice(0, 2)) // Limit to first 2 units

    const promises = idsToPreload
      .filter((id) => !dataCache.has(`data_${id}`) && !this.preloadedIds.has(id))
      .slice(0, 3) // Limit linked preloads
      .map((id) => this.preloadSingleId(id))

    await Promise.all(promises)
  }

  queuePreload(id: string): void {
    if (!this.preloadedIds.has(id) && !dataCache.has(`data_${id}`)) {
      this.preloadQueue.add(id)

      // Process queue with debouncing
      if (!this.isPreloading) {
        setTimeout(() => {
          if (!this.isPreloading) {
            this.processPreloadQueue()
          }
        }, 500)
      }
    }
  }

  // Get preloading statistics
  getStats() {
    return {
      preloadedCount: this.preloadedIds.size,
      queueSize: this.preloadQueue.size,
      isPreloading: this.isPreloading,
      cacheStats: dataCache.getStats(),
    }
  }

  // Force preload specific IDs
  async forcePreload(ids: string[]): Promise<void> {
    const promises = ids.map((id) => this.preloadSingleId(id))
    await Promise.all(promises)
  }
}

// Global preloader instance
export const preloader = new PreloaderService()

// Hook for React components
export function usePreloader() {
  return {
    queuePreload: (id: string) => preloader.queuePreload(id),
    forcePreload: (ids: string[]) => preloader.forcePreload(ids),
    getStats: () => preloader.getStats(),
  }
}

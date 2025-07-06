// Service für das Caching von React-Komponenten und deren Render-Ergebnisse

import type React from "react"

interface ComponentCacheEntry {
  component: React.ReactElement
  props: any
  timestamp: number
  expiresAt: number
  renderCount: number
}

interface ComponentCacheConfig {
  defaultTTL: number
  maxSize: number
  enablePropsDiff: boolean
}

class ComponentCacheService {
  private cache = new Map<string, ComponentCacheEntry>()
  private config: ComponentCacheConfig

  constructor(config: Partial<ComponentCacheConfig> = {}) {
    this.config = {
      defaultTTL: 5 * 60 * 1000, // 5 Minuten
      maxSize: 100,
      enablePropsDiff: true,
      ...config,
    }
  }

  // Generiert Cache-Key basierend auf Komponenten-Name und Props
  private generateCacheKey(componentName: string, props: any): string {
    const propsHash = this.hashProps(props)
    return `${componentName}_${propsHash}`
  }

  // Einfacher Props-Hash für Cache-Key
  private hashProps(props: any): string {
    try {
      const sortedProps = this.sortObjectKeys(props)
      return btoa(JSON.stringify(sortedProps)).slice(0, 16)
    } catch {
      return Math.random().toString(36).slice(2, 18)
    }
  }

  private sortObjectKeys(obj: any): any {
    if (obj === null || typeof obj !== "object") return obj
    if (Array.isArray(obj)) return obj.map(this.sortObjectKeys.bind(this))

    const sorted: any = {}
    Object.keys(obj)
      .sort()
      .forEach((key) => {
        sorted[key] = this.sortObjectKeys(obj[key])
      })
    return sorted
  }

  // Prüft ob Props sich geändert haben (shallow comparison)
  private propsChanged(oldProps: any, newProps: any): boolean {
    if (!this.config.enablePropsDiff) return false

    const oldKeys = Object.keys(oldProps || {})
    const newKeys = Object.keys(newProps || {})

    if (oldKeys.length !== newKeys.length) return true

    return oldKeys.some((key) => oldProps[key] !== newProps[key])
  }

  // Komponente aus Cache abrufen
  get(componentName: string, props: any): React.ReactElement | null {
    const cacheKey = this.generateCacheKey(componentName, props)
    const entry = this.cache.get(cacheKey)

    if (!entry) return null

    // TTL-Prüfung
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(cacheKey)
      return null
    }

    // Props-Änderung prüfen
    if (this.propsChanged(entry.props, props)) {
      this.cache.delete(cacheKey)
      return null
    }

    entry.renderCount++
    return entry.component
  }

  // Komponente in Cache speichern
  set(componentName: string, props: any, component: React.ReactElement, ttl?: number): void {
    const cacheKey = this.generateCacheKey(componentName, props)
    const now = Date.now()
    const expiresAt = now + (ttl || this.config.defaultTTL)

    // Cache-Größe begrenzen
    if (this.cache.size >= this.config.maxSize) {
      const oldestKey = Array.from(this.cache.keys())[0]
      this.cache.delete(oldestKey)
    }

    this.cache.set(cacheKey, {
      component,
      props: { ...props }, // Shallow copy
      timestamp: now,
      expiresAt,
      renderCount: 1,
    })
  }

  // Cache-Statistiken
  getStats() {
    const entries = Array.from(this.cache.values())
    const now = Date.now()
    const validEntries = entries.filter((entry) => now <= entry.expiresAt)
    const totalRenderCount = entries.reduce((sum, entry) => sum + entry.renderCount, 0)

    return {
      totalEntries: this.cache.size,
      validEntries: validEntries.length,
      expiredEntries: entries.length - validEntries.length,
      totalRenderCount,
      averageRenderCount: totalRenderCount / entries.length || 0,
      cacheHitRate: this.calculateHitRate(),
      memoryUsage: this.estimateMemoryUsage(),
    }
  }

  private calculateHitRate(): number {
    // Vereinfachte Hit-Rate Berechnung
    const entries = Array.from(this.cache.values())
    const totalRenders = entries.reduce((sum, entry) => sum + entry.renderCount, 0)
    const uniqueComponents = entries.length
    return uniqueComponents > 0 ? ((totalRenders - uniqueComponents) / totalRenders) * 100 : 0
  }

  private estimateMemoryUsage(): number {
    return JSON.stringify(Array.from(this.cache.entries())).length
  }

  // Cache bereinigen
  cleanup(): void {
    const now = Date.now()
    const expiredKeys: string[] = []

    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        expiredKeys.push(key)
      }
    }

    expiredKeys.forEach((key) => this.cache.delete(key))
  }

  clear(): void {
    this.cache.clear()
  }
}

// Globale Instanz
export const componentCache = new ComponentCacheService({
  defaultTTL: 10 * 60 * 1000, // 10 Minuten für Komponenten
  maxSize: 50,
  enablePropsDiff: true,
})

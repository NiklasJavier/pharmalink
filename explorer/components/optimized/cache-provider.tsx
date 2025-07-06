"use client"

import { createContext, useContext, useEffect, useState, type ReactNode } from "react"
import { componentCache } from "@/lib/component-cache-service"
import { componentPreloader } from "@/lib/component-preloader"

interface CacheContextType {
  cacheStats: any
  clearCache: () => void
  preloadComponents: () => void
  isPreloading: boolean
}

const CacheContext = createContext<CacheContextType | undefined>(undefined)

export function useCacheContext() {
  const context = useContext(CacheContext)
  if (!context) {
    throw new Error("useCacheContext must be used within CacheProvider")
  }
  return context
}

interface CacheProviderProps {
  children: ReactNode
}

export function CacheProvider({ children }: CacheProviderProps) {
  const [cacheStats, setCacheStats] = useState(null)
  const [isPreloading, setIsPreloading] = useState(false)

  // Cache-Statistiken aktualisieren
  useEffect(() => {
    const updateStats = () => {
      setCacheStats(componentCache.getStats())
    }

    updateStats()
    const interval = setInterval(updateStats, 5000) // Alle 5 Sekunden

    return () => clearInterval(interval)
  }, [])

  // Komponenten-Preloading beim Start
  useEffect(() => {
    setIsPreloading(true)
    componentPreloader.preloadCommonComponents()

    setTimeout(() => {
      setIsPreloading(false)
    }, 2000)
  }, [])

  const clearCache = () => {
    componentCache.clear()
    setCacheStats(componentCache.getStats())
  }

  const preloadComponents = () => {
    setIsPreloading(true)
    componentPreloader.preloadCommonComponents()

    setTimeout(() => {
      setIsPreloading(false)
    }, 1000)
  }

  return (
    <CacheContext.Provider
      value={{
        cacheStats,
        clearCache,
        preloadComponents,
        isPreloading,
      }}
    >
      {children}
    </CacheContext.Provider>
  )
}

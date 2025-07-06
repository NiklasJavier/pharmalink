"use client"

import { useMemo, useRef } from "react"
import { componentCache } from "@/lib/component-cache-service"
import type { ReactElement } from "react"

interface UseCachedComponentOptions {
  ttl?: number
  cacheKey?: string
  dependencies?: any[]
  enabled?: boolean
}

export function useCachedComponent<T extends Record<string, any>>(
  componentName: string,
  props: T,
  renderFunction: (props: T) => ReactElement,
  options: UseCachedComponentOptions = {},
): ReactElement {
  const { ttl, cacheKey, dependencies = [], enabled = true } = options

  const renderCountRef = useRef(0)
  const lastPropsRef = useRef<T>(props)

  return useMemo(() => {
    if (!enabled) {
      return renderFunction(props)
    }

    const finalCacheKey = cacheKey || componentName

    // Versuche aus Cache zu laden
    const cached = componentCache.get(finalCacheKey, props)
    if (cached) {
      console.log(`Cache hit for component: ${finalCacheKey}`)
      return cached
    }

    // Komponente rendern
    console.log(`Cache miss for component: ${finalCacheKey}, rendering...`)
    renderCountRef.current++
    const rendered = renderFunction(props)

    // In Cache speichern
    componentCache.set(finalCacheKey, props, rendered, ttl)
    lastPropsRef.current = props

    return rendered
  }, [componentName, JSON.stringify(props), ...dependencies])
}

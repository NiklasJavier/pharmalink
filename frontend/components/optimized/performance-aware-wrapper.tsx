"use client"

import { memo, useMemo } from "react"
import type { ReactNode, ComponentType } from "react"

interface PerformanceWrapperProps {
  children: ReactNode
  cacheKey?: string
  dependencies?: any[]
  skipMemo?: boolean
}

// HOC für Performance-optimierte Komponenten
export function withPerformanceOptimization<T extends Record<string, any>>(
  Component: ComponentType<T>,
  options: {
    displayName?: string
    cacheKey?: (props: T) => string
    shouldUpdate?: (prevProps: T, nextProps: T) => boolean
  } = {},
) {
  const { displayName, cacheKey, shouldUpdate } = options

  const MemoizedComponent = memo(Component, shouldUpdate)
  MemoizedComponent.displayName = displayName || `Memoized(${Component.displayName || Component.name})`

  return function PerformanceOptimizedComponent(props: T) {
    // Memoize Props für Stabilität
    const memoizedProps = useMemo(() => props, [JSON.stringify(props)])

    // Cache-Key generieren falls gewünscht
    const componentCacheKey = useMemo(() => {
      return cacheKey ? cacheKey(props) : undefined
    }, [props])

    return <MemoizedComponent {...memoizedProps} />
  }
}

// Wrapper für Performance-bewusste Komponenten
export const PerformanceAwareWrapper = memo(function PerformanceAwareWrapper({
  children,
  cacheKey,
  dependencies = [],
  skipMemo = false,
}: PerformanceWrapperProps) {
  const memoizedChildren = useMemo(() => {
    if (skipMemo) return children
    return children
  }, [children, ...dependencies])

  return <>{memoizedChildren}</>
})

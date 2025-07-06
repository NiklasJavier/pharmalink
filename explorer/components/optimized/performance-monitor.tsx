"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Activity, Database, Zap, Clock } from "lucide-react"
import { globalCache, dataCache, configCache } from "@/lib/cache-service"
import { preloader } from "@/lib/preloader-service"

export function PerformanceMonitor() {
  const [isVisible, setIsVisible] = useState(false)
  const [stats, setStats] = useState<any>(null)

  useEffect(() => {
    if (isVisible) {
      const updateStats = () => {
        setStats({
          globalCache: globalCache.getStats(),
          dataCache: dataCache.getStats(),
          configCache: configCache.getStats(),
          preloader: preloader.getStats(),
          performance: {
            memory: (performance as any).memory
              ? {
                  used: Math.round((performance as any).memory.usedJSHeapSize / 1024 / 1024),
                  total: Math.round((performance as any).memory.totalJSHeapSize / 1024 / 1024),
                  limit: Math.round((performance as any).memory.jsHeapSizeLimit / 1024 / 1024),
                }
              : null,
            navigation: performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming,
          },
        })
      }

      updateStats()
      const interval = setInterval(updateStats, 2000)
      return () => clearInterval(interval)
    }
  }, [isVisible])

  if (!isVisible) {
    return (
      <Button
        onClick={() => setIsVisible(true)}
        variant="outline"
        size="sm"
        className="fixed bottom-4 left-4 z-50 opacity-50 hover:opacity-100"
        title="Performance Monitor anzeigen"
      >
        <Activity className="h-4 w-4" />
      </Button>
    )
  }

  return (
    <Card className="fixed bottom-4 left-4 z-50 w-80 max-h-96 overflow-y-auto">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm flex items-center gap-2">
            <Activity className="h-4 w-4" />
            Performance Monitor
          </CardTitle>
          <Button onClick={() => setIsVisible(false)} variant="ghost" size="sm" className="h-6 w-6 p-0">
            ×
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 text-xs">
        {/* Cache Stats */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Database className="h-3 w-3" />
            <span className="font-medium">Cache Status</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <Badge variant="outline" className="text-xs">
                Data: {stats?.dataCache?.validEntries || 0}
              </Badge>
            </div>
            <div>
              <Badge variant="outline" className="text-xs">
                Config: {stats?.configCache?.validEntries || 0}
              </Badge>
            </div>
          </div>
        </div>

        {/* Preloader Stats */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Zap className="h-3 w-3" />
            <span className="font-medium">Preloader</span>
          </div>
          <div className="space-y-1">
            <div className="flex justify-between">
              <span>Preloaded:</span>
              <Badge variant="secondary" className="text-xs">
                {stats?.preloader?.preloadedCount || 0}
              </Badge>
            </div>
            <div className="flex justify-between">
              <span>Queue:</span>
              <Badge variant="secondary" className="text-xs">
                {stats?.preloader?.queueSize || 0}
              </Badge>
            </div>
            {stats?.preloader?.isPreloading && (
              <Badge variant="outline" className="text-xs">
                Loading...
              </Badge>
            )}
          </div>
        </div>

        {/* Memory Stats */}
        {stats?.performance?.memory && (
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Clock className="h-3 w-3" />
              <span className="font-medium">Memory</span>
            </div>
            <div className="space-y-1">
              <div className="flex justify-between">
                <span>Used:</span>
                <Badge variant="secondary" className="text-xs">
                  {stats.performance.memory.used}MB
                </Badge>
              </div>
              <div className="flex justify-between">
                <span>Total:</span>
                <Badge variant="secondary" className="text-xs">
                  {stats.performance.memory.total}MB
                </Badge>
              </div>
            </div>
          </div>
        )}

        {/* Page Load Stats */}
        {stats?.performance?.navigation && (
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Clock className="h-3 w-3" />
              <span className="font-medium">Load Time</span>
            </div>
            <div className="space-y-1">
              <div className="flex justify-between">
                <span>DOM:</span>
                <Badge variant="secondary" className="text-xs">
                  {Math.round(
                    stats.performance.navigation.domContentLoadedEventEnd -
                      stats.performance.navigation.navigationStart,
                  )}
                  ms
                </Badge>
              </div>
              <div className="flex justify-between">
                <span>Load:</span>
                <Badge variant="secondary" className="text-xs">
                  {Math.round(stats.performance.navigation.loadEventEnd - stats.performance.navigation.navigationStart)}
                  ms
                </Badge>
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-2 pt-2 border-t">
          <Button
            onClick={() => {
              globalCache.clear()
              dataCache.clear()
              configCache.clear()
            }}
            variant="outline"
            size="sm"
            className="text-xs h-6"
          >
            Clear Cache
          </Button>
          <Button
            onClick={() => {
              globalCache.cleanup()
              dataCache.cleanup()
              configCache.cleanup()
            }}
            variant="outline"
            size="sm"
            className="text-xs h-6"
          >
            Cleanup
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

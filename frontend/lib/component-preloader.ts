interface ComponentPreloadConfig {
  componentName: string
  propsVariations: any[]
  priority: number
}

class ComponentPreloader {
  private preloadQueue: ComponentPreloadConfig[] = []
  private isPreloading = false

  // Komponente für Preloading registrieren
  registerForPreload(config: ComponentPreloadConfig): void {
    this.preloadQueue.push(config)
    this.preloadQueue.sort((a, b) => b.priority - a.priority) // Höchste Priorität zuerst
  }

  // Preloading starten
  async startPreloading(): Promise<void> {
    if (this.isPreloading || this.preloadQueue.length === 0) return

    this.isPreloading = true

    try {
      // Verwende requestIdleCallback für Performance
      if ("requestIdleCallback" in window) {
        requestIdleCallback(() => this.processPreloadQueue(), { timeout: 5000 })
      } else {
        setTimeout(() => this.processPreloadQueue(), 1000)
      }
    } finally {
      this.isPreloading = false
    }
  }

  private async processPreloadQueue(): Promise<void> {
    const config = this.preloadQueue.shift()
    if (!config) return

    const { componentName, propsVariations } = config

    // Preload verschiedene Props-Variationen
    for (const props of propsVariations) {
      // Simuliere Komponenten-Rendering für Cache
      const cacheKey = `${componentName}_${JSON.stringify(props).slice(0, 16)}`

      // Hier würde normalerweise die Komponente gerendert und gecacht
      console.log(`Preloading component: ${componentName} with props:`, props)
    }

    // Nächste Komponente verarbeiten
    if (this.preloadQueue.length > 0) {
      setTimeout(() => this.processPreloadQueue(), 100)
    }
  }

  // Häufig verwendete Komponenten preloaden
  preloadCommonComponents(): void {
    const commonComponents: ComponentPreloadConfig[] = [
      {
        componentName: "InfoCard",
        propsVariations: [
          { title: "Identifikation", data: [] },
          { title: "Wirkstoff", data: [] },
          { title: "Haltbarkeit", data: [] },
        ],
        priority: 10,
      },
      {
        componentName: "Timeline",
        propsVariations: [{ title: "Lieferkette", events: [] }],
        priority: 8,
      },
      {
        componentName: "DocumentCard",
        propsVariations: [{ title: "Dokumente", documents: [] }],
        priority: 6,
      },
    ]

    commonComponents.forEach((config) => this.registerForPreload(config))
    this.startPreloading()
  }
}

export const componentPreloader = new ComponentPreloader()

export interface AppConfig {
  app: {
    name: string
    version: string
    description: string
    environment: "development" | "production" | "staging"
  }
  api: {
    documentation_url: string
    backend_version_url: string
    base_url: string
    search_url: string
    history_url: string
  }
  data: {
    source: "auto" | "local" | "api"
    timeout: number
    retry_attempts: number
    cache_duration: number
  }
  ui: {
    theme: {
      primary_color: string
      header_height: string
      mobile_header_height: string
    }
  }
  features: {
    qr_scanner: boolean
    search_history: boolean
    offline_mode: boolean
  }
  links: {
    support_url: string
    privacy_url: string
    terms_url: string
  }
}

let config: AppConfig | null = null

export function getConfig(): AppConfig {
  if (config) return config

  // SERVER: read YAML from disk
  if (typeof window === "undefined") {
    try {
      const { readFileSync } = require("fs")
      const { join } = require("path")
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const yaml = require("js-yaml")
      const configPath = join(process.cwd(), "config", "app.yaml")
      const fileContents = readFileSync(configPath, "utf8")
      config = yaml.load(fileContents) as AppConfig
      return config
    } catch (err) {
      console.error("Failed to load YAML config – falling back to defaults", err)
    }
  }

  // BROWSER  **or** server failure → fallback
  config = {
    app: {
      name: "PharmaLink Explorer",
      version: "v1.2.3",
      description: "Digitale Informationen für die pharmazeutische Lieferkette",
      environment: "development",
    },
    api: {
      documentation_url: "/api-docs",
      backend_version_url: "/api/version",
      base_url: "/api/v1",
      search_url: "/api/v1/search",
      history_url: "/api/v1/history",
    },
    data: {
      source: "auto",
      timeout: 10000,
      retry_attempts: 3,
      cache_duration: 300000,
    },
    ui: {
      theme: {
        primary_color: "emerald",
        header_height: "20",
        mobile_header_height: "16",
      },
    },
    features: {
      qr_scanner: false,
      search_history: false,
      offline_mode: false,
    },
    links: {
      support_url: "https://vercel.com/help",
      privacy_url: "#",
      terms_url: "#",
    },
  }
  return config
}

// Client-side config (safe for browser)
export interface ClientConfig {
  app: AppConfig["app"]
  api: {
    documentation_url: string
    base_url: string
    search_url: string
    history_url: string
  }
  data: AppConfig["data"]
  ui: AppConfig["ui"]
  features: AppConfig["features"]
  links: AppConfig["links"]
}

export function getClientConfig(): ClientConfig {
  const full = getConfig()
  return {
    app: full.app,
    api: {
      documentation_url: full.api.documentation_url,
      base_url: full.api.base_url,
      search_url: full.api.search_url,
      history_url: full.api.history_url,
    },
    data: full.data,
    ui: full.ui,
    features: full.features,
    links: full.links,
  }
}

// Helper function to determine data source
export function getDataSource(): "local" | "api" {
  const cfg = getConfig()
  if (cfg.data.source === "local") return "local"
  if (cfg.data.source === "api") return "api"
  return cfg.app.environment === "development" ? "local" : "api"
}

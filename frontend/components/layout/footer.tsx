"use client"

import Link from "next/link"
import { Book, Loader2 } from "lucide-react"
import { useConfig } from "@/hooks/use-config"
import { useBackendVersion } from "@/lib/version-service"

export function Footer() {
  const { config } = useConfig()
  const { version: backendVersion, loading: versionLoading } = useBackendVersion()

  const apiDocsUrl = config?.api.documentation_url || "/api-docs"
  const supportUrl = config?.links.support_url || "https://vercel.com/help"

  return (
    <footer className="border-t border-gray-200 bg-white py-6 mt-auto">
      <div className="w-full max-w-7xl mx-auto px-4 md:px-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-sm text-gray-600">
          <div className="flex items-center gap-6">
            <Link href={apiDocsUrl} className="flex items-center gap-2 hover:text-emerald-600 transition-colors">
              <Book className="h-4 w-4" />
              API Dokumentation
            </Link>
            <Link href={supportUrl} className="hover:text-emerald-600 transition-colors">
              Support
            </Link>
          </div>
          <div className="flex items-center gap-2">
            <span>Backend:</span>
            {versionLoading ? (
              <div className="flex items-center gap-1">
                <Loader2 className="h-3 w-3 animate-spin" />
                <span className="font-mono text-gray-400">loading...</span>
              </div>
            ) : (
              <span
                className={`font-mono ${backendVersion?.status === "error" ? "text-red-500" : "text-gray-500"}`}
                title={
                  backendVersion?.status === "error"
                    ? "Backend version could not be fetched"
                    : `Last updated: ${backendVersion?.timestamp}`
                }
              >
                {backendVersion?.version || "unknown"}
              </span>
            )}
          </div>
        </div>
      </div>
    </footer>
  )
}

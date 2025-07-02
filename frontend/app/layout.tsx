import type React from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "./globals.css"
import { MobileLayoutProvider } from "@/components/layout/mobile-layout-provider"
import { PerformanceMonitor } from "@/components/optimized/performance-monitor"

const inter = Inter({
  subsets: ["latin"],
  display: "swap", // Optimiert Font-Loading
  preload: true,
})

export const metadata: Metadata = {
  title: "PharmaLink Explorer",
  description: "Digitale Informationen für die pharmazeutische Lieferkette",
  viewport: {
    width: "device-width",
    initialScale: 1,
    maximumScale: 1,
    userScalable: false,
    viewportFit: "cover",
  },
  themeColor: "#10b981",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "PharmaLink",
  },
    generator: 'v0.dev'
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="de" className="high-dpi-optimized">
      <head>
        <meta name="format-detection" content="telephone=no" />
        <meta name="mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="default" />
        <meta name="apple-touch-fullscreen" content="yes" />

        {/* Preload critical resources */}
        <link rel="preload" href="/api/config" as="fetch" crossOrigin="anonymous" />

        {/* DNS prefetch for external resources */}
        <link rel="dns-prefetch" href="//vercel.com" />
        <link rel="dns-prefetch" href="//api.ihredomäne.com" />
      </head>
      <body className={`${inter.className} custom-scrollbar`}>
        <MobileLayoutProvider>
          {children}
          {process.env.NODE_ENV === "development" && <PerformanceMonitor />}
        </MobileLayoutProvider>
      </body>
    </html>
  )
}

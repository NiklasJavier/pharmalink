"use client"

import Link from "next/link"
import { usePreloader } from "@/lib/preloader-service"
import type { ReactNode } from "react"

interface PreloadLinkProps {
  href: string
  children: ReactNode
  className?: string
  preloadOnHover?: boolean
  preloadOnFocus?: boolean
}

export function PreloadLink({
  href,
  children,
  className,
  preloadOnHover = true,
  preloadOnFocus = true,
}: PreloadLinkProps) {
  const { queuePreload } = usePreloader()

  const handleMouseEnter = () => {
    if (preloadOnHover && href.startsWith("/") && href.length > 1) {
      const id = href.substring(1)
      queuePreload(id)
    }
  }

  const handleFocus = () => {
    if (preloadOnFocus && href.startsWith("/") && href.length > 1) {
      const id = href.substring(1)
      queuePreload(id)
    }
  }

  return (
    <Link href={href} className={className} onMouseEnter={handleMouseEnter} onFocus={handleFocus}>
      {children}
    </Link>
  )
}

"use client"

import { cn } from "@/lib/utils"
import { useMobileLayout } from "./mobile-layout-provider"
import type { ReactNode } from "react"

interface ResponsiveContainerProps {
  children: ReactNode
  className?: string
  maxWidth?: "sm" | "md" | "lg" | "xl" | "2xl" | "full"
  padding?: "none" | "sm" | "md" | "lg"
  centerContent?: boolean
  adaptToKeyboard?: boolean
}

export function ResponsiveContainer({
  children,
  className,
  maxWidth = "full",
  padding = "md",
  centerContent = false,
  adaptToKeyboard = false,
}: ResponsiveContainerProps) {
  const { isMobile, isTablet, isKeyboardVisible, safeAreaInsets } = useMobileLayout()

  const maxWidthClasses = {
    sm: "max-w-sm",
    md: "max-w-md",
    lg: "max-w-lg",
    xl: "max-w-xl",
    "2xl": "max-w-2xl",
    full: "max-w-full",
  }

  const paddingClasses = {
    none: "",
    sm: isMobile ? "px-3 py-2" : "px-4 py-3",
    md: isMobile ? "px-4 py-3" : "px-6 py-4",
    lg: isMobile ? "px-6 py-4" : "px-8 py-6",
  }

  const dynamicStyles = {
    paddingLeft: `max(${paddingClasses[padding].includes("px-") ? "1rem" : "0"}, ${safeAreaInsets.left}px)`,
    paddingRight: `max(${paddingClasses[padding].includes("px-") ? "1rem" : "0"}, ${safeAreaInsets.right}px)`,
    ...(adaptToKeyboard &&
      isKeyboardVisible && {
        paddingBottom: `${safeAreaInsets.bottom}px`,
      }),
  }

  return (
    <div
      className={cn(
        "w-full mx-auto transition-all duration-300 ease-in-out",
        maxWidthClasses[maxWidth],
        paddingClasses[padding],
        centerContent && "flex items-center justify-center",
        isKeyboardVisible && adaptToKeyboard && "keyboard-aware",
        className,
      )}
      style={dynamicStyles}
    >
      {children}
    </div>
  )
}

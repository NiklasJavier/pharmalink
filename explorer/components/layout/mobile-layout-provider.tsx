"use client"

import { createContext, useContext, useEffect, useState, type ReactNode } from "react"

interface MobileLayoutContextType {
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  orientation: "portrait" | "landscape"
  viewportHeight: number
  viewportWidth: number
  safeAreaInsets: {
    top: number
    bottom: number
    left: number
    right: number
  }
  isKeyboardVisible: boolean
  devicePixelRatio: number
}

/* 1️⃣ Fallback values so the hook is safe without a provider */
const defaultLayoutState: MobileLayoutContextType = {
  isMobile: false,
  isTablet: false,
  isDesktop: true,
  orientation: "portrait",
  viewportHeight: 0,
  viewportWidth: 0,
  safeAreaInsets: { top: 0, bottom: 0, left: 0, right: 0 },
  isKeyboardVisible: false,
  devicePixelRatio: 1,
}

/* 2️⃣ Initialise with the fallback so it’s never undefined */
const MobileLayoutContext = createContext<MobileLayoutContextType>(defaultLayoutState)

export function useMobileLayout() {
  /* 3️⃣ No runtime error – always returns a value */
  return useContext(MobileLayoutContext)
}

interface MobileLayoutProviderProps {
  children: ReactNode
}

export function MobileLayoutProvider({ children }: MobileLayoutProviderProps) {
  const [layoutState, setLayoutState] = useState<MobileLayoutContextType>(defaultLayoutState)

  useEffect(() => {
    // Funktion zum Aktualisieren der CSS Custom Properties für Viewport
    const updateViewportHeight = () => {
      const vh = window.innerHeight * 0.01
      document.documentElement.style.setProperty("--vh", `${vh}px`)

      const vw = window.innerWidth * 0.01
      document.documentElement.style.setProperty("--vw", `${vw}px`)
    }

    // Funktion zum Ermitteln der Safe Area Insets
    const getSafeAreaInsets = () => {
      const computedStyle = getComputedStyle(document.documentElement)
      return {
        top: Number.parseInt(computedStyle.getPropertyValue("--safe-area-inset-top") || "0"),
        bottom: Number.parseInt(computedStyle.getPropertyValue("--safe-area-inset-bottom") || "0"),
        left: Number.parseInt(computedStyle.getPropertyValue("--safe-area-inset-left") || "0"),
        right: Number.parseInt(computedStyle.getPropertyValue("--safe-area-inset-right") || "0"),
      }
    }

    // Funktion zum Erkennen der Tastatur (mobile)
    const detectKeyboard = () => {
      const initialHeight = window.innerHeight
      return window.innerHeight < initialHeight * 0.75
    }

    // Hauptfunktion zum Aktualisieren des Layout-Status
    const updateLayoutState = () => {
      const width = window.innerWidth
      const height = window.innerHeight
      const orientation = width > height ? "landscape" : "portrait"

      // Responsive Breakpoints
      const isMobile = width < 768
      const isTablet = width >= 768 && width < 1024
      const isDesktop = width >= 1024

      updateViewportHeight()

      setLayoutState({
        isMobile,
        isTablet,
        isDesktop,
        orientation,
        viewportHeight: height,
        viewportWidth: width,
        safeAreaInsets: getSafeAreaInsets(),
        isKeyboardVisible: detectKeyboard(),
        devicePixelRatio: window.devicePixelRatio || 1,
      })
    }

    // Initial setup
    updateLayoutState()

    // Event Listeners
    const handleResize = () => {
      updateLayoutState()
    }

    const handleOrientationChange = () => {
      // Delay für iOS Orientation Change
      setTimeout(updateLayoutState, 100)
    }

    // Visual Viewport API für bessere Keyboard-Erkennung (falls verfügbar)
    const handleVisualViewportChange = () => {
      if (window.visualViewport) {
        const keyboardVisible = window.visualViewport.height < window.innerHeight * 0.75
        setLayoutState((prev) => ({
          ...prev,
          isKeyboardVisible: keyboardVisible,
          viewportHeight: window.visualViewport.height,
        }))
      }
    }

    window.addEventListener("resize", handleResize)
    window.addEventListener("orientationchange", handleOrientationChange)

    if (window.visualViewport) {
      window.visualViewport.addEventListener("resize", handleVisualViewportChange)
    }

    // Cleanup
    return () => {
      window.removeEventListener("resize", handleResize)
      window.removeEventListener("orientationchange", handleOrientationChange)

      if (window.visualViewport) {
        window.visualViewport.removeEventListener("resize", handleVisualViewportChange)
      }
    }
  }, [])

  // CSS-Klassen basierend auf dem Layout-Status setzen
  useEffect(() => {
    const { isMobile, isTablet, orientation, isKeyboardVisible } = layoutState

    const classes = []
    if (isMobile) classes.push("is-mobile")
    if (isTablet) classes.push("is-tablet")
    if (orientation === "landscape") classes.push("is-landscape")
    if (isKeyboardVisible) classes.push("keyboard-visible")

    // Entferne alte Klassen
    document.body.classList.remove("is-mobile", "is-tablet", "is-landscape", "keyboard-visible")

    // Füge neue Klassen hinzu
    classes.forEach((className) => {
      document.body.classList.add(className)
    })
  }, [layoutState])

  return <MobileLayoutContext.Provider value={layoutState}>{children}</MobileLayoutContext.Provider>
}

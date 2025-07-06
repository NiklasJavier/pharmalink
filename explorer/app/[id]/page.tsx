"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { useParams, useSearchParams } from "next/navigation"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { NotFoundMessage } from "@/components/passport/not-found-message"
import { EnhancedJsonDisplay } from "@/components/passport/enhanced-json-display"
import { FloatingOperations } from "@/components/passport/floating-operations"
import { useMobileLayout } from "@/components/layout/mobile-layout-provider"
import { getDataById } from "@/lib/optimized-data-service" // Verwende optimierte Version
import { UrlParamsDisplay } from "@/components/passport/url-params-display"
import { AlertCircle, Loader2, Search } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { HistoryModal } from "@/components/passport/history-modal"
import { extractMetaPopups } from "@/lib/meta-popup-service"
import { extractDeliveryChains } from "@/lib/delivery-service"
import { preloader } from "@/lib/preloader-service"
import { cn } from "@/lib/utils"

export default function ProductPassportPage() {
  const params = useParams()
  const searchParams = useSearchParams()
  const { isMobile, isTablet, isKeyboardVisible } = useMobileLayout()
  const [activeTab, setActiveTab] = useState("hersteller")
  const productId = params.id as string

  const [dataResponse, setDataResponse] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [globalExpanded, setGlobalExpanded] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [searchResults, setSearchResults] = useState<any[]>([])
  const [currentResultIndex, setCurrentResultIndex] = useState(0)
  const [historyModal, setHistoryModal] = useState<{
    isOpen: boolean
    keyPath: string
    keyName: string
    currentValue: string
    triggerElement?: HTMLElement
  } | null>(null)

  const [jsonOperationsPanelMinimized, setJsonOperationsPanelMinimized] = useState(true)
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [metaPopups, setMetaPopups] = useState<any[]>([])
  const [deliveryChains, setDeliveryChains] = useState<any[]>([])
  const [meldungenModalExpanded, setMeldungenModalExpanded] = useState(false)
  const [lieferungenModalExpanded, setLieferungenModalExpanded] = useState(false)

  const lastOpenedHistoryRef = useRef<string | null>(null)
  const isHistorySwitchingRef = useRef(false)
  const isClosingModalRef = useRef(false)

  // URL PARAM HANDLING
  useEffect(() => {
    const urlSearchTerm = searchParams.get("search") || ""
    const urlExpanded = searchParams.get("expanded") !== "false"
    const urlResultIndex = Number.parseInt(searchParams.get("resultIndex") || "0")

    setSearchTerm(urlSearchTerm)
    setGlobalExpanded(urlExpanded)
    setCurrentResultIndex(urlResultIndex)
  }, [searchParams])

  // OPTIMIZED DATA FETCHING mit Preloading
  useEffect(() => {
    if (!productId) return

    setLoading(true)

    // Start preloading linked data immediately
    const preloadLinkedData = async (response: any) => {
      if (response?.linkedIds) {
        const linkedIds: string[] = []
        if (response.linkedIds.medikament) linkedIds.push(response.linkedIds.medikament)
        if (response.linkedIds.hersteller) linkedIds.push(response.linkedIds.hersteller)
        if (response.linkedIds.unit) linkedIds.push(...response.linkedIds.unit.slice(0, 2))

        if (linkedIds.length > 0) {
          preloader.forcePreload(linkedIds)
        }
      }
    }

    getDataById(productId)
      .then((response) => {
        setDataResponse(response)

        if (response?.data) {
          const popups = extractMetaPopups(response.data)
          setMetaPopups(popups)

          const chains = extractDeliveryChains(response.data)
          setDeliveryChains(chains)

          // Preload linked data in background
          preloadLinkedData(response)
        }
      })
      .finally(() => setLoading(false))
  }, [productId])

  // INITIAL TAB BASED ON ID
  useEffect(() => {
    let initialTab = "hersteller"
    if (productId?.startsWith("MED-")) initialTab = "medikament"
    else if (productId?.startsWith("UNIT-")) initialTab = "einheit"
    setActiveTab(initialTab)
  }, [productId])

  // URL PARAM UPDATE HELPER
  const updateUrlParams = useCallback(
    (updates: Record<string, string | null>) => {
      const current = new URLSearchParams(Array.from(searchParams.entries()))
      Object.entries(updates).forEach(([k, v]) => {
        if (!v) current.delete(k)
        else current.set(k, v)
      })
      const q = current.toString()
      window.history.replaceState({}, "", `${window.location.pathname}${q ? `?${q}` : ""}`)
    },
    [searchParams],
  )

  // EXPAND/COLLAPSE ALL
  const handleExpandAll = () => {
    setGlobalExpanded(true)
    updateUrlParams({ expanded: "true" })
  }
  const handleCollapseAll = () => {
    setGlobalExpanded(false)
    updateUrlParams({ expanded: "false" })
  }

  // SEARCH
  const handleSearchChange = (term: string) => {
    setSearchTerm(term)
    setCurrentResultIndex(0)
    updateUrlParams({ search: term, resultIndex: term ? "0" : null })
  }

  const navigateSearchResults = (dir: "next" | "prev") => {
    if (!searchResults.length) return
    const newIdx =
      dir === "next"
        ? (currentResultIndex + 1) % searchResults.length
        : (currentResultIndex - 1 + searchResults.length) % searchResults.length
    setCurrentResultIndex(newIdx)
    updateUrlParams({ resultIndex: newIdx.toString() })
  }

  const clearSearch = () => {
    setSearchTerm("")
    setSearchResults([])
    setCurrentResultIndex(0)
    updateUrlParams({ search: null, resultIndex: null })
  }

  const handleSearchResultsChange = (results: any[]) => {
    setSearchResults(results)
    if (results.length && currentResultIndex >= results.length) {
      setCurrentResultIndex(0)
      updateUrlParams({ resultIndex: "0" })
    }
  }

  // OPTIMIZED TAB NAVIGATION mit Preloading
  const handleTabNavigation = (id: string) => {
    // Preload the target page before navigation
    preloader.queuePreload(id)

    // Small delay to allow preloading to start
    setTimeout(() => {
      window.location.href = `/${id}`
    }, 100)
  }

  const handlePopupAction = (popupId: string, action: string) => {
    console.log(`Popup action: ${popupId} -> ${action}`)

    switch (action) {
      case "reload":
        window.location.reload()
        break
      case "navigate":
        break
      case "dismiss":
        break
      default:
        console.log(`Unknown popup action: ${action}`)
    }
  }

  const handleMeldungenExpandChange = (isExpanded: boolean) => {
    setMeldungenModalExpanded(isExpanded)
  }

  const handleLieferungenExpandChange = (isExpanded: boolean) => {
    setLieferungenModalExpanded(isExpanded)
  }

  // AUTO-OPEN HISTORY MODAL FROM URL PARAMS
  useEffect(() => {
    const historyParam = searchParams.get("history") || null

    if (isClosingModalRef.current) {
      return
    }

    if (!historyParam) {
      lastOpenedHistoryRef.current = null
      if (!isHistorySwitchingRef.current) {
        setTimeout(() => {
          isHistorySwitchingRef.current = false
        }, 50)
      }
      return
    }

    if (loading || !dataResponse?.data) return
    if (lastOpenedHistoryRef.current === historyParam && historyModal?.isOpen) return

    const findKeyInData = (obj: any, targetPath: string): { key: string; value: string } | null => {
      const pathParts = targetPath.split(".")
      let current = obj
      let currentKey = ""

      for (const part of pathParts) {
        if (current && typeof current === "object" && part in current) {
          current = current[part]
          currentKey = part
        } else {
          return null
        }
      }
      return { key: currentKey, value: String(current ?? "") }
    }

    const found = findKeyInData(dataResponse.data, historyParam)
    if (found) {
      const wasHistoryOpen = historyModal?.isOpen

      if (wasHistoryOpen) {
        isHistorySwitchingRef.current = true

        setHistoryModal({
          isOpen: true,
          keyPath: historyParam,
          keyName: found.key,
          currentValue: found.value,
        })
        lastOpenedHistoryRef.current = historyParam

        setJsonOperationsPanelMinimized(true)
        setIsTransitioning(false)
      } else {
        isHistorySwitchingRef.current = false
        setIsTransitioning(true)
        setJsonOperationsPanelMinimized(true)

        setTimeout(() => {
          setHistoryModal({
            isOpen: true,
            keyPath: historyParam,
            keyName: found.key,
            currentValue: found.value,
          })
          setIsTransitioning(false)
          lastOpenedHistoryRef.current = historyParam
        }, 200)
      }
    }
  }, [searchParams.get("history"), loading, dataResponse, historyModal?.isOpen])

  const closeHistoryModal = () => {
    isClosingModalRef.current = true

    setHistoryModal(null)

    const current = new URLSearchParams(Array.from(searchParams.entries()))
    current.delete("history")
    const search = current.toString()
    const query = search ? `?${search}` : ""
    window.history.replaceState({}, "", `${window.location.pathname}${query}`)

    const willSwitchToAnother = isHistorySwitchingRef.current

    if (!willSwitchToAnother) {
      setIsTransitioning(true)

      setTimeout(() => {
        setIsTransitioning(false)
      }, 200)
    }

    setTimeout(() => {
      isHistorySwitchingRef.current = false
      isClosingModalRef.current = false
    }, 100)
  }

  const handleHistoryOpen = (keyPath: string, keyName: string, currentValue: string, iconElement: HTMLElement) => {
    if (historyModal?.isOpen && historyModal.keyPath === keyPath) return

    isClosingModalRef.current = false

    const current = new URLSearchParams(Array.from(searchParams.entries()))
    current.set("history", keyPath)
    const search = current.toString()
    const query = search ? `?${search}` : ""
    window.history.replaceState({}, "", `${window.location.pathname}${query}`)

    const wasHistoryOpen = historyModal?.isOpen

    if (wasHistoryOpen) {
      isHistorySwitchingRef.current = true
      setHistoryModal({
        isOpen: true,
        keyPath,
        keyName,
        currentValue,
        triggerElement: iconElement,
      })
    } else {
      isHistorySwitchingRef.current = false
      setIsTransitioning(true)
      setJsonOperationsPanelMinimized(true)

      setTimeout(() => {
        setHistoryModal({
          isOpen: true,
          keyPath,
          keyName,
          currentValue,
          triggerElement: iconElement,
        })
        setIsTransitioning(false)
      }, 200)
    }
  }

  // RENDER STATES
  if (loading) {
    return (
      <div className="flex flex-col min-h-screen bg-gray-50">
        <Header activeTab={activeTab as any} onTabChange={setActiveTab as any} showTabs={false} />
        <main className={cn("flex-1 flex items-center justify-center", isMobile ? "pt-24" : "pt-16")}>
          <div className="w-full max-w-7xl mx-auto px-4 md:px-6">
            <div className="flex items-center gap-3">
              <Loader2 className="h-6 w-6 animate-spin text-emerald-600" />
              <span className={cn("text-gray-600", isMobile ? "text-base" : "text-lg")}>
                {dataResponse?.source === "cache" ? "Lade aus Cache…" : "Lade Produktdaten…"}
              </span>
            </div>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  if (dataResponse?.error) {
    return (
      <div className="flex flex-col min-h-screen bg-gray-50">
        <Header activeTab={activeTab as any} onTabChange={setActiveTab as any} showTabs={false} />
        <main className={cn("flex-1", isMobile ? "pt-24" : "pt-16")}>
          <div className="w-full max-w-7xl mx-auto px-4 md:px-6">
            <Alert className="border-red-200 bg-red-50 mb-6">
              <AlertCircle className="h-4 w-4 text-red-600" />
              <AlertDescription className="text-red-800">
                <strong>Fehler beim Laden der Daten:</strong> {dataResponse.error}
              </AlertDescription>
            </Alert>
            <NotFoundMessage searchedId={productId} />
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  const productData = dataResponse?.data
  const isValid = !!productData
  const correctedId = dataResponse?.correctedId
  const linkedIds = dataResponse?.linkedIds

  return (
    <div className={cn("flex flex-col min-h-screen bg-gray-50", isKeyboardVisible && "keyboard-aware")}>
      <Header
        activeTab={activeTab as any}
        onTabChange={setActiveTab as any}
        showTabs={isValid}
        linkedIds={linkedIds}
        onSearch={handleTabNavigation}
        productId={productId}
        originalId={correctedId ? productId : undefined}
        correctedId={correctedId}
      />

      <main className={cn("flex-1", isMobile ? "pt-24" : "pt-16")}>
        <div className="w-full max-w-7xl mx-auto px-4 md:px-6">
          {isValid && productData ? (
            <>
              {correctedId && correctedId !== productId && (
                <div className={cn("mb-2 p-3 bg-blue-50 border border-blue-200 rounded-lg", isMobile && "text-sm")}>
                  <div className="flex items-center gap-2 text-blue-800">
                    <Search className="h-4 w-4 flex-shrink-0" />
                    <span className="break-words">
                      Gesuchte ID <code className="bg-blue-100 px-1 rounded break-all">{productId}</code> wurde
                      korrigiert zu{" "}
                      <code className="bg-blue-100 px-1 rounded font-semibold break-all">{correctedId}</code>
                    </span>
                  </div>
                </div>
              )}

              <UrlParamsDisplay />

              <div className={cn("transition-all duration-300", isMobile && "text-sm")}>
                <EnhancedJsonDisplay
                  data={productData}
                  maxDepth={isMobile ? 4 : 6}
                  productId={productId}
                  globalExpanded={globalExpanded}
                  searchTerm={searchTerm}
                  searchResults={searchResults}
                  currentResultIndex={currentResultIndex}
                  onSearchResultsChange={handleSearchResultsChange}
                  onSearchTermChange={handleSearchChange}
                  onHistoryOpen={handleHistoryOpen}
                />
              </div>

              <FloatingOperations
                globalExpanded={globalExpanded}
                onExpandAll={handleExpandAll}
                onCollapseAll={handleCollapseAll}
                searchTerm={searchTerm}
                onSearchChange={handleSearchChange}
                onClearSearch={clearSearch}
                searchResults={searchResults}
                currentResultIndex={currentResultIndex}
                onNavigateSearch={navigateSearchResults}
                jsonData={productData}
                dataId={correctedId || productId}
                forceMinimized={historyModal?.isOpen || isTransitioning}
                popups={metaPopups}
                deliveryChains={deliveryChains}
                onPopupExpandChange={handleMeldungenExpandChange}
                onDeliveryExpandChange={handleLieferungenExpandChange}
              />
            </>
          ) : (
            <div className="py-8">
              <NotFoundMessage searchedId={productId} />
            </div>
          )}
        </div>
      </main>

      <Footer />

      {historyModal && !isTransitioning && (
        <HistoryModal
          isOpen={historyModal.isOpen}
          onClose={closeHistoryModal}
          keyPath={historyModal.keyPath}
          keyName={historyModal.keyName}
          currentValue={historyModal.currentValue}
          triggerElement={historyModal.triggerElement}
        />
      )}
    </div>
  )
}

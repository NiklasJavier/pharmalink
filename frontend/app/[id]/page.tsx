"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { useParams, useSearchParams } from "next/navigation"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { NotFoundMessage } from "@/components/passport/not-found-message"
import { EnhancedJsonDisplay } from "@/components/passport/enhanced-json-display"
import { FloatingOperations } from "@/components/passport/floating-operations"
import { FloatingPopups } from "@/components/passport/floating-popups"
import { FloatingDeliveries } from "@/components/passport/floating-deliveries"
import { getDataById } from "@/lib/data-service"
import { UrlParamsDisplay } from "@/components/passport/url-params-display"
import { AlertCircle, Loader2, Search } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { HistoryModal } from "@/components/passport/history-modal"
import { extractMetaPopups } from "@/lib/meta-popup-service"
import { extractDeliveryChains } from "@/lib/delivery-service"

export default function ProductPassportPage() {
  const params = useParams()
  const searchParams = useSearchParams()
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

  // State für sequenzielle Animation
  const [jsonOperationsPanelMinimized, setJsonOperationsPanelMinimized] = useState(true)
  const [isTransitioning, setIsTransitioning] = useState(false)

  // State für Meta-Popups
  const [metaPopups, setMetaPopups] = useState<any[]>([])

  // State für Lieferketten
  const [deliveryChains, setDeliveryChains] = useState<any[]>([])

  // Neuer State für Meldungen-Modal
  const [meldungenModalExpanded, setMeldungenModalExpanded] = useState(false)

  // Neuer State für Lieferungen-Modal
  const [lieferungenModalExpanded, setLieferungenModalExpanded] = useState(false)

  // Merkt sich, ob wir den History-Path schon geöffnet haben
  const lastOpenedHistoryRef = useRef<string | null>(null)

  // Neuer Ref um zu tracken ob wir gerade zwischen History-Modals wechseln
  const isHistorySwitchingRef = useRef(false)

  // Neuer Ref um zu tracken ob wir gerade das Modal schließen
  const isClosingModalRef = useRef(false)

  // --- URL PARAM HANDLING ----------------------------------------------------
  useEffect(() => {
    const urlSearchTerm = searchParams.get("search") || ""
    const urlExpanded = searchParams.get("expanded") !== "false"
    const urlResultIndex = Number.parseInt(searchParams.get("resultIndex") || "0")

    setSearchTerm(urlSearchTerm)
    setGlobalExpanded(urlExpanded)
    setCurrentResultIndex(urlResultIndex)
  }, [searchParams])

  // --- DATA FETCHING ---------------------------------------------------------
  useEffect(() => {
    if (!productId) return

    setLoading(true)
    getDataById(productId)
      .then((response) => {
        setDataResponse(response)

        // Extrahiere Meta-Popups aus den Daten
        if (response?.data) {
          const popups = extractMetaPopups(response.data)
          setMetaPopups(popups)

          // Extrahiere Lieferketten aus den Daten
          const chains = extractDeliveryChains(response.data)
          setDeliveryChains(chains)
        }
      })
      .finally(() => setLoading(false))
  }, [productId])

  // --- INITIAL TAB BASED ON ID ----------------------------------------------
  useEffect(() => {
    let initialTab = "hersteller"
    if (productId?.startsWith("MED-")) initialTab = "medikament"
    else if (productId?.startsWith("UNIT-")) initialTab = "einheit"
    setActiveTab(initialTab)
  }, [productId])

  // --- URL PARAM UPDATE HELPER ----------------------------------------------
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

  // --- EXPAND/COLLAPSE ALL ---------------------------------------------------
  const handleExpandAll = () => {
    setGlobalExpanded(true)
    updateUrlParams({ expanded: "true" })
  }
  const handleCollapseAll = () => {
    setGlobalExpanded(false)
    updateUrlParams({ expanded: "false" })
  }

  // --- SEARCH ----------------------------------------------------------------
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

  // --- TAB NAVIGATION (CLEAN URL) -------------------------------------------
  const handleTabNavigation = (id: string) => {
    window.location.href = `/${id}`
  }

  // --- POPUP ACTION HANDLER --------------------------------------------------
  const handlePopupAction = (popupId: string, action: string) => {
    console.log(`Popup action: ${popupId} -> ${action}`)

    // Hier können verschiedene Aktionen implementiert werden
    switch (action) {
      case "reload":
        window.location.reload()
        break
      case "navigate":
        // Navigation zu einer anderen Seite
        break
      case "dismiss":
        // Popup wird automatisch dismissed
        break
      default:
        console.log(`Unknown popup action: ${action}`)
    }
  }

  // Handler-Funktion für Meldungen-Modal Änderungen
  const handleMeldungenExpandChange = (isExpanded: boolean) => {
    setMeldungenModalExpanded(isExpanded)
  }

  // Handler-Funktion für Lieferungen-Modal Änderungen
  const handleLieferungenExpandChange = (isExpanded: boolean) => {
    setLieferungenModalExpanded(isExpanded)
  }

  // --- AUTO-OPEN HISTORY MODAL FROM URL PARAMS -------------------------------
  useEffect(() => {
    const historyParam = searchParams.get("history") || null

    // Wenn wir gerade das Modal schließen, ignoriere URL-Änderungen
    if (isClosingModalRef.current) {
      return
    }

    if (!historyParam) {
      lastOpenedHistoryRef.current = null
      // Nur wenn wir nicht gerade zwischen History-Modals wechseln, reset switching flag
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
      // Prüfen ob bereits ein History-Modal offen ist
      const wasHistoryOpen = historyModal?.isOpen

      if (wasHistoryOpen) {
        // Wir wechseln zwischen History-Modals - JSON-Operations bleibt geschlossen
        isHistorySwitchingRef.current = true

        // Direkter Wechsel ohne Animation-Delay
        setHistoryModal({
          isOpen: true,
          keyPath: historyParam,
          keyName: found.key,
          currentValue: found.value,
        })
        lastOpenedHistoryRef.current = historyParam

        // Stelle sicher, dass JSON-Operations minimiert bleibt
        setJsonOperationsPanelMinimized(true)
        setIsTransitioning(false) // Keine Transition beim direkten Wechsel
      } else {
        // Erstes Öffnen - normale sequenzielle Animation
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
        }, 200) // 200ms Delay für sequenzielle Animation
      }
    }
  }, [searchParams.get("history"), loading, dataResponse, historyModal?.isOpen])

  const closeHistoryModal = () => {
    // Setze das Closing-Flag um weitere URL-Reaktionen zu verhindern
    isClosingModalRef.current = true

    // Modal sofort schließen
    setHistoryModal(null)

    // Remove history parameter from URL
    const current = new URLSearchParams(Array.from(searchParams.entries()))
    current.delete("history")
    const search = current.toString()
    const query = search ? `?${search}` : ""
    window.history.replaceState({}, "", `${window.location.pathname}${query}`)

    // Prüfe ob wir gerade zwischen History-Modals wechseln
    const willSwitchToAnother = isHistorySwitchingRef.current

    if (!willSwitchToAnother) {
      // Nur wenn wir nicht zu einem anderen History-Modal wechseln
      // Sequenzielle Animation: Erst History schließen, dann JSON-Operations kann erweitert werden
      setIsTransitioning(true)

      setTimeout(() => {
        setIsTransitioning(false)
        // JSON-Operations Panel kann wieder erweitert werden (bleibt aber minimiert bis Benutzer es öffnet)
      }, 200) // 200ms Delay
    }

    // Reset flags nach kurzer Zeit
    setTimeout(() => {
      isHistorySwitchingRef.current = false
      isClosingModalRef.current = false
    }, 100)
  }

  const handleHistoryOpen = (keyPath: string, keyName: string, currentValue: string, iconElement: HTMLElement) => {
    if (historyModal?.isOpen && historyModal.keyPath === keyPath) return

    // Reset closing flag wenn wir ein neues Modal öffnen
    isClosingModalRef.current = false

    // Update URL with history parameter
    const current = new URLSearchParams(Array.from(searchParams.entries()))
    current.set("history", keyPath)
    const search = current.toString()
    const query = search ? `?${search}` : ""
    window.history.replaceState({}, "", `${window.location.pathname}${query}`)

    // Prüfen ob bereits ein History-Modal offen ist
    const wasHistoryOpen = historyModal?.isOpen

    if (wasHistoryOpen) {
      // Direkter Wechsel zwischen History-Modals - kein JSON-Operations Panel Animation
      isHistorySwitchingRef.current = true
      setHistoryModal({
        isOpen: true,
        keyPath,
        keyName,
        currentValue,
        triggerElement: iconElement,
      })
    } else {
      // Erstes Öffnen - normale sequenzielle Animation
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
      }, 200) // 200ms Delay für sequenzielle Animation
    }
  }

  // --------------------------- RENDER STATES ---------------------------------
  if (loading) {
    return (
      <div className="flex flex-col min-h-screen bg-gray-50">
        <Header activeTab={activeTab as any} onTabChange={setActiveTab as any} showTabs={false} />
        <main className="flex-1 w-full max-w-7xl mx-auto p-4 md:p-8 flex items-center justify-center">
          <Loader2 className="h-6 w-6 animate-spin text-emerald-600 mr-3" />
          <span className="text-lg text-gray-600">Lade Produktdaten…</span>
        </main>
        <Footer />
      </div>
    )
  }

  if (dataResponse?.error) {
    return (
      <div className="flex flex-col min-h-screen bg-gray-50">
        <Header activeTab={activeTab as any} onTabChange={setActiveTab as any} showTabs={false} />
        <main className="flex-1 w-full max-w-7xl mx-auto p-4 md:p-8">
          <Alert className="max-w-2xl mx-auto border-red-200 bg-red-50 mb-6">
            <AlertCircle className="h-4 w-4 text-red-600" />
            <AlertDescription className="text-red-800">
              <strong>Fehler beim Laden der Daten:</strong> {dataResponse.error}
            </AlertDescription>
          </Alert>
          <NotFoundMessage searchedId={productId} />
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
    <div className="flex flex-col min-h-screen bg-gray-50">
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

      <main className="flex-1 w-full max-w-7xl mx-auto px-4 md:px-8">
        {isValid && productData ? (
          <>
            {correctedId && correctedId !== productId && (
              <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <div className="flex items-center gap-2 text-sm text-blue-800">
                  <Search className="h-4 w-4" />
                  <span>
                    Gesuchte ID <code className="bg-blue-100 px-1 rounded">{productId}</code> wurde korrigiert zu{" "}
                    <code className="bg-blue-100 px-1 rounded font-semibold">{correctedId}</code>
                  </span>
                </div>
              </div>
            )}

            <UrlParamsDisplay />
            <EnhancedJsonDisplay
              data={productData}
              maxDepth={6}
              productId={productId}
              globalExpanded={globalExpanded}
              searchTerm={searchTerm}
              searchResults={searchResults}
              currentResultIndex={currentResultIndex}
              onSearchResultsChange={handleSearchResultsChange}
              onSearchTermChange={handleSearchChange}
              onHistoryOpen={handleHistoryOpen}
            />

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
              forceMinimized={
                historyModal?.isOpen || isTransitioning || meldungenModalExpanded || lieferungenModalExpanded
              } // Erweitert um lieferungenModalExpanded
            />

            {/* Meta-Popups */}
            <FloatingPopups
              popups={metaPopups}
              jsonOperationsMinimized={jsonOperationsPanelMinimized}
              historyModalOpen={!!historyModal?.isOpen}
              onPopupAction={handlePopupAction}
              onExpandChange={handleMeldungenExpandChange}
            />

            {/* Lieferketten */}
            <FloatingDeliveries
              deliveryChains={deliveryChains}
              jsonOperationsMinimized={jsonOperationsPanelMinimized}
              historyModalOpen={!!historyModal?.isOpen}
              onExpandChange={handleLieferungenExpandChange}
              meldungenModalExpanded={meldungenModalExpanded} // Neue Prop hinzufügen
            />
          </>
        ) : (
          <div className="p-4 md:p-8">
            <NotFoundMessage searchedId={productId} />
          </div>
        )}
      </main>

      <Footer />
      {/* History Modal - nur anzeigen wenn nicht in Transition und Modal offen */}
      {historyModal && !isTransitioning && (
        <HistoryModal
          isOpen={historyModal.isOpen}
          onClose={closeHistoryModal}
          productId={correctedId || productId}
          keyPath={historyModal.keyPath}
          keyName={historyModal.keyName}
          currentValue={historyModal.currentValue}
          triggerElement={historyModal.triggerElement}
          jsonOperationsPanelMinimized={jsonOperationsPanelMinimized}
        />
      )}
    </div>
  )
}

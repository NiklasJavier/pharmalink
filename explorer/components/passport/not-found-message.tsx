import { Search, AlertCircle, ArrowLeft } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import Link from "next/link"

type NotFoundMessageProps = {
  searchedId: string
}

export function NotFoundMessage({ searchedId }: NotFoundMessageProps) {
  const availableIds = [
    { id: "MED-1", type: "Medikament", description: "Aspirin 500mg Tabletten" },
    { id: "UNIT-1", type: "Unit", description: "Blisterpackung mit Tracking-Daten" },
    { id: "HERSTELLER-1", type: "Hersteller", description: "Bayer AG Produktionsdaten" },
  ]

  return (
    <div className="max-w-2xl mx-auto">
      <Card className="border-red-200 bg-red-50">
        <CardContent className="p-8 text-center">
          <div className="flex justify-center mb-4">
            <div className="p-3 bg-red-100 rounded-full">
              <AlertCircle className="h-8 w-8 text-red-600" />
            </div>
          </div>

          <h2 className="text-2xl font-bold text-red-900 mb-2">Kein Ergebnis gefunden</h2>
          <p className="text-red-700 mb-6">
            Die gesuchte ID <span className="font-mono bg-white px-2 py-1 rounded border">{searchedId}</span> konnte
            nicht gefunden werden.
          </p>

          <div className="mb-6">
            <Link href="/">
              <Button className="bg-red-600 hover:bg-red-700 text-white">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Neue Suche starten
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-6 border-blue-200 bg-blue-50">
        <CardContent className="p-6">
          <div className="flex items-center gap-2 mb-4">
            <Search className="h-5 w-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-blue-900">Verfügbare Test-IDs</h3>
          </div>

          <div className="space-y-3">
            {availableIds.map((item) => (
              <Link key={item.id} href={`/${item.id}`}>
                <div className="p-3 bg-white rounded-lg border border-blue-200 hover:border-blue-400 hover:shadow-sm transition-all cursor-pointer">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-mono text-sm bg-blue-100 px-2 py-1 rounded text-blue-800">{item.id}</span>
                      <span className="ml-3 font-medium text-gray-900">{item.type}</span>
                    </div>
                    <span className="text-sm text-gray-600">{item.description}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          <p className="text-sm text-blue-700 mt-4">
            Klicken Sie auf eine der verfügbaren IDs, um die entsprechenden Daten anzuzeigen.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

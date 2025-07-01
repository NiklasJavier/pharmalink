import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Download } from "lucide-react"
import type { ReactNode } from "react"

type DocumentCardProps = {
  title: string
  icon: ReactNode
  documents: { name: string; size: string }[]
}

export function DocumentCard({ title, icon, documents }: DocumentCardProps) {
  return (
    <Card id="dokumente" className="bg-white border-gray-200">
      <CardHeader className="flex flex-row items-center gap-3 space-y-0">
        <div className="p-2 bg-gray-100 rounded-md">{icon}</div>
        <CardTitle className="text-lg">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2">
          {documents.map((doc) => (
            <li key={doc.name} className="flex items-center justify-between p-2 rounded-md hover:bg-gray-100">
              <div>
                <p className="font-medium">{doc.name}</p>
                <p className="text-sm text-gray-500">{doc.size}</p>
              </div>
              <Button variant="ghost" size="icon">
                <Download className="h-4 w-4" />
              </Button>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

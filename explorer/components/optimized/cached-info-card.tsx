import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useCachedComponent } from "@/hooks/use-cached-component"
import type { ReactNode } from "react"

type InfoCardProps = {
  title: string
  icon: ReactNode
  data: { label: string; value: string }[]
}

// Gecachte Version der InfoCard
export function CachedInfoCard(props: InfoCardProps) {
  return useCachedComponent(
    "InfoCard",
    props,
    ({ title, icon, data }) => (
      <Card id={title.toLowerCase().split(" ")[0]} className="bg-white border-gray-200">
        <CardHeader className="flex flex-row items-center gap-3 space-y-0">
          <div className="p-2 bg-gray-100 rounded-md">{icon}</div>
          <CardTitle className="text-lg">{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="space-y-3">
            {data.map((item) => (
              <li key={item.label} className="flex justify-between text-sm">
                <span className="text-gray-500">{item.label}</span>
                <span className="font-medium text-right">{item.value}</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    ),
    {
      ttl: 15 * 60 * 1000, // 15 Minuten - InfoCards ändern sich selten
      dependencies: [props.data.length], // Re-cache wenn Anzahl der Items ändert
    },
  )
}

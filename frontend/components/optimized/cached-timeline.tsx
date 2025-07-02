import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { CheckCircle, Circle } from "lucide-react"
import { useCachedComponent } from "@/hooks/use-cached-component"
import type { ReactNode } from "react"
import { cn } from "@/lib/utils"

type TimelineProps = {
  title: string
  icon: ReactNode
  events: { title: string; location: string; date: string; status: "completed" | "current" | "pending" }[]
}

export function CachedTimeline(props: TimelineProps) {
  return useCachedComponent(
    "Timeline",
    props,
    ({ title, icon, events }) => (
      <Card id="lieferkette" className="bg-white border-gray-200">
        <CardHeader className="flex flex-row items-center gap-3 space-y-0">
          <div className="p-2 bg-gray-100 rounded-md">{icon}</div>
          <CardTitle className="text-lg">{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="relative pl-6">
            {events.map((event, index) => (
              <div key={index} className="relative pb-8">
                {index !== events.length - 1 && (
                  <div className="absolute left-[1px] top-4 -bottom-4 w-0.5 bg-gray-300" />
                )}
                <div className="absolute left-0 top-0.5">
                  {event.status === "completed" ? (
                    <CheckCircle className="h-4 w-4 text-emerald-500" />
                  ) : (
                    <Circle
                      className={cn(
                        "h-4 w-4",
                        event.status === "current" ? "text-blue-500 animate-pulse" : "text-gray-400",
                      )}
                    />
                  )}
                </div>
                <div className="pl-6">
                  <p className="font-semibold">{event.title}</p>
                  <p className="text-sm text-gray-500">{event.location}</p>
                  <p className="text-xs text-gray-400">{event.date}</p>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    ),
    {
      ttl: 30 * 60 * 1000, // 30 Minuten - Timeline ändert sich selten
      dependencies: [props.events.length, props.events.map((e) => e.status).join(",")], // Re-cache bei Status-Änderungen
    },
  )
}

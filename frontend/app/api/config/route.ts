import { NextResponse } from "next/server"
import { getClientConfig } from "@/lib/config"

export async function GET() {
  try {
    const config = await getClientConfig()
    return NextResponse.json(config)
  } catch (error) {
    console.error("Failed to get config:", error)
    return NextResponse.json({ error: "Failed to load configuration" }, { status: 500 })
  }
}

import { NextResponse } from "next/server"
import { getClientConfig } from "@/lib/config"

export async function GET() {
  try {
    // await the async helper so we send actual data, not a Promise
    const config = await getClientConfig()
    return NextResponse.json(config)
  } catch (error) {
    console.error("Failed to get config:", error)
    return NextResponse.json({ error: "Failed to load configuration" }, { status: 500 })
  }
}

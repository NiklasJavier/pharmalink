import { NextResponse } from "next/server"

export async function GET() {
  return NextResponse.json({
    version: "v1.2.3",
    timestamp: new Date().toISOString(),
    status: "success",
  })
}

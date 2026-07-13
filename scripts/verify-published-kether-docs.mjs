import { createHash } from "node:crypto"

const channel = process.argv[2] ?? "snapshot"
const expectedReleaseId = process.argv[3]
const base = "https://zhibeigg.github.io/Orryx/kether"

if (!expectedReleaseId) throw new Error("Expected releaseId argument is required")
if (!new Set(["stable", "snapshot"]).has(channel)) throw new Error(`Invalid channel: ${channel}`)

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))
const digest = (bytes) => createHash("sha256").update(bytes).digest("hex")

async function fetchBytes(url) {
  const response = await fetch(url, {
    cache: "no-store",
    headers: { "Cache-Control": "no-cache", "User-Agent": "orryx-kether-docs-verifier" },
  })
  if (!response.ok) throw new Error(`${response.status} ${url}`)
  return Buffer.from(await response.arrayBuffer())
}

async function verify() {
  const pointerUrl = `${base}/channels/${channel}.json?verify=${Date.now()}`
  const pointer = JSON.parse((await fetchBytes(pointerUrl)).toString("utf8"))
  if (pointer.releaseId !== expectedReleaseId) {
    throw new Error(`published releaseId is ${pointer.releaseId}, expected ${expectedReleaseId}`)
  }
  if (typeof pointer.releaseManifest !== "string" || !pointer.releaseManifest.startsWith("/Orryx/kether/")) {
    throw new Error("unsafe releaseManifest path")
  }
  const manifestUrl = new URL(pointer.releaseManifest, "https://zhibeigg.github.io").toString()
  const manifest = JSON.parse((await fetchBytes(`${manifestUrl}?verify=${Date.now()}`)).toString("utf8"))
  if (manifest.releaseId !== expectedReleaseId || manifest.plugin?.commit !== pointer.commit) {
    throw new Error("release manifest identity mismatch")
  }

  const payloads = {}
  for (const [name, asset] of Object.entries(manifest.assets)) {
    if (!/^[A-Za-z0-9._-]+$/.test(asset.path)) throw new Error(`unsafe asset path: ${asset.path}`)
    const url = new URL(asset.path, manifestUrl).toString()
    const bytes = await fetchBytes(`${url}?verify=${Date.now()}`)
    if (bytes.length !== asset.bytes) throw new Error(`${name} byte count mismatch`)
    if (digest(bytes) !== asset.sha256) throw new Error(`${name} sha256 mismatch`)
    payloads[name] = bytes
  }

  const schema = JSON.parse(payloads.schema.toString("utf8"))
  for (const field of ["actions", "selectors", "triggers", "properties"]) {
    if (!Array.isArray(schema[field]) || schema[field].length !== manifest.counts[field]) {
      throw new Error(`${field} count mismatch`)
    }
  }
  return { channel, releaseId: expectedReleaseId, counts: manifest.counts }
}

let lastError
for (let attempt = 1; attempt <= 18; attempt += 1) {
  try {
    const result = await verify()
    console.log(JSON.stringify(result, null, 2))
    process.exit(0)
  } catch (error) {
    lastError = error
    console.warn(`Published docs verification attempt ${attempt}/18 failed: ${error.message}`)
    if (attempt < 18) await delay(10_000)
  }
}

throw lastError

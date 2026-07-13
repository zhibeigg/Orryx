import { spawnSync } from "node:child_process"
import { mkdir, rm, writeFile } from "node:fs/promises"
import { posix, resolve } from "node:path"

const ref = process.argv[2] ?? "origin/kether-docs"
const output = resolve(process.argv[3] ?? "build/kether-docs-previous")
const currentReleaseId = process.argv[4] ?? null

await mkdir(output, { recursive: true })
await Promise.all([
  rm(resolve(output, "actions-schema.json"), { force: true }),
  rm(resolve(output, "release-id.txt"), { force: true })
])

function gitShow(path) {
  const result = spawnSync("git", ["show", `${ref}:${path}`], { encoding: "utf8" })
  if (result.status !== 0) return null
  return result.stdout
}

function releaseManifestPath(releaseId) {
  const match = /^Orryx@(.+)\+([0-9a-f]{40})$/.exec(releaseId)
  if (!match) throw new Error(`invalid releaseId: ${releaseId}`)
  const safeVersion = match[1].replace(/[^A-Za-z0-9._-]/g, "_")
  return `kether/releases/${safeVersion}/${match[2]}/manifest.json`
}

try {
  const channelText = gitShow("kether/channels/stable.json")
  if (!channelText) {
    console.log("No previous stable Kether docs channel found; diff will use an empty baseline")
    process.exit(0)
  }
  const channel = JSON.parse(channelText)
  let previousReleaseId = channel.releaseId
  let manifestPath

  if (currentReleaseId && channel.releaseId === currentReleaseId) {
    if (typeof channel.releaseManifest !== "string" || !channel.releaseManifest.startsWith("/Orryx/kether/")) {
      throw new Error("current stable releaseManifest is invalid")
    }
    const currentManifestText = gitShow(channel.releaseManifest.slice("/Orryx/".length))
    if (!currentManifestText) throw new Error("cannot read current stable release manifest")
    const currentManifest = JSON.parse(currentManifestText)
    previousReleaseId = currentManifest.previousReleaseId
    if (!previousReleaseId) {
      console.log("Current release has no previousReleaseId; diff will use an empty baseline")
      process.exit(0)
    }
    manifestPath = releaseManifestPath(previousReleaseId)
  } else {
    if (typeof channel.releaseManifest !== "string" || !channel.releaseManifest.startsWith("/Orryx/kether/")) {
      throw new Error("previous stable releaseManifest is invalid")
    }
    manifestPath = channel.releaseManifest.slice("/Orryx/".length)
  }

  const manifestText = gitShow(manifestPath)
  if (!manifestText) throw new Error(`cannot read previous release manifest: ${manifestPath}`)
  const manifest = JSON.parse(manifestText)
  const schemaName = manifest.assets?.schema?.path
  if (typeof schemaName !== "string" || !/^[A-Za-z0-9._-]+$/.test(schemaName)) {
    throw new Error("previous schema path is invalid")
  }
  const schemaPath = posix.join(posix.dirname(manifestPath), schemaName)
  const schemaText = gitShow(schemaPath)
  if (!schemaText) throw new Error(`cannot read previous schema: ${schemaPath}`)

  await mkdir(output, { recursive: true })
  await writeFile(resolve(output, "actions-schema.json"), schemaText, "utf8")
  await writeFile(resolve(output, "release-id.txt"), `${previousReleaseId}\n`, "utf8")
  console.log(JSON.stringify({ previousReleaseId, schemaPath }, null, 2))
} catch (error) {
  console.error(`Previous Kether docs snapshot is invalid: ${error.message}`)
  process.exitCode = 1
}

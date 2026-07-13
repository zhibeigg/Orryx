import { createHash } from "node:crypto"
import { copyFile, mkdir, readdir, readFile, stat } from "node:fs/promises"
import { join, relative, resolve, sep } from "node:path"

const sourceRoot = resolve(process.argv[2] ?? "build/generated-docs")
const historyRoot = resolve(process.argv[3] ?? "build/kether-docs-history")

function fail(message) {
  throw new Error(`Kether docs history merge failed: ${message}`)
}

async function hash(path) {
  return createHash("sha256").update(await readFile(path)).digest("hex")
}

async function walk(directory) {
  const result = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) result.push(...await walk(path))
    else if (entry.isFile()) result.push(path)
  }
  return result
}

function isImmutable(path) {
  const normalized = path.split(sep).join("/")
  return normalized.startsWith("kether/releases/") ||
    normalized.startsWith("kether/snapshots/") ||
    normalized.startsWith("kether/contracts/")
}

await mkdir(historyRoot, { recursive: true })
const sourceFiles = await walk(sourceRoot)
let copied = 0
let unchanged = 0

for (const source of sourceFiles) {
  const rel = relative(sourceRoot, source)
  if (rel.startsWith("..") || rel.includes(`..${sep}`)) fail(`source path escapes root: ${rel}`)
  const target = join(historyRoot, rel)
  await mkdir(resolve(target, ".."), { recursive: true })

  let targetExists = false
  try {
    targetExists = (await stat(target)).isFile()
  } catch {
    targetExists = false
  }

  if (targetExists && isImmutable(rel)) {
    const [sourceHash, targetHash] = await Promise.all([hash(source), hash(target)])
    if (sourceHash !== targetHash) fail(`immutable artifact would be overwritten: ${rel}`)
    unchanged += 1
    continue
  }

  await copyFile(source, target)
  copied += 1
}

console.log(JSON.stringify({ copied, unchanged, sourceRoot, historyRoot }, null, 2))

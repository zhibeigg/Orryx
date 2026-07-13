import { createHash } from "node:crypto"
import { readdir, readFile, stat } from "node:fs/promises"
import { dirname, isAbsolute, join, normalize, resolve, sep } from "node:path"

const site = resolve(process.argv[2] ?? "build/generated-docs")
const idPattern = /^[a-z0-9][a-z0-9._-]*$/
const digestPattern = /^[0-9a-f]{64}$/
const commitPattern = /^[0-9a-f]{40}$/

function fail(message) {
  throw new Error(`Kether docs validation failed: ${message}`)
}

function check(condition, message) {
  if (!condition) fail(message)
}

async function readJson(path) {
  let text
  try {
    text = await readFile(path, "utf8")
  } catch (error) {
    fail(`cannot read ${path}: ${error.message}`)
  }
  try {
    return { value: JSON.parse(text), text }
  } catch (error) {
    fail(`invalid JSON ${path}: ${error.message}`)
  }
}

async function sha256(path) {
  const bytes = await readFile(path)
  return createHash("sha256").update(bytes).digest("hex")
}

function resolvePublishedPath(rootPath) {
  check(typeof rootPath === "string" && rootPath.startsWith("/Orryx/kether/"), `unsafe published path: ${rootPath}`)
  check(!rootPath.includes("..") && !rootPath.includes("\\") && !rootPath.includes("?") && !rootPath.includes("#"), `unsafe published path: ${rootPath}`)
  const relative = rootPath.slice("/Orryx/".length)
  const target = resolve(site, relative)
  check(target === site || target.startsWith(site + sep), `published path escapes site: ${rootPath}`)
  return target
}

function resolveAsset(manifestPath, assetPath) {
  check(typeof assetPath === "string" && /^[A-Za-z0-9._-]+$/.test(assetPath), `unsafe asset path: ${assetPath}`)
  check(!isAbsolute(assetPath), `absolute asset path: ${assetPath}`)
  const target = resolve(dirname(manifestPath), normalize(assetPath))
  check(target.startsWith(dirname(manifestPath) + sep), `asset escapes release directory: ${assetPath}`)
  return target
}

function uniqueIds(items, label) {
  const ids = new Set()
  for (const item of items) {
    check(typeof item.id === "string" && idPattern.test(item.id), `${label} has invalid id: ${item.id}`)
    check(!ids.has(item.id), `${label} has duplicate id: ${item.id}`)
    ids.add(item.id)
  }
}

const channelDirectory = join(site, "kether", "channels")
const channelFiles = (await readdir(channelDirectory)).filter((name) => name.endsWith(".json"))
check(channelFiles.length === 1, `generated site must contain exactly one channel pointer, got ${channelFiles.join(", ")}`)
const channelPath = join(channelDirectory, channelFiles[0])
const { value: channel, text: channelText } = await readJson(channelPath)
check(Buffer.byteLength(channelText) <= 32 * 1024, "channel pointer exceeds 32 KiB")
check(channel.formatVersion === 1, "unsupported channel formatVersion")
check(channel.channel === "stable" || channel.channel === "snapshot", "invalid channel")
check(channelFiles[0] === `${channel.channel}.json`, "channel filename does not match payload")
check(commitPattern.test(channel.commit), "channel commit must be a full lowercase SHA")
check(typeof channel.releaseId === "string" && channel.releaseId.includes(channel.commit), "channel releaseId does not include commit")

const releaseManifestPath = resolvePublishedPath(channel.releaseManifest)
const { value: manifest, text: manifestText } = await readJson(releaseManifestPath)
check(Buffer.byteLength(manifestText) <= 64 * 1024, "release manifest exceeds 64 KiB")
check(manifest.formatVersion === 1, "unsupported release formatVersion")
check(manifest.releaseId === channel.releaseId, "channel and release manifest releaseId mismatch")
check(manifest.channel === channel.channel, "channel and release manifest channel mismatch")
check(manifest.plugin?.id === "Orryx", "release plugin id must be Orryx")
check(manifest.plugin?.version === channel.pluginVersion, "release version mismatch")
check(manifest.plugin?.commit === channel.commit, "release commit mismatch")
check(manifest.schemaVersion === 3, "release schemaVersion must be 3")

const budgets = {
  schema: 4 * 1024 * 1024,
  schemaContract: 512 * 1024,
  markdown: 8 * 1024 * 1024,
  changes: 2 * 1024 * 1024,
  checksums: 128 * 1024,
}
const requiredAssets = Object.keys(budgets)
for (const name of requiredAssets) {
  const asset = manifest.assets?.[name]
  check(asset && typeof asset === "object", `missing asset ${name}`)
  check(Number.isInteger(asset.bytes) && asset.bytes > 0 && asset.bytes <= budgets[name], `${name} size is outside budget`)
  check(digestPattern.test(asset.sha256), `${name} has invalid sha256`)
  const path = resolveAsset(releaseManifestPath, asset.path)
  const info = await stat(path)
  check(info.isFile(), `${name} is not a file`)
  check(info.size === asset.bytes, `${name} byte count mismatch`)
  check(await sha256(path) === asset.sha256, `${name} sha256 mismatch`)
}

const schemaPath = resolveAsset(releaseManifestPath, manifest.assets.schema.path)
const { value: schema } = await readJson(schemaPath)
check(schema.version === 2, "compatibility version must remain 2")
check(schema.schemaVersion === 3, "actions schemaVersion must be 3")
check(schema.plugin?.id === "Orryx", "schema plugin id must be Orryx")
check(schema.plugin?.version === manifest.plugin.version, "schema plugin version mismatch")
check(schema.plugin?.commit === manifest.plugin.commit, "schema commit mismatch")
for (const field of ["types", "categories"]) check(schema[field] && typeof schema[field] === "object" && !Array.isArray(schema[field]), `schema ${field} must be an object`)
for (const field of ["actions", "selectors", "triggers", "properties"]) check(Array.isArray(schema[field]), `schema ${field} must be an array`)

uniqueIds(schema.actions, "actions")
uniqueIds(schema.selectors, "selectors")
uniqueIds(schema.triggers, "triggers")
uniqueIds(schema.properties, "properties")

for (const action of schema.actions) {
  check(typeof action.name === "string" && action.name.length > 0, `${action.id} has invalid name`)
  check(typeof action.syntax === "string" && action.syntax.length > 0, `${action.id} has no syntax`)
  check(action.syntax.split(/\s+/)[0] === action.name, `${action.id} syntax does not start with action name`)
  check(schema.categories[action.category], `${action.id} references unknown category ${action.category}`)
  check(["public", "private"].includes(action.visibility), `${action.id} has invalid visibility`)
  check(["normal", "branch", "loop", "container"].includes(action.flow), `${action.id} has invalid flow`)
  check(Array.isArray(action.inputs), `${action.id} inputs must be an array`)
  check(Array.isArray(action.examples), `${action.id} examples must be an array`)
  check(Array.isArray(action.requirements), `${action.id} requirements must be an array`)
  for (const [index, input] of action.inputs.entries()) {
    check(typeof input.key === "string" && input.key.length > 0, `${action.id} input ${index} has invalid key`)
    check(schema.types[input.type], `${action.id} input ${index} references unknown type ${input.type}`)
    check(typeof input.required === "boolean", `${action.id} input ${index} has invalid required flag`)
    check(Object.hasOwn(input, "default"), `${action.id} input ${index} omits default`)
  }
  if (action.output !== null) check(schema.types[action.output?.type], `${action.id} output references unknown type`)
  if (action.source?.file) {
    check(!isAbsolute(action.source.file) && !action.source.file.includes("..") && !action.source.file.includes("\\"), `${action.id} has unsafe source file`)
  }
}

for (const selector of schema.selectors) {
  check(typeof selector.syntax === "string" && selector.syntax.startsWith("@"), `${selector.id} has invalid syntax`)
  check(Array.isArray(selector.params), `${selector.id} params must be an array`)
  for (const param of selector.params) check(schema.types[param.type], `${selector.id} references unknown type ${param.type}`)
}
for (const trigger of schema.triggers) {
  check(Array.isArray(trigger.variables) && Array.isArray(trigger.specialKeys), `${trigger.id} variables must be arrays`)
  for (const variable of [...trigger.variables, ...trigger.specialKeys]) check(schema.types[variable.type], `${trigger.id} references unknown type ${variable.type}`)
}
for (const property of schema.properties) {
  check(Array.isArray(property.keys), `${property.id} keys must be an array`)
  for (const key of property.keys) check(schema.types[key.type], `${property.id} references unknown type ${key.type}`)
}

for (const field of ["actions", "selectors", "triggers", "properties"]) {
  check(manifest.counts?.[field] === schema[field].length, `${field} count mismatch`)
}

const checksumsPath = resolveAsset(releaseManifestPath, manifest.assets.checksums.path)
const { value: checksums } = await readJson(checksumsPath)
check(checksums.formatVersion === 1 && checksums.algorithm === "SHA-256", "invalid checksums contract")
for (const name of ["schema", "schemaContract", "markdown", "changes"]) {
  const asset = manifest.assets[name]
  check(checksums.files?.[asset.path] === asset.sha256, `checksums.json mismatch for ${asset.path}`)
}

const changesPath = resolveAsset(releaseManifestPath, manifest.assets.changes.path)
const { value: changes } = await readJson(changesPath)
check(changes.formatVersion === 1, "changes formatVersion must be 1")
check(changes.toReleaseId === manifest.releaseId, "changes toReleaseId mismatch")

for (const contract of ["channel-manifest-v1.schema.json", "release-manifest-v1.schema.json", "actions-schema-v3.schema.json"]) {
  await readJson(join(site, "kether", "contracts", contract))
}
for (const legacy of ["manifest.json", "actions-schema.json", "latest.md"]) {
  const info = await stat(join(site, "kether", legacy))
  check(info.isFile() && info.size > 0, `missing legacy compatibility file ${legacy}`)
}

console.log(JSON.stringify({
  channel: channel.channel,
  releaseId: manifest.releaseId,
  version: manifest.plugin.version,
  commit: manifest.plugin.commit,
  counts: manifest.counts,
  assets: Object.fromEntries(requiredAssets.map((name) => [name, manifest.assets[name].bytes])),
}, null, 2))

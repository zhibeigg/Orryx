package org.gitee.orryx.module.wiki

import java.io.File

internal object KetherDocsContracts {

    val channelManifest: String = """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}id": "$KETHER_DOCS_BASE_URL/contracts/channel-manifest-v1.schema.json",
          "title": "Orryx Kether channel manifest v1",
          "type": "object",
          "additionalProperties": false,
          "required": ["formatVersion", "channel", "releaseId", "pluginVersion", "commit", "publishedAt", "releaseManifest"],
          "properties": {
            "${'$'}schema": { "type": "string", "format": "uri" },
            "formatVersion": { "const": 1 },
            "channel": { "enum": ["stable", "snapshot"] },
            "releaseId": { "type": "string", "minLength": 1, "maxLength": 200 },
            "pluginVersion": { "type": "string", "pattern": "^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?${'$'}" },
            "commit": { "type": "string", "pattern": "^[0-9a-f]{40}${'$'}" },
            "publishedAt": { "type": "string", "format": "date-time" },
            "releaseManifest": { "type": "string", "pattern": "^/Orryx/kether/(releases|snapshots)/" }
          }
        }
    """.trimIndent()

    val releaseManifest: String = """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}id": "$KETHER_DOCS_BASE_URL/contracts/release-manifest-v1.schema.json",
          "title": "Orryx Kether release manifest v1",
          "type": "object",
          "additionalProperties": false,
          "required": ["formatVersion", "releaseId", "channel", "plugin", "schemaVersion", "registryVersion", "generatedAt", "assets", "counts", "compatibility"],
          "properties": {
            "${'$'}schema": { "type": "string", "format": "uri" },
            "formatVersion": { "const": 1 },
            "releaseId": { "type": "string", "minLength": 1, "maxLength": 200 },
            "channel": { "enum": ["stable", "snapshot"] },
            "plugin": {
              "type": "object",
              "additionalProperties": false,
              "required": ["id", "version", "commit"],
              "properties": {
                "id": { "const": "Orryx" },
                "version": { "type": "string" },
                "commit": { "type": "string", "pattern": "^[0-9a-f]{40}${'$'}" }
              }
            },
            "schemaVersion": { "const": 3 },
            "registryVersion": { "const": 4 },
            "generatedAt": { "type": "string", "format": "date-time" },
            "previousReleaseId": { "type": ["string", "null"] },
            "assets": {
              "type": "object",
              "required": ["registry", "registryContract", "schema", "schemaContract", "markdown", "changes", "checksums"],
              "additionalProperties": { "${'$'}ref": "#/${'$'}defs/asset" }
            },
            "counts": {
              "type": "object",
              "additionalProperties": false,
              "required": ["actions", "selectors", "triggers", "properties"],
              "properties": {
                "actions": { "type": "integer", "minimum": 0 },
                "selectors": { "type": "integer", "minimum": 0 },
                "triggers": { "type": "integer", "minimum": 0 },
                "properties": { "type": "integer", "minimum": 0 }
              }
            },
            "compatibility": {
              "type": "object",
              "additionalProperties": false,
              "required": ["minimumEditorManifestFormat", "minimumEditorSchemaVersion", "minimumEditorRegistryVersion"],
              "properties": {
                "minimumEditorManifestFormat": { "type": "integer", "minimum": 1 },
                "minimumEditorSchemaVersion": { "type": "integer", "minimum": 1 },
                "minimumEditorRegistryVersion": { "type": "integer", "minimum": 1 }
              }
            }
          },
          "${'$'}defs": {
            "asset": {
              "type": "object",
              "additionalProperties": false,
              "required": ["path", "mediaType", "bytes", "sha256"],
              "properties": {
                "path": { "type": "string", "pattern": "^[A-Za-z0-9._-]+${'$'}" },
                "mediaType": { "type": "string", "minLength": 1 },
                "bytes": { "type": "integer", "minimum": 1 },
                "sha256": { "type": "string", "pattern": "^[0-9a-f]{64}${'$'}" }
              }
            }
          }
        }
    """.trimIndent()

    val actionsSchema: String = """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}id": "$KETHER_DOCS_BASE_URL/contracts/actions-schema-v3.schema.json",
          "title": "Orryx Kether actions schema v3",
          "type": "object",
          "required": ["version", "schemaVersion", "plugin", "types", "categories", "actions", "selectors", "triggers", "properties"],
          "properties": {
            "${'$'}schema": { "type": "string", "format": "uri" },
            "version": { "const": 2 },
            "schemaVersion": { "const": 3 },
            "pluginId": { "const": "Orryx" },
            "pluginVersion": { "type": "string" },
            "commit": { "type": "string", "pattern": "^[0-9a-f]{40}${'$'}" },
            "plugin": {
              "type": "object",
              "required": ["id", "version", "commit"],
              "properties": {
                "id": { "const": "Orryx" },
                "version": { "type": "string" },
                "commit": { "type": "string", "pattern": "^[0-9a-f]{40}${'$'}" }
              }
            },
            "types": { "type": "object", "minProperties": 1 },
            "categories": { "type": "object", "minProperties": 1 },
            "actions": {
              "type": "array",
              "items": {
                "type": "object",
                "required": ["id", "name", "aliases", "namespace", "category", "visibility", "description", "syntax", "inputs", "output", "flow", "examples", "execution", "requirements"],
                "properties": {
                  "id": { "${'$'}ref": "#/${'$'}defs/id" },
                  "name": { "type": "string", "minLength": 1 },
                  "aliases": { "type": "array", "items": { "type": "string" } },
                  "namespace": { "type": "string", "minLength": 1 },
                  "category": { "type": "string", "minLength": 1 },
                  "visibility": { "enum": ["public", "private"] },
                  "description": { "type": "string" },
                  "syntax": { "type": "string", "minLength": 1 },
                  "inputs": { "type": "array" },
                  "output": { "type": ["object", "null"] },
                  "flow": { "enum": ["normal", "branch", "loop", "container"] },
                  "examples": { "type": "array", "items": { "type": "string" } },
                  "execution": { "type": "object" },
                  "requirements": { "type": "array", "items": { "type": "string" } },
                  "source": { "type": "object" }
                }
              }
            },
            "selectors": { "type": "array", "items": { "type": "object", "required": ["id", "name", "description", "syntax", "params", "examples"] } },
            "triggers": { "type": "array", "items": { "type": "object", "required": ["id", "name", "category", "description", "variables", "specialKeys"] } },
            "properties": { "type": "array", "items": { "type": "object", "required": ["id", "name", "group", "description", "usage", "keys"] } }
          },
          "${'$'}defs": {
            "id": { "type": "string", "pattern": "^[a-z0-9][a-z0-9._-]*${'$'}" }
          }
        }
    """.trimIndent()

    fun writeGlobalContracts(directory: File) {
        KetherDocsContract.writeUtf8(File(directory, "channel-manifest-v1.schema.json"), channelManifest)
        KetherDocsContract.writeUtf8(File(directory, "release-manifest-v1.schema.json"), releaseManifest)
        KetherDocsContract.writeUtf8(File(directory, "actions-schema-v3.schema.json"), actionsSchema)
        KetherDocsContract.writeUtf8(File(directory, "kether-registry-v4.schema.json"), KetherRegistryContracts.registryV4)
    }
}

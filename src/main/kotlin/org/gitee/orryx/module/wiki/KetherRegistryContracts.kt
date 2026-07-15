package org.gitee.orryx.module.wiki

internal object KetherRegistryContracts {

    val registryV4: String = """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}id": "$KETHER_DOCS_BASE_URL/contracts/kether-registry-v4.schema.json",
          "title": "Orryx Kether Registry v4",
          "type": "object",
          "additionalProperties": false,
          "required": ["registryVersion", "schemaVersion", "pluginId", "pluginVersion", "commit", "plugin", "namespaces", "types", "categories", "actions", "selectors", "triggers", "properties", "compatibility"],
          "properties": {
            "${'$'}schema": { "type": "string", "format": "uri" },
            "registryVersion": { "const": 4 },
            "schemaVersion": { "const": 4 },
            "pluginId": { "const": "Orryx" },
            "pluginVersion": { "type": "string" },
            "commit": { "type": "string", "pattern": "^[0-9a-f]{40}${'$'}" },
            "plugin": { "${'$'}ref": "#/${'$'}defs/plugin" },
            "namespaces": { "type": "array", "uniqueItems": true, "items": { "type": "string", "minLength": 1 } },
            "types": { "type": "object", "minProperties": 1, "additionalProperties": { "${'$'}ref": "#/${'$'}defs/type" } },
            "categories": { "type": "object", "minProperties": 1, "additionalProperties": { "${'$'}ref": "#/${'$'}defs/category" } },
            "actions": { "type": "array", "minItems": 1, "items": { "${'$'}ref": "#/${'$'}defs/action" } },
            "selectors": { "type": "array", "minItems": 1, "items": { "${'$'}ref": "#/${'$'}defs/selector" } },
            "triggers": { "type": "array", "minItems": 1, "items": { "${'$'}ref": "#/${'$'}defs/trigger" } },
            "properties": { "type": "array", "minItems": 1, "items": { "${'$'}ref": "#/${'$'}defs/property" } },
            "compatibility": {
              "type": "object",
              "additionalProperties": false,
              "required": ["actionsSchemaVersion", "actionsSchemaAsset"],
              "properties": {
                "actionsSchemaVersion": { "const": 3 },
                "actionsSchemaAsset": { "const": "actions-schema.json" }
              }
            }
          },
          "${'$'}defs": {
            "id": { "type": "string", "pattern": "^[a-z0-9][a-z0-9._-]*${'$'}" },
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
            "type": {
              "type": "object",
              "additionalProperties": false,
              "required": ["name", "widget", "color", "parents", "children", "assignableFrom", "ketherFillable", "rawType"],
              "properties": {
                "name": { "type": "string" },
                "widget": { "enum": ["number", "text", "toggle", "select", "selector", "vector3", "location", "matrix", "duration", "port", "list"] },
                "color": { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}${'$'}" },
                "step": { "type": "number", "exclusiveMinimum": 0 },
                "parents": { "type": "array", "uniqueItems": true, "items": { "type": "string" } },
                "children": { "type": "array", "uniqueItems": true, "items": { "type": "string" } },
                "assignableFrom": { "type": "array", "uniqueItems": true, "items": { "type": "string" } },
                "ketherFillable": { "type": "boolean" },
                "rawType": { "type": "string", "minLength": 1 },
                "inputHint": { "type": "string" }
              }
            },
            "category": {
              "type": "object",
              "additionalProperties": false,
              "required": ["color", "icon"],
              "properties": { "color": { "type": "string" }, "icon": { "type": "string" } }
            },
            "alias": {
              "type": "object",
              "additionalProperties": false,
              "required": ["name", "kind"],
              "properties": { "name": { "type": "string", "minLength": 1 }, "kind": { "enum": ["parser", "deprecated", "compatibility"] } }
            },
            "input": {
              "type": "object",
              "required": ["name", "key", "type", "acceptedTypes", "required", "default", "ketherFillable", "rawType"],
              "properties": {
                "name": { "type": "string" }, "key": { "type": "string", "minLength": 1 }, "type": { "type": "string" },
                "acceptedTypes": { "type": "array", "minItems": 1, "uniqueItems": true, "items": { "type": "string" } },
                "required": { "type": "boolean" }, "default": {}, "description": { "type": "string" },
                "keyword": { "type": "string" }, "keywordAlternatives": { "type": "array", "uniqueItems": true, "items": { "type": "string" } },
                "ketherFillable": { "type": "boolean" }, "rawType": { "type": "string" }, "inputHint": { "type": "string" }
              }
            },
            "action": {
              "type": "object",
              "required": ["id", "name", "aliases", "namespace", "shared", "category", "visibility", "description", "flow", "examples", "execution", "requirements", "source", "grammar", "output"],
              "properties": {
                "id": { "${'$'}ref": "#/${'$'}defs/id" }, "name": { "type": "string", "minLength": 1 },
                "aliases": { "type": "array", "items": { "${'$'}ref": "#/${'$'}defs/alias" } }, "namespace": { "type": "string" },
                "shared": { "type": "boolean" }, "category": { "type": "string" }, "visibility": { "enum": ["public", "private"] },
                "description": { "type": "string", "minLength": 1 }, "deprecated": {}, "builtin": { "type": "boolean" },
                "flow": { "enum": ["normal", "branch", "loop", "container"] }, "examples": { "type": "array", "items": { "type": "string" } }, "example": { "type": "string" },
                "execution": {
                  "type": "object", "required": ["thread", "suspends", "contexts"],
                  "properties": { "thread": { "enum": ["main", "async", "any", "unknown"] }, "suspends": { "type": "boolean" }, "contexts": { "type": "array", "items": { "type": "string" } } }
                },
                "requirements": { "type": "array", "items": { "type": "object", "required": ["id", "required"], "properties": { "id": { "type": "string" }, "required": { "type": "boolean" } } } },
                "source": { "type": "object", "required": ["symbol", "group", "registry"] },
                "grammar": { "type": "object", "required": ["syntax", "inputs", "variants"], "properties": { "syntax": { "type": "string" }, "inputs": { "type": "array", "items": { "${'$'}ref": "#/${'$'}defs/input" } }, "variants": { "type": "array", "minItems": 1 } } },
                "output": { "type": "object", "required": ["status"], "properties": { "status": { "enum": ["none", "declared", "unknown"] }, "type": { "type": "string" }, "description": { "type": "string" } } },
                "slots": { "type": "array" }, "provides": { "type": "array" }
              }
            },
            "selector": { "type": "object", "required": ["id", "name", "aliases", "description", "syntax", "params", "examples"] },
            "triggerEntry": {
              "type": "object",
              "required": ["name", "aliases", "type", "description", "readable", "writable", "nullable", "rawType", "ketherFillable"],
              "properties": {
                "name": { "type": "string" }, "aliases": { "type": "array", "items": { "type": "string" } }, "type": { "type": "string" }, "description": { "type": "string" },
                "readable": { "type": "boolean" }, "writable": { "type": "boolean" }, "nullable": { "type": "boolean" }, "rawType": { "type": "string" },
                "ketherFillable": { "type": "boolean" }, "inputHint": { "type": "string" }
              }
            },
            "trigger": {
              "type": "object",
              "required": ["id", "name", "aliases", "category", "description", "eventClass", "cancellable", "variables", "specialKeys"],
              "properties": {
                "id": { "${'$'}ref": "#/${'$'}defs/id" }, "name": { "type": "string" }, "aliases": { "type": "array" }, "category": { "type": "string" }, "description": { "type": "string" },
                "eventClass": { "type": ["string", "null"] }, "cancellable": { "type": "boolean" },
                "variables": { "type": "array", "items": { "${'$'}ref": "#/${'$'}defs/triggerEntry" } }, "specialKeys": { "type": "array", "items": { "${'$'}ref": "#/${'$'}defs/triggerEntry" } }
              }
            },
            "property": { "type": "object", "required": ["id", "name", "group", "description", "usage", "keys"] }
          }
        }
    """.trimIndent()
}

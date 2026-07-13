# UI source evidence

Reviewed: 2026-07-13.

## Backend selection

`src/main/kotlin/org/gitee/orryx/module/ui/IUIManager.kt` selects Bukkit, DragonCore, GermPlugin or ArcartX from `UI.use`, with DRAGON/GERM/AX aliases and Bukkit fallback when a client plugin is missing.

## Backend-specific evidence

- `module/ui/bukkit/BukkitSkillUI.kt` reads title, Lore, XMaterial and ordered integer Slots from `ui/bukkit/setting.yml`.
- `module/ui/dragoncore/DragonCoreSkillUI.kt` and `DragonCoreSkillHud.kt` send named placeholders; skill/icon arrays are encoded with `<br>` and must stay index-aligned.
- `module/ui/germplugin/GermPluginSkillUI.kt` clones named GUI parts, replaces `{skill}` in icon paths and depends on canvas/layout/index names.
- `module/ui/arcartx/ArcartXSkillUI.kt` and HUD send ArcartX-specific structured data including icon fields.
- `src/main/resources/ui/{bukkit,dragoncore,germplugin,arcartx}` contains different templates/settings, not one interchangeable schema.

## Derived rule

A UI request must be a discriminated backend variant. Placeholders, arrays, icons and assets are protocol dependencies; cross-backend adaptation requires an explicit capability/loss analysis instead of blind field translation.

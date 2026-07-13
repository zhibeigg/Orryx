# UI component contract

## Input

UTF-8 JSON object; launcher fixes `component` to `ui`.

Required discriminator:

- `variant`: exactly one of `bukkit`, `dragoncore`, `germplugin`, `arcartx`.

Recommended fields:

- `request`, `mode` (`design`, `validate`, `adapt`).
- `source_variant` when adapting.
- `settings`, `ui`, `hud`.
- `ordered_data`: aligned skills, bind keys, icons, cooldowns and placeholders.
- `assets`: GUI files, images, fonts, sounds, models and plugin versions.

## Variant checks

- Bukkit: title, Slots, XMaterial/item fields, Lore, page controls and inventory bounds.
- DragonCore: GUI/HUD files, exact placeholder names, `<br>` ordered arrays and icon/client assets.
- GermPlugin: required part/index names, `{skill}` substitutions, canvas/layout and callback dependencies.
- ArcartX: UI/network variable schema, icon fields and client asset package.

Mixed backend-only fields are errors. Alias input may normalize to a canonical variant, but output stays discriminated.

## Adaptation

Return a capability mapping: preserved, degraded and unsupported. Unsupported dynamic layout, callbacks, animations or placeholder semantics cannot be guessed into another backend. Ordered arrays retain index and length across every linked channel.

## Output

Shared arrays:

- `artifacts`: target-variant server/client config proposals.
- `diagnostics`: schema, alignment, placeholder, capability and asset findings.
- `references`: placeholders, GUI parts, indexes, icons and asset paths.
- `checks`: binary discriminator/order/asset checks.
- `requirements`: plugin, client asset, capability, and adaptation prerequisites.

The component does not install plugins or reload the server.

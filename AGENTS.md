# GateCraft Development Protection Rules

This file is the mandatory development policy for GateCraft and GateCraftCore.
Every coding, migration, UI, optimization, refactor, debugging, validation, or release task MUST follow these rules before making changes.

## 1. Primary principle

Stability has priority over elegance, cleanup, refactoring, code style, or architectural purity.

If a requested change can be completed without touching a proven working subsystem, that subsystem MUST remain unchanged and byte-identical whenever practical.

When in doubt, preserve the existing MIT App Inventor block structure rather than refactor it.

Do not perform unrelated cleanup, modernization, renaming, consolidation, optimization, or refactoring while completing a requested task.

## 2. Protected calculation and mathematical logic — DO NOT MODIFY WITHOUT EXPLICIT USER APPROVAL

GateCraft calculation formulas, mathematical rules, engineering logic, rounding behavior, dimensional logic, selection algorithms, material calculations, cost calculations, gate geometry, concrete calculations, weight calculations, electrical calculations, welding calculations, cutting logic, motor-selection logic, and all other calculation engines are PROTECTED.

### Mandatory rule

- Do NOT change calculation formulas or mathematical logic merely because a different implementation appears cleaner, more standard, more elegant, more efficient, or theoretically better.
- Do NOT silently correct a calculation that appears suspicious.
- Do NOT change constants, coefficients, units, rounding, tolerances, default values, boundary conditions, fallback behavior, ordering, or decision rules unless the user explicitly authorizes that specific calculation change.
- Do NOT replace existing formulas with textbook formulas or industry-standard alternatives without approval.
- Do NOT normalize historical GateCraft behavior automatically.

### If a possible calculation error is discovered

1. Stop modification of that calculation path.
2. Preserve the current calculation logic unchanged.
3. Report the suspected issue to the user clearly.
4. Explain what input, formula, handoff, result, or edge case appears wrong.
5. Show the current behavior and the proposed corrected behavior when possible.
6. Wait for explicit user approval before changing the calculation.
7. Only after explicit approval may the calculation logic be modified.

Finding a bug is NOT permission to fix the mathematical logic automatically.

## 3. Mandatory calculation and data-flow audit

Every development task MUST include a read-only audit of the calculation and data-transfer paths affected by the change, even when the requested task is only UI, language, theme, navigation, performance, or layout work.

The purpose of the audit is to DETECT regressions or pre-existing errors, not to silently fix protected calculation logic.

For every affected module, verify as applicable:

- input value -> parsing -> validation -> internal value
- units and unit conversions
- language-independent internal codes vs visible translated text
- formula inputs
- calculation order
- intermediate values
- rounding and formatting boundaries
- result values
- saved values
- restored values
- data passed between procedures
- data passed between screens
- StartValue / OtherScreenClosed handoffs
- TinyDB / SharedPreferences keys and record structure
- list indexes and record indexes
- dictionary keys and schema versions
- module -> Summary handoff
- module -> Quote/Offer handoff
- module -> Inventory/stock handoff
- module -> Project Center / Business System handoff
- module -> Cutting Optimizer handoff where applicable
- export data
- language switching without changing internal values
- theme switching without changing business/calculation values

The audit must confirm that the data arriving at the next stage is the same value, type, unit, meaning, and record field that the receiving logic expects.

If a mismatch or suspicious calculation is found during this audit, REPORT IT. Do not silently repair protected calculation logic unless the user explicitly approves the fix.

For a focused task, audit the changed path and directly connected upstream/downstream paths. For a release-wide validation or large migration, audit all known calculation and data-transfer paths that can reasonably be validated statically.

## 4. MIT App Inventor block safety

The `.bky` files are safety-critical project source.

- NEVER rewrite or reserialize whole `.bky` files with a generic XML serializer.
- Preserve canonical App Inventor XML namespaces and lexical structure.
- Avoid moving, rebuilding, or regenerating large block trees when a targeted edit is possible.
- Preserve existing block IDs whenever practical.
- Do not generate duplicate block IDs.
- Do not generate duplicate top-level events.
- Do not rename procedures, globals, components, or event targets unless the requested task requires it.
- Do not convert a proven block structure into a new architecture just to reduce block count.
- Prefer exact targeted text/tree edits over whole-file reconstruction.

A previous GateCraft regression caused `The blocks area did not load properly` because Blockly XML namespaces were reserialized incorrectly. Preventing that class of regression is mandatory.

## 5. Navigation and screen-opening logic is protected

Do not deeply rewrite working navigation unless the task explicitly requests a navigation fix.

Protected areas include:

- `Screen.Initialize`
- `OtherScreenClosed`
- `BackPressed`
- `open another screen with start value`
- StartValue handling
- navigation globals
- delayed Clock navigation
- startup Clock logic
- close-screen timing
- proven screen-name helper structures

If a UI task can be solved without changing navigation, navigation MUST remain unchanged.

If navigation must be modified, use the smallest possible change and compare behavior with the last proven working version.

## 6. Screen3 / Szerszámraktár protection

Screen3 and the Szerszámraktár/service/inventory workflows are protected unless the task explicitly targets them.

In unrelated work, do not modify their business logic, component identity tree, save/return/service/inventory procedures, record layout, state transitions, or screen behavior.

Known critical procedures must be preserved unless explicitly targeted, including historically protected service/inventory procedures such as:

- `SZERV_Finish`
- `LELTAR_SaveCurrent`
- `SZ_SaveReturn`
- `sz_BuildRecord`
- `SZERV_Buildservicelist`
- `SZ_SaveIssue`

## 7. Billing / Pro / purchase protection

Billing, purchase, restore-purchase, entitlement, Basic/Pro gating, Play-related billing events, and existing monetization logic are protected.

Do not modify, remove, simplify, refactor, or relocate billing logic unless the user explicitly requests a billing change.

Unrelated releases must preserve billing event logic semantically unchanged.

## 8. Component identity and schema protection

Existing component names, procedure names, TinyDB tags, record field positions, dictionary keys, schema versions, asset names, screen names, and component UUIDs are compatibility contracts.

Do not rename or reorder them without an explicit migration plan and user approval.

If a record structure must change, use a versioned migration. Never silently reinterpret old user data.

## 9. UI/theme rules

UI work must not alter calculation or business logic.

For labels that have no dedicated background and sit directly on the main themed background:

- Standard: black text
- Ocean: black text
- Forge: white text
- Industrial: black text

A true header may use its own themed background; header foreground and background must have clear opposite contrast.

Main content containers, cards, sections, detail areas, and page containers should not create an unnecessary opaque `background before the background`. Where the design requires the main theme background to remain visible, these containers should be transparent.

Spinners should use their normal/default visual surface unless a specific screen requires otherwise, with black visible item text by default.

Do not add arbitrary decorative backgrounds to labels, content groups, or module sections.

## 10. Loading-cover policy

A screen or module must not expose half-themed or partially initialized UI during startup.

Where a module has a loading-cover VA, keep it visible until language, theme, data, and initial UI state are ready, then hide it.

New modules should use the same principle.

Where practical, the loading cover may show the current localized module name. The displayed module name must follow the current language.

Do not deeply rewrite proven screen-opening logic solely to implement a loading cover. Prefer a minimal startup/timer/UI-layer solution.

## 11. Screen1 full-page overlay/page isolation

When opening a full-page Screen1 subsystem such as Settings, GateCraft Center, Profile, Backup, Project Center, Business System, or calculation-data deletion:

- hide the main page
- hide loading/splash/cover layers that should not remain behind it
- hide other full-page overlays
- show only the requested page at the same logical UI level
- do not allow the main page or another page to remain visible behind the active page

The GateCraft Center entry belongs in Settings, not permanently at the top of the home page.

## 12. Language safety

Visible translations must never be used as calculation/business identifiers when a stable internal code/index exists.

Changing language must not change:

- selected internal codes
- calculation inputs
- stored values
- project state
- record field meaning
- data-transfer semantics

Use stable indexes/codes for logic and localized text only for display.

## 13. Scope control

Each task must touch the minimum possible scope.

Before editing, identify:

- requested behavior
- exact affected screens/modules
- exact affected files
- protected neighboring systems that must not change

Do not expand scope because another area could also be improved.

A discovered unrelated bug should be reported separately unless the user explicitly asks to include it.

## 14. Required validation after every change

At minimum, perform all applicable static checks before declaring a build valid:

- AIA ZIP integrity
- all `.bky` XML parse
- all `.scm` JSON parse
- canonical Blockly/App Inventor namespace validation
- duplicate block IDs = 0
- duplicate top-level events = 0
- missing component references = 0
- missing procedure references = 0
- missing global references = 0 where validation supports it
- GateCraftCore method existence
- GateCraftCore method argument-count/signature validation
- component version consistency
- JSON/data asset parse
- protected billing path comparison where applicable
- protected Screen3 path comparison where applicable
- calculation/data-flow audit described in Section 3

When a known stable baseline exists, compare protected subsystems against that baseline and preserve them byte-identically or semantically identically as appropriate.

## 15. Runtime truthfulness

Static validation is not proof of Android runtime correctness.

Never claim that a change is definitely runtime-safe only because XML/JSON/build validation passed.

For AIA changes, recommend or perform the appropriate sequence when possible:

1. Import as a NEW MIT App Inventor project.
2. Open critical Blocks workspaces and confirm no red blocks-load warning.
3. Build APK.
4. Test affected screens on device.
5. Test language/theme changes.
6. Test save/load/data handoffs for affected modules.
7. Test protected billing and Screen3 paths before a production/Play release when the release touches shared infrastructure.

## 16. Bug reporting vs bug fixing

The developer/agent is expected to notice problems while working.

When a problem is found:

- report it clearly
- classify whether it is UI, navigation, data-flow, calculation, storage, translation, billing, or structural
- state whether it is inside or outside the requested task
- state whether fixing it would touch a protected area

For protected calculation/mathematical logic, reporting is mandatory and automatic fixing is forbidden without explicit user authorization.

## 17. Final GateCraft rule

DO NOT BREAK A WORKING PART TO IMPROVE AN UNRELATED PART.

Preserve proven behavior first. Make the smallest targeted change. Audit calculations and data flow every time. Report suspicious logic. Change protected mathematical or calculation behavior only after explicit user approval.

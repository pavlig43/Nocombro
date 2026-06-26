---
name: nocombro-labels
description: Work on Nocombro thermal labels, PPTX label templates, label generation, label dialog UI, template resources, and related product specification PDF output. Use when changing `features/label/thermal/*`, `docs/templates`, thermal label PPTX generation, or product PDF generation touchpoints that overlap labels/templates.
---

# Nocombro Labels

Use this skill for thermal label generation and related PPTX/PDF output.

## Quick Start

Read these first:

1. `features/label/thermal/src/desktopMain/kotlin/ru/pavlig43/thermallabel/api/component/ThermalLabelDialogComponent.kt`
2. `features/label/thermal/src/desktopMain/kotlin/ru/pavlig43/thermallabel/api/ui/ThermalLabelDialog.kt`
3. `features/label/thermal/src/desktopMain/kotlin/ru/pavlig43/thermallabel/api/data/ThermalLabelTemplateService.kt`
4. `features/label/thermal/src/desktopMain/kotlin/ru/pavlig43/thermallabel/internal/ThermalLabelPptxGenerator.kt`
5. `features/label/thermal/src/desktopMain/resources/templates`
6. `docs/templates`

Read product specification PDF code only when the task overlaps product output or PDF generation:

- `features/form/product/src/desktopMain/kotlin/ru/pavlig43/product/internal/update/tabs/specification/ProductSpecificationPdfGenerator.kt`
- `features/form/product/src/desktopMain/kotlin/ru/pavlig43/product/internal/update/tabs/specification/ProductSpecificationPdfRepository.kt`

## Workflow

1. Decide whether the change is dialog UI, template lookup, PPTX generation, template files, or product PDF output.
2. Keep template dimensions and `ThermalLabelSize` in sync.
3. Check generated-file paths and resource loading before changing UI.
4. If label output depends on product data, inspect the product form/specification path after label code.
5. Use navigation/DI skills only if the dialog cannot be constructed or reached.

## Guardrails

- Do not edit PPTX templates and generator assumptions independently.
- Do not treat template resources under `docs/templates` and feature resources as interchangeable without checking copy/resource loading.
- Do not broaden product specification PDF changes unless the label task needs it.

## Tests

Run the narrow feature or product tests if available; otherwise use compile/build checks around the affected module. For product PDF overlap, pair with `nocombro-forms`.

---
name: nocombro-forms
description: Work on Nocombro form features for documents, products, vendors, declarations, transactions, and expenses. Use when changing form UI or validation, when editing save/load behavior, when tracing how a form opens, or when checking how an `id` is passed from navigation into a form component.
---

# Nocombro Forms

Use this skill for existing CRUD-style forms and form-driven screens. Start in the concrete form module and expand to navigation only if opening or `id` passing is involved.

## Quick Start

Read these files first:

1. `AGENTS.md`
2. matching module under `features/form/*`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt` only if the task mentions opening the form, editing an `id`, or route arguments

Form modules:

- `features/form/document`
- `features/form/product`
- `features/form/vendor`
- `features/form/declaration`
- `features/form/transaction`
- `features/form/expense`

## Workflow

1. Pick the concrete form module from the user task.
2. Read only the screen, component, and state files needed for that form.
3. Inspect repositories, use-cases, or persistence only if the form data path is broken.
4. Inspect `MainTabConfig.kt` and root navigation only if the form is not opening or gets the wrong `id`.

## Common Cases

### Change existing form fields or validation

Start inside the feature module. Prefer local UI/state updates before checking root wiring.

### Form opens wrong entity or no entity

Check in this order:

1. route/config in `MainTabConfig.kt`
2. feature component arguments
3. repository or use-case that loads by `id`

### Add a new form-related route

Use the navigation and DI skills as companions only after the form module itself is ready.

## Guardrails

- Do not scan all form modules; pick one module first.
- Do not jump into root navigation unless the bug is about opening, arguments, or reachability.
- Keep reads narrow: screen/component/state first, repositories later.

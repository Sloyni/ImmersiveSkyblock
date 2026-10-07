---
mode: agent
description: Generate ISkyblock .ui files with strict project syntax and zero syntax errors
---

You are a specialized ISkyblock UI DSL agent.
Your only goal is to generate valid .ui files aligned with this repository.

Primary reference:

- docs/ISkyblock-UI-Agent-Guide.md

Hard rules:

1. Output only .ui content unless the user explicitly asks for explanation.
2. Use only controls and style constructors proven in this project.
3. Follow exact import patterns used by existing files.
4. Keep every property/statement terminated with `;`.
5. Keep braces balanced and nested correctly.
6. Prefer `$C` shared templates for consistency.
7. For dynamic content, predeclare slots (`Row0..RowN`) with `Visible: false`.
8. Do not invent unsupported controls, properties, or style constructor names.
9. Do not invent texture paths if an existing path can be reused.
10. Respect existing naming conventions (`SectionName`, `RowN`, `ApplyBoxN`, `ErrorN`, etc.).
11. Do not output `//` comments in `.ui` files.
12. Never rename/remove an existing `#Id` if Java may reference it, unless you also update Java selectors in the same change.
13. Before final output, verify selector parity with Java (`commandBuilder.set("#...")` and `addEventBinding("#...")`).
14. In island hero layouts, keep island name centered as an overlay on the image (not below image).
15. For DropdownBox, set `.Value` only to values present in `.Entries` after normalization.
16. For icon action buttons, keep square wrappers and readable icon size (about 16-18 inside 28-32).
17. For confirmation/rename popups, bottom actions should use 50/50 width split via `FlexWeight` and be pushed down with a spacer.

Generation strategy:

1. Detect context type:

- Full page: use `$C.@PageOverlay {}` + `$C.@Container { #Title { ... } #Content { ... } }`
- Section file: use `@SectionName = Group { LayoutMode: Top; Visible: false; ... };`

2. Build layout using known patterns:

- Row containers with `Anchor`, `LayoutMode`, `Background`, `Padding`
- Label + input/action split using `FlexWeight` and fixed right controls
- Toggle rows with `$C.@CheckBox`
- Text entry rows with `$C.@TextField`
- Language selection rows with `DropdownBox` + `$C.@DefaultDropdownBoxStyle`

3. For admin dashboard composition:

- Import section aliases
- Compose with `$Alias.@Template #Id {}` in the main content block

4. Before final output, run internal validation:

- No missing semicolon
- No unmatched braces
- No unknown control
- No unknown style constructor
- IDs are unique and coherent
- No Java selector points to a missing `#Id`

If the user asks to modify an existing .ui file, preserve current visual language and only change what is requested.

# ISkyblock UI Agent Guide

This document defines a strict, project-specific reference for generating .ui files without syntax errors.
It was built by analyzing all active .ui files in this workspace, with a strong focus on the admin dashboard.

## Scope analyzed (26 files)

- src/main/resources/Common/UI/Custom/Common.ui
- src/main/resources/Common/UI/Custom/Sounds.ui
- src/main/resources/Common/UI/Custom/Hud/IslandMenuHud.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.AddonsSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.CreateTemplateForm.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.CreateTemplateSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.DeleteIslandConfirm.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.DeleteTemplateConfirm.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.EditTemplateForm.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.HomesGroupsManager.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.HomesSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.HomesViewerPage.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.IslandsSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.IslandsViewerPage.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.LanguagesManager.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.LevelsSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.MobsManagerSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.OreGeneratorSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.ResetsSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.SettingsSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockAdminDashboard.TemplatesSection.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockChoosePlayer.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockInit.ui
- src/main/resources/Common/UI/Custom/Pages/ISkyblockTemplateSelect.ui
- src/main/resources/Common/UI/Custom/Pages/IslandMenu.ui

## Proven syntax model

Use only syntax patterns already present in the files above.

### 1) Imports / aliases

```ui
$C = "../Common.ui";
$Sounds = "../Sounds.ui";
$TemplatesSection = "ISkyblockAdminDashboard.TemplatesSection.ui";
```

Rules:

- Alias names start with `$`.
- Import path is a quoted string.
- Every import line ends with `;`.

### 2) Constants and template definitions

```ui
@ColorDefault = #ffffff;
@DefaultLabelStyle = (FontSize: 16, TextColor: #96a9be);
@SettingsSection = Group {
  LayoutMode: Top;
  Visible: false;
};
```

Rules:

- Constants/templates start with `@`.
- Template block definitions end with `};`.

### 3) Element declarations

```ui
Group #Sidebar {
  Anchor: (Width: 250);
  LayoutMode: Top;
}

Label #Title {
  Text: "ISkyblock Admin";
  Style: (...$C.@DefaultLabelStyle, FontSize: 16);
}
```

Rules:

- Valid seen element roots include `Group`, `Label`, `Button`, `TextButton`, `DropdownBox`, `TabNavigation`, `TabButton`, `TextField`, `CompactTextField`, `SliderNumberField`.
- IDs use `#IdName` and are optional (but required for dynamic updates/events).

### 4) Template instantiation

```ui
$C.@Container {
  #Title {
    $C.@Title { @Text = "ISkyblock Admin"; }
  }
}

$C.@TextField #SettingsProtectionRadiusField {
  @Anchor = (Horizontal: 0, Height: 28, Vertical: 0);
}

$SettingsSection.@SettingsSection #SettingsSection {}
```

Rules:

- Syntax is `AliasOrLocalTemplate.@TemplateName` optionally followed by `#Id` and `{ ... }`.
- Template parameters are overridden with `@Param = ...;` inside the block.

### 5) Style and object merge

```ui
Style: (...$C.@DefaultLabelStyle, FontSize: 13, TextColor: #ffffff);
Style: (...$C.@DefaultDropdownBoxStyle);
```

Rules:

- Use `...` for spread/merge.
- Keep key/value pairs inside parentheses.

### 6) Child-slot override by ID

Inside container templates, child IDs are overridden directly:

```ui
$C.@Container {
  #Title {
    $C.@Title { @Text = "My Title"; }
  }

  #Content {
    LayoutMode: Top;
  }
}
```

This pattern is required by the current UI architecture.

## High-confidence control/style inventory

### Controls directly used in active pages

- `Group`
- `Label`
- `Button`
- `TextButton`
- `DropdownBox`
- `TabNavigation`
- `TabButton`
- `SliderNumberField`
- `TextField`
- `CompactTextField`
- `BackButton` (via common template)

### Shared controls via common templates

- `$C.@CheckBox`
- `$C.@TextField`
- `$C.@CancelTextButton`
- `$C.@SmallSecondaryTextButton`

### Style constructors confirmed

- `PatchStyle(...)`
- `LabelStyle(...)`
- `ButtonStyle(...)`
- `TextButtonStyle(...)`
- `DropdownBoxStyle(...)`
- `DropdownBoxSounds(...)`
- `TabNavigationStyle(...)`
- `TabStyleState(...)`
- `SliderStyle(...)`
- `CheckBoxStyle(...)`
- `ColorPickerStyle(...)`

## Admin dashboard architecture (important)

Main file pattern in `ISkyblockAdminDashboard.ui`:

1. Import all section files as aliases.
2. Build overlay/container with `$C.@PageOverlay {}` and `$C.@Container { ... }`.
3. Use a left sidebar with duplicated active/inactive nav buttons.
4. Compose all sections in content with:

```ui
$SectionAlias.@SectionTemplate #SectionId {}
```

Each section file pattern:

```ui
@SomeSection = Group {
  LayoutMode: Top;
  Visible: false;
  ...
};
```

Rich dynamic section patterns:

- `TemplatesSection`: repeated rows `#TplRow0..n`, `#TplUp0..n`, `#TplDelete0..n`, etc.
- `ResetsSection`, `HomesSection`, `IslandsSection`, `OreGeneratorSection`: repeated slot rows with numeric suffixes.
- Default approach is predeclared rows with `Visible: false`.

## Anti-error generation rules (strict)

1. Always end properties/statements with `;`.
2. Keep braces balanced for every element/template/style tuple.
3. Do not invent new control names not seen in this project.
4. Do not invent new style constructor names.
5. Prefer `$C` templates instead of ad-hoc widgets when available.
6. For editable fields, follow `$C.@TextField #Id { @Anchor = (...); }` pattern.
7. For toggles, use `$C.@CheckBox #Id { @Anchor = (...); Value: false; }`.
8. For admin rows, keep row shape consistent: background, left label, right action/input.
9. For dynamic lists, predeclare fixed slots (`Row0..RowN`) and set `Visible: false` by default.
10. Reuse existing visual assets (`../Common/Buttons/...`, `../icons/...`) instead of inventing paths.
11. Keep import paths relative to file location exactly as existing files do.
12. For section templates, end with `};` and initialize `Visible: false` unless the page must open visible.
13. Do not emit `//` comments inside `.ui` output. Keep generated UI files comment-free to avoid parser ambiguity.
14. Never rename/remove an ID that may be referenced by Java without updating Java selectors in the same change.
15. Before final output, cross-check selector parity: every `#Id.Property` or event selector used in Java must exist in the target `.ui`.

## Java/UI selector parity (mandatory)

Runtime failures like `selected element in customUI command was not found` are usually selector mismatches.

Checklist before finalizing a UI edit:

1. Search Java page class for selectors targeting this page (both `commandBuilder.set("#...")` and `addEventBinding("#...")`).
2. Ensure each static selector ID exists in the `.ui` file.
3. If an element was replaced (example: `#BtnLevel` -> `#BtnRefreshLevel`), migrate Java selectors in the same commit.
4. Keep dynamic selector families consistent (`#Row0..#RowN`, `#BtnLang0..#BtnLangN`).

## Integration constraint with Java appendInline

For Java-side `appendInline`, do not rely on template imports inside inline fragments.
Current project practice is to predefine slots in .ui and update them from Java by ID.

## Legacy island-menu lessons (must preserve)

These points come from regressions caught while rebuilding the island menu.

1. Island name placement:

- For the island hero card, `#IslandName` must be centered on top of the island image (overlay), not rendered as a row below the image.

2. Dropdown value safety:

- For `DropdownBox`, the assigned `.Value` must always be one of `.Entries[].Value`.
- Normalize locale tokens consistently (same normalization for entries and current value) before setting `.Value`.

3. Icon button sizing:

- Keep icon action wrappers square (`Width == Height`) and use a readable icon size (typically 16-18 in a 28-32 wrapper).
- Avoid stretch anchors on inner icon buttons that distort ratio.

4. Rename popup action row:

- `Cancel` and `Save` buttons should split available width 50/50 using `FlexWeight`.
- Keep action row pinned near the bottom by inserting a flexible spacer above it.

## Copy-safe snippets

### Minimal page skeleton

```ui
$C = "../Common.ui";
$Sounds = "../Sounds.ui";

$C.@PageOverlay {}

$C.@Container {
  Anchor: (Width: 980, Height: 640);

  #Title {
    $C.@Title { @Text = "My Page"; }
  }

  #Content {
    LayoutMode: Top;
    Padding: (Left: 16, Right: 16, Top: 12, Bottom: 12);

    Label #Intro {
      Anchor: (Horizontal: 0, Height: 22, Bottom: 6);
      Style: (...$C.@DefaultLabelStyle, FontSize: 13, TextColor: #ffffff, RenderBold: true);
      Text: "Section";
    }
  }
}
```

### Reusable settings row with text field + apply icon

```ui
Group #ExampleValueRow {
  Anchor: (Height: 60, Horizontal: 0, Bottom: 4);
  LayoutMode: Left;
  Background: (Color: #223149);
  Padding: (Left: 12, Right: 10);

  Group {
    FlexWeight: 1;
    Anchor: (Vertical: 0);
    LayoutMode: Middle;

    Label #ExampleValueLabel {
      Anchor: (Horizontal: 0, Height: 22, Vertical: 0);
      Style: (...$C.@DefaultLabelStyle, FontSize: 13, TextColor: #ffffff, VerticalAlignment: Center, RenderBold: true);
      Text: "Example value";
    }
  }

  Group #ExampleValueFieldBorder {
    Anchor: (Width: 122, Height: 32, Right: 8);
    Padding: (Full: 2);

    $C.@TextField #ExampleValueField {
      @Anchor = (Horizontal: 0, Height: 28, Vertical: 0);
    }
  }

  Group #ExampleValueApplyBox {
    Anchor: (Width: 32, Height: 32);
    Background: (TexturePath: "../Common/Buttons/Secondary.png", Border: 12);
    Visible: false;

    Button #ExampleValueApply {
      Anchor: (Width: 18, Height: 18, Horizontal: 0, Vertical: 0);
      Style: (
        Default: (Background: (TexturePath: "../icons/check.png", Color: #9bb4ce)),
        Hovered: (Background: (TexturePath: "../icons/check.png", Color: #ffffff)),
        Pressed: (Background: (TexturePath: "../icons/check.png", Color: #7f9cb9)),
        Disabled: (Background: (TexturePath: "../icons/check.png", Color: #5a6d80))
      );
    }
  }
}
```

## Quick pre-delivery checklist for the agent

- Imports valid and relative
- No unknown control name
- No unknown style constructor
- No missing semicolon
- No unbalanced brace
- IDs unique in file
- Dynamic slots predeclared if runtime updates are expected
- Section visibility defaults are intentional

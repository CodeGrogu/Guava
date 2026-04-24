# "Garage" Design System Guide

This document describes the **Valentine's Garage** industrial design system, based on the **Tactile Technicality** (Neo-Brutalism) aesthetic implemented in April 2026.

## 1. Aesthetic Philosophy

The "Garage" theme is designed to feel like a high-end workshop diagnostic tool. It prioritizes:
- **Mechanical Feedback**: Buttons that visibly "press down" via shadow offsets.
- **High-Contrast Utility**: Clear separation of elements using hard borders and saturated primary accents.
- **Precision Typography**: Extensive use of Monospaced fonts for technical data points.

## 2. Core Components

### `GarageBackground`
A digital engineering grid (dot-grid or line-grid) that provides a consistent technical canvas for all screens.
- **Usage**: Always place as the first element in a `Box` to serve as the base layer.

### `GarageButton` (Neo-Brutal)
A professional button featuring:
- **Hard Shadows**: A solid black 4dp offset shadow that doesn't use blurs.
- **Haptic Simulation**: The button surface translates to (0,0) when pressed, hiding the shadow and simulating a physical switch.
- **Tokens**: Uses `SafetyOrange` for high visibility actions.

### `GarageTextField`
Refined input fields with:
- **Geometric Shapes**: Square corners (extra-small Material 3 shape).
- **Reactive Borders**: Thick 2dp borders that darken when focused.
- **Integrated Labels**: Monospaced labels positioned for maximum readability during technical entry.

### `RoleCard`
High-fidelity selection cards for user designations.
- **Mechanic**: Features a Wrench icon.
- **Manager**: Features a Clipboard icon.
- **States**: Uses `animateColorAsState` to provide fluid feedback when a role is selected.

## 3. Color Palette

| Token | Hex | Usage |
| :--- | :--- | :--- |
| `SafetyOrange` | #FF8C00 | Primary actions, "Go" buttons. |
| `IndustrialBlack` | #1A1A1A | Borders, shadows, high-contrast text. |
| `BlueprintBlue` | #2196F3 | Informational accents, technical data. |
| `GearGray` | #455A64 | Secondary surfaces. |

## 4. Implementation Reference
- **Theme**: [Theme.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/main/java/com/codegrogu/guava/ui/theme/Theme.kt)
- **Components**: [IndustrialComponents.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/main/java/com/codegrogu/guava/ui/components/IndustrialComponents.kt)

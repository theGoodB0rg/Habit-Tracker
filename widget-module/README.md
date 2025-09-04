# Habits Widget Module

## Overview
This module provides a fully functional, accessible, and responsive Android home screen widget for displaying and managing today's habits. Features include:
- Display of today's habits (synced with DB)
- Quick "Mark Done" toggle for each habit
- Refresh button for manual sync
- Modern, touch-friendly, and accessible UI
- Handles all edge cases, loading, and error states

## Key Files
- `HabitsWidgetProvider.kt`: Main widget logic and event handling
- `HabitsWidgetService.kt`: RemoteViewsFactory and DB sync
- `res/layout/widget_habits.xml`: Widget layout
- `res/layout/widget_habit_item.xml`: Habit row layout
- `res/xml/habits_widget_info.xml`: Widget metadata
- `build.gradle.kts`: Dependencies and build config

## Testing
- Unit and UI test stubs in `src/test/`
- Ensure widget updates, toggles, and refreshes work as expected

## Accessibility & Performance
- All touch targets >= 44dp
- Text overflow and responsive sizing handled
- Content descriptions for all interactive elements
- Efficient DB access and race condition protection

## Integration
- Requires `core-architecture` module for DB access
- Add to app manifest and build as part of the main app

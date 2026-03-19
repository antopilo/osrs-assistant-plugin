# OSRS Assistant
An in-game assistant for Old School RuneScape. Ask questions, get personalized advice based on your actual stats and gear, and follow interactive checklists that auto-complete as you play. This does not perform any actions for you, the player.

## Features
Ask anything about OSRS directly from a sidebar panel. The assistant sees your current stats, gear, inventory, quests, and location so it gives advice you can actually follow right now.

Quick action suggestions get you started fast: **What should I do next?**, **Gear check**, **Money making**, **Train what?**

### Interactive Checklists
When you ask for a quest guide, training plan, or any multi-step task, the assistant creates a checklist that tracks your progress automatically. Steps complete on their own when you gain XP, pick up items, finish quests, or arrive at locations.

You can run multiple checklists at once, choose which one to track for navigation, and fold ones you're not focused on.

### Navigation
Works with the Shortest Path plugin if you have it installed to display shortest-path to locations mentionned.

### Right-Click "Ask Assistant"
Right-click any NPC, item, object, quest, or skill in-game and select **Ask Assistant** to learn about it instantly.

### Rich Responses
Responses include interactive inline elements:
- **Skill mentions** that show green or red based on whether you meet the level
- **Item names** with icons you can right-click to open the wiki or copy the name
- **Quest names** colored by your completion state
- **Location names** that start navigation with one click

### Player Context
Control what account information the assistant can see.
- Skills, Inventory, Equipment, Bank, Quests, Location

### Navigation
- **Use Shortest Path plugin** -- enable for better pathing if installed

## Building

```bash
./gradlew jar
```

## License
BSD 2-Clause License. See [LICENSE](LICENSE).

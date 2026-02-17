# Ourcraft

A Paper / Spigot plugin for Minecraft **1.21.11** that links all online players into a single shared survival state.

All players still move independently, but they share:
- inventory
- armor
- offhand
- health
- hunger and saturation
- air
- fire and freeze state
- potion effects
- XP
- most survival mechanics

Think of it as: **multiple bodies, one player state**.

---

## Features

### Shared survival state
When sharing is enabled, every online player becomes part of one synchronized entity.

Any change made by one player is applied to all players.

Examples:
- one player eats → everyone gains hunger
- one player takes damage → everyone loses hearts
- one player places blocks → items decrease for all
- one player picks up items → items appear for all
- one player burns → everyone burns
- one player drowns → everyone drowns
- one player gets potion effects → everyone gets them

Players still:
- move independently
- mine independently
- fight independently
- explore independently

But their **survival stats are unified**.

---

## Commands

### `/share`
Enables shared state.

- Clears inventories
- Clears effects
- Resets survival stats
- Starts synchronization

### `/unshare`
Disables shared state.

- Clears inventories again
- Clears effects again
- Players return to independent state

---

## Permissions

`shareeverything.admin`  
Default: op

---

## Installation

1. Build the plugin
2. Place the jar inside:

3. Start or restart the server

Supported server:
- Paper 1.21.11 (and compatible builds)

---

## How it works (technical overview)

The plugin continuously compares all players and maintains a **single shared snapshot** of the survival state.

Whenever a player changes something:
- the snapshot updates
- the snapshot is applied to everyone

To avoid ghost items and duplication:
- only actual changes propagate
- constant overwriting is avoided

No NMS is used — Bukkit/Paper API only.

---

## What is shared

### Inventory
- main inventory
- hotbar
- armor
- offhand

### Survival stats
- health
- absorption hearts
- hunger
- saturation
- exhaustion
- air

### Damage state
- fire ticks
- freeze ticks
- fall damage
- invulnerability ticks

### Effects
- all potion effects
- duration and amplifier

### Experience
- XP level
- XP bar
- total experience

---

## What is NOT perfectly shared

Minecraft does not expose some internal values via API:

- exact hurt animation timing
- last damage cause metadata
- some internal combat flags

Gameplay still behaves correctly.

---

## Recommended usage

This plugin is designed for:
- cooperative challenge runs
- shared-life hardcore
- chaos multiplayer challenges
- content creation

---

## License

You may modify and use freely for personal servers or content creation.

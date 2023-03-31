# Recipe Plan Fix

This plugin bypasses the ned to program recipe plans in a project table. Allowing them to be used on servers where the project table is banned.

# Usage
1. In game, right click a recipe plan to open the programming menu.
2. Place a recipe plan in the input slot, on the left.
3. Arrange items in the crafting grid in the middle.
4. Optionally, specify the output of the crafting recipe on the right.
5. Click any of the green glass panes to program the recipe plan in the input slot.

# Known Bugs
- When a recipe plan is closed the items returned to the player may be 'ghost items' and remain invisible until the player's inventory is refreshed. Rest assured that no items are ever lost. Unfortunately, this bug can not be fixed using Sponge 7.2.0

# Missing Features
- Programming a recipe does not check the validity of a recipe. This is either impossible, or prohibitively difficult, in Sponge 7.2.0. Invalid recipes will simply not function in the auto crafting table; no exploits or bugs are known to emerge from this.
- The output of a crafting recipe must be set manually and cannot be looked up, although setting the output serves only a visual purpose.

# Installation
Copy RecipePlanFixPlugin.java (found in src/target) to your servers plugins folder, usually .../mods or .../mods/plugins.

# Building
This project requires Java 8. Do not use any other version. Build using maven:

`mvn clean install` 

A jar file is already provided with the source code.
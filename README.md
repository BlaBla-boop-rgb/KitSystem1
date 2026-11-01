# KitSystem (Paper 1.21.x, Java 21)

## Bauen
    mvn clean package
Die Datei `target/KitSystem.jar` in den `plugins`-Ordner legen.

## Ablauf
1. Inventar so ausrüsten, wie das Kit sein soll → `/kit create Sword`
   - Icon wird automatisch aus dem Namen gewählt (Liste in `config.yml` -> `icon-keywords`)
   - Der Amboss "KitEditor" wird gegeben und der Layout-Editor öffnet sich sofort
2. `/kit finish Sword` → du bekommst den **Kit-Ständer** (Armorstand-Item)
3. Ständer auf einen Block rechtsklicken = platzieren
   - Rechtsklick auf den Ständer: Kit nehmen (mit dem eigenen Layout des Spielers)
   - Shift + Rechtsklick auf den Ständer: aufheben und woanders neu platzieren
4. Kit später ändern: `/kit edit Sword` → Inventar ändern → `/kit save Sword`
   (alle Ständer aktualisieren sich automatisch)

## Layout-Editor (Amboss)
Rechtsklick mit dem Amboss → Kit-Auswahl mit einem Icon pro Kit → Kit anklicken.
Bedienung: **Item antippen, dann Zielslot antippen** (tauscht). Kein Ziehen nötig,
deshalb funktioniert es auch am Handy (Bedrock/Geyser, Touch). Jeder Spieler hat sein eigenes Layout.

## Befehle
/kit, /kit list, /kit load <kit>
/kit create|finish|edit|save|delete <kit>
/kit icon <kit> [material], /kit cooldown <kit> <sek>, /kit stand <kit>, /kit give <kit> [spieler]

Rechte: kitsystem.use (Standard: alle), kitsystem.admin (Standard: OP)

# Ktab — Skins / Custom Heads

## IDs intégrés

```yaml
skin: "none"
skin: "viewer"
skin: "player:Krunsh_"
```

- `none` : tête Minecraft par défaut de la fake entry.
- `viewer` : copie la skin du joueur qui regarde son TAB.
- `player:<pseudo>` : copie la skin d'un joueur actuellement en ligne.

## Profils configurés

```yaml
skins:

  volkaria:
    enabled: true
    texture_hash: "HASH_TEXTURES_MINECRAFT_NET"
    cache_key: "volkaria"

  server:
    enabled: true
    texture_url: "http://textures.minecraft.net/texture/HASH"

  signed:
    enabled: true
    value: "BASE64_MOJANG"
    signature: "SIGNATURE_MOJANG"
```

Les formats `texture_hash`, `texture_url`, `value`, et `value + signature`
sont supportés.

## Affectation

Skin sur toute une colonne :

```yaml
virtual_layout:
  columns:
    jobs:
      skin: "volkaria"
      title: "&e&lMÉTIERS"
```

Skin uniquement sur le titre :

```yaml
title_skin: "volkaria"
```

Skin uniquement sur une ligne :

```yaml
lines:
  - text: "&7Joueur: &f%player_name%"
    skin: "viewer"
```

L'ancienne syntaxe reste compatible :

```yaml
lines:
  - "&7Mineur: &fNv.%kjob_level_mineur%"
```

## Commandes

```text
/ktab skin list
/ktab skin info <skinId> [joueur]
/ktab skin test <skinId> [joueur]
/ktab skin clear [joueur]
```

`skin test` remplace temporairement la tête de la première cellule du viewer
pendant 10 secondes. Aucune modification du YAML n'est nécessaire.

Exemples :

```text
/ktab skin test viewer
/ktab skin test volkaria Krunsh_
/ktab skin info player:Krunsh_ Krunsh_
```

## Cache

Ktab met en cache les skins configurées. La clé finale inclut automatiquement
le hash du payload de texture : modifier le hash, l'URL ou la Base64 force donc
un `REMOVE_PLAYER + ADD_PLAYER` même si `cache_key` n'a pas été changé.

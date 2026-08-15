# Ktab

Ktab est le moteur de TAB indépendant de **Volkaria**, conçu pour Minecraft
**1.8.8 / PandaSpigot / KhopeSpigot** avec Java 8.

Le plugin remplace le TAB vanilla par une grille virtuelle configurable,
performante et indépendante des plugins métier. Ktab ne dépend pas directement
de KjobsUltimate : les données externes passent par **PlaceholderAPI**.

## Fonctionnalités

- Header / footer personnalisables.
- Layout virtuel jusqu'à 4 colonnes × 20 lignes.
- Positionnement automatique ou exact avec `row: 1..20`.
- Diff de packets :
  - `ADD_PLAYER` uniquement pour une nouvelle entrée ;
  - `UPDATE_DISPLAY_NAME` si seul le texte change ;
  - `REMOVE_PLAYER + ADD_PLAYER` si la skin change.
- Cache par viewer.
- Masquage des vrais joueurs dans le TAB sans les rendre invisibles en jeu.
- Hook optionnel ServerNPC pour retirer les NPC du PlayerInfo/TAB.
- PlaceholderAPI pour KjobsUltimate, Vault, Kfaction et autres expansions.
- Custom heads / GameProfile :
  - `none`
  - `viewer`
  - `player:<pseudo>`
  - texture hash
  - texture URL
  - Base64 Mojang
  - Base64 + signature
- Conditions dynamiques par colonne ou cellule.
- Validation de configuration.
- Outils debug, dump et preview.

## Architecture

```text
Ktab
├── command
│   └── KtabCommand
├── condition
│   ├── ConditionEvaluator
│   ├── TabCondition
│   └── TabConditionGroup
├── config
│   └── KtabConfig
├── integration
│   └── ServerNpcHook
├── layout
│   ├── TabColumn
│   ├── TabCell
│   ├── VirtualLayoutRenderer
│   └── PackedTabSizing
├── packet
│   ├── TabPacketSender
│   ├── VirtualTabPacketSender
│   └── TabVisibilityPacketSender
├── render
│   └── PlaceholderRenderer
├── service
│   ├── TabService
│   └── VirtualTabService
├── skin
│   └── TabSkinResolver
└── visibility
    └── TabVisibilityController
```

Le principe est volontairement séparé :

```text
config
  ↓
conditions
  ↓
layout
  ↓
PlaceholderAPI
  ↓
snapshot rendu
  ↓
diff
  ↓
packets 1.8.8
```

## Dépendances

### Obligatoire

- PlaceholderAPI

### Optionnelle

- ServerNPC

ServerNPC est utilisé uniquement pour identifier les UUID de ses NPC et les
retirer du TAB. Les NPC restent visibles normalement dans le monde.

## Compilation

Projet Maven Java 8 :

```powershell
mvn clean package
```

Le jar est produit dans :

```text
target/Ktab-1.0.0-SNAPSHOT.jar
```

Le projet utilise actuellement le même mode de dépendance PlaceholderAPI que
les autres plugins Volkaria. Si le `pom.xml` utilise un `systemPath`, vérifie
que le jar attendu est présent dans `libs/`.

## Configuration rapide

```yaml
enabled: true
placeholderapi: true

header:
  - "&6&lVOLKARIA"

footer:
  - "&7Joueurs en ligne: &f%server_online%"

visibility:
  hide_real_players: true
  hide_servernpc: true

virtual_layout:
  enabled: true
  columns_count: 3
  rows: 15
  max_entries: 45

  columns:

    infos:
      title: "&6&lVOLKARIA"
      title_row: 1

      lines:
        - row: 2
          text: "&7Joueur: &f%player_name%"
          skin: "viewer"

        - row: 3
          text: "&7Métier: &e%kjob_display_job_name%"
```

## Lignes fixes

```yaml
- row: 6
  text: "&7Slots: &f%kjob_slots_used%&8/&f%kjob_slots_unlocked%"
```

Une row fixe reste stable même si une cellule conditionnelle disparaît.

Plus de détails : [`docs/LAYOUT.md`](docs/LAYOUT.md).

## Custom heads

Exemple avec la skin du viewer :

```yaml
- row: 2
  text: "&7Joueur: &f%player_name%"
  skin: "viewer"
```

Profil custom :

```yaml
skins:

  volkaria:
    enabled: true
    texture_hash: "HASH_TEXTURES_MINECRAFT_NET"
```

Puis :

```yaml
title_skin: "volkaria"
```

Plus de détails : [`docs/SKINS.md`](docs/SKINS.md).

## Conditions dynamiques

Exemple avec un placeholder Kjobs :

```yaml
- row: 8
  text: "&aQuête disponible !"

  when:
    mode: all
    conditions:
      - type: not_equals
        input: "%kjob_claimable_quests%"
        value: "0"
```

Exemple permission :

```yaml
when:
  type: permission
  value: "volkaria.vip"
```

Types disponibles :

```text
permission
not_permission
equals
not_equals
contains
not_contains
starts_with
ends_with
empty
not_empty
online_min
online_max
```

Plus de détails : [`docs/CONDITIONS.md`](docs/CONDITIONS.md).

## Commandes

```text
/ktab status
/ktab reload
/ktab preview [joueur]
/ktab debug [joueur]
/ktab refresh [joueur|all]
/ktab clear [joueur|all]

/ktab validate
/ktab dump [joueur] [page]

/ktab skin list
/ktab skin info <skinId> [joueur]
/ktab skin test <skinId> [joueur]
/ktab skin clear [joueur]
```

### `/ktab validate`

Contrôle notamment :

- collisions de rows ;
- row hors limites ;
- skins inconnues ;
- nombre de colonnes ;
- `max_entries` ;
- conditions invalides.

### `/ktab dump`

Affiche le rendu réel d'un viewer :

```text
#00 c1/r1 infos skin=none
   VOLKARIA

#01 c1/r2 infos skin=viewer
   Joueur: Krunsh_
```

V8 affiche aussi le résultat des conditions runtime.

## Placeholders natifs Ktab

Ktab résout directement :

```text
%player_name%
%player_ping%
%server_online%
%server_max_players%
```

Tous les autres placeholders sont délégués à PlaceholderAPI.

## KjobsUltimate

Ktab n'importe aucune classe Java de KjobsUltimate.

Le contrat est uniquement :

```text
Ktab
  ↓
PlaceholderAPI
  ↓
%kjob_*%
  ↓
KjobsUltimate
```

Cela permet de redémarrer, modifier ou faire évoluer les deux plugins
indépendamment.

## Documentation

```text
docs/
├── CONDITIONS.md
├── LAYOUT.md
├── SKINS.md
├── config-v5-heads-example.yml
├── config-v7-layout-example.yml
└── config-v8-conditions-example.yml
```

## Compatibilité

Cible actuelle :

```text
Minecraft: 1.8.8
NMS:       v1_8_R3
Java:      8
Server:    PandaSpigot / KhopeSpigot
```

La couche NMS est isolée dans les classes `packet/` afin de ne pas contaminer
le renderer, la configuration ou les intégrations.

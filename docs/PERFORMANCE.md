# Ktab — Performance V9.1

## Objectif

V9.1 pose la fondation pour supporter plusieurs centaines de joueurs sans
rerendre toute la tablist sur un seul tick.

L'objectif principal est de limiter les **pics de travail**, tout en conservant
un YAML très configurable.

## Scheduler central

Avant V9, `TabService` et `VirtualTabService` possédaient leurs propres boucles
globales.

V9 utilise un scheduler unique :

```text
KtabSchedulerService
├── RefreshWheel
├── DirtyQueue
├── PerformanceMetrics
├── TabService
└── VirtualTabService
```

### RefreshWheel

La roue répartit les viewers sur `refresh_window_ticks`.

Exemple :

```yaml
performance:
  scheduler:
    refresh_window_ticks: 40
    max_viewers_per_tick: 25
```

Avec 700 joueurs :

```text
ceil(700 / 40) = 18 viewers réguliers / tick
```

Le serveur ne traite donc plus les 700 viewers le même tick.

`max_viewers_per_tick` agit comme plafond anti-spike. Si le plafond est trop
faible pour respecter la fenêtre demandée, le cycle complet prend simplement
plus longtemps.

## DirtyQueue

Les changements urgents utilisent une file FIFO dédupliquée par UUID.

```yaml
performance:
  scheduler:
    dirty_queue:
      enabled: true
      max_per_tick: 30
```

Exemples de raisons :

```text
JOIN
GLOBAL
MANUAL
SKIN
CONFIG
FALLBACK
```

Si un viewer reçoit plusieurs invalidations avant son passage, elles sont
fusionnées en un seul travail.

## Join / Quit

V9 supprime les anciens :

```text
join -> applyAll() + refreshAll()
quit -> applyAll() + refreshAll()
```

### Join

Avec `event_driven: true` :

```text
nouveau joueur
├── retire les joueurs/NPC déjà présents de SON tab
└── chaque ancien viewer retire uniquement CE nouveau joueur
```

Cela évite de reconstruire et renvoyer toute la liste réelle pour chaque viewer.

### Quit

Aucun sweep de visibilité n'est nécessaire :

```text
quit
├── unregister du RefreshWheel
├── purge DirtyQueue
├── purge caches
└── vanilla retire déjà le profil du joueur
```

## Fallback de visibilité

Un sweep complet de sécurité reste possible :

```yaml
performance:
  visibility:
    event_driven: true
    fallback_scan_ticks: 0
```

`0` désactive ce sweep, ce qui est recommandé à forte population lorsque le
mode événementiel fonctionne correctement.

Si tu veux un filet de sécurité, utilise plutôt une valeur élevée, par exemple
`6000 ticks` (5 minutes).

Le batch des profils est construit une seule fois puis réutilisé pour chaque
viewer.

Pour ServerNPC :

```yaml
servernpc_scan_ticks: 100
```

Le scan ne traite que les UUID des NPC.

Mettre `0` désactive un fallback.

## Refresh global lors d'un join / quit

```yaml
refresh_global_on_join_quit: true
```

C'est utile pour rafraîchir rapidement des valeurs comme `%server_online%`.

Cela ne provoque pas de `refreshAll()` immédiat : tous les viewers sont placés
dans la DirtyQueue puis consommés avec le budget `max_per_tick`.

## Mode compatibilité

```yaml
performance:
  enabled: false
```

Ktab repasse sur un comportement global à intervalle fixe, piloté par :

```yaml
update_interval_ticks: 40

virtual_layout:
  update_interval_ticks: 40
```

Ce mode est surtout utile pour comparer ou diagnostiquer.

## /ktab perf

```text
/ktab perf
```

Affiche notamment :

```text
Online
wheel size
refresh window
viewers réguliers/tick
cycle estimé
DirtyQueue actuelle / pic
temps scheduler last / avg / max
temps viewer avg / max
refresh totals
Virtual packet operations
Header/Footer packets
visibility targeted/full/NPC
```

Reset :

```text
/ktab perf reset
```

## Configuration recommandée pour 700 joueurs

Base de départ :

```yaml
performance:
  enabled: true

  scheduler:
    refresh_window_ticks: 40
    max_viewers_per_tick: 25
    refresh_global_on_join_quit: true

    dirty_queue:
      enabled: true
      max_per_tick: 30

  visibility:
    event_driven: true
    fallback_scan_ticks: 0
    servernpc_scan_ticks: 100
```

Cette configuration n'est pas une garantie absolue de capacité à 700 joueurs :
le coût réel dépend surtout des expansions PlaceholderAPI et du matériel.

V9.2 ciblera précisément ce deuxième poste de coût avec :

```text
CompiledTemplate
GlobalSnapshot
PlayerSnapshot
déduplication PAPI
TTL configurable
```

## Threading

Ktab ne déporte pas aveuglément PlaceholderAPI ou Bukkit en async.

Le modèle V9 privilégie :

```text
main thread
+ budgets stricts
+ cache
+ diff
+ scheduler réparti
```

Cela évite les problèmes de thread-safety des expansions PlaceholderAPI et APIs
Bukkit legacy.


## V9.2 — Compiled Placeholders & Snapshot Cache

V9.2 réduit le coût PlaceholderAPI sans rendre PAPI ou Bukkit asynchrones.

Pipeline :

```text
texte YAML
  ↓ une fois
TemplateCompiler
  ↓
CompiledTemplate
  ↓ par viewer
GlobalSnapshot + PlayerSnapshot
  ↓
PlaceholderAPI uniquement sur cache miss
  ↓
texte final
```

Configuration :

```yaml
performance:
  placeholders:
    compiled_templates: true
    deduplicate: true
    max_compiled_templates: 512

    cache:
      enabled: true
      default_ttl_ticks: 40
      max_entries_per_player: 64

      rules:
        - match: "%player_ping%"
          ttl_ticks: 20

        - match: "%kjob_level_*%"
          ttl_ticks: 60

        - match: "%kjob_*%"
          ttl_ticks: 40
```

Les règles sont évaluées de haut en bas. La première correspondance gagne.

`ttl_ticks: 0` désactive le cache inter-renders pour le placeholder concerné.

### Compatibilité

Si une expansion PlaceholderAPI particulière ne supporte pas correctement la
résolution token-par-token, utilise :

```yaml
performance:
  placeholders:
    deduplicate: false
```

Ktab repasse alors sur le mode PAPI historique pour les chaînes complètes.

### Diagnostic

```text
/ktab perf
```

affiche désormais :

- templates compilés ;
- snapshots joueurs ;
- nombre de valeurs en cache ;
- demandes placeholders ;
- résolutions réelles ;
- cache hits ;
- hit rate ;
- appels PAPI legacy.

Pour vider le cache sans redémarrer :

```text
/ktab perf clearcache
```

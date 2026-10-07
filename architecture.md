# Architecture du projet ISkyblock

Date: 2026-05-04

## But du document

Ce fichier sert d'index et de guide d'architecture pour le plugin ISkyblock. Il doit permettre à un développeur ou à une IA d'obtenir en un coup d'œil une vue précise : organisation des packages, responsabilités principales, modèle de données, flux d'exécution au démarrage et points d'extension (addons, UI, protection, interactions). Utilise ce fichier comme référence pour construire des prompts, générer des tests, ou faire des refactorings ciblés.

## Résumé rapide

- Plugin Hytale fournissant un système Skyblock avec îles, templates, warps, homes, levels, protections et addons.
- Architecture modulaire : séparation claire entre `service` métier (`island`), persistance (`repository`), addons (`addons/*`), protections (`protection/*`) et UI (`src/main/resources` + `ui` package).
- Build: Gradle wrapper présent (`gradlew`, `build.gradle`).

## Commandes utiles

- Construire le plugin (Windows) :

```powershell
.\build.bat
```

## Arborescence importante

- Fichiers de build : [build.gradle](build.gradle), [gradlew](gradlew), [gradlew.bat](gradlew.bat)
- Entrée plugin : [src/main/java/com/sloyni/main/ISkyblockPlugin.java](src/main/java/com/sloyni/main/ISkyblockPlugin.java#L1-L400)
- Code java principal : [src/main/java](src/main/java)
- Ressources UI : [src/main/resources/Common/UI/Custom](src/main/resources/Common/UI/Custom)

## Principaux packages et responsabilités

- **com.sloyni.main**
  - `ISkyblockPlugin` : point d'initialisation du plugin, enregistre systèmes/propriétés/commandes et conserve les singletons applicatifs. Voir [ISkyblockPlugin.java](src/main/java/com/sloyni/main/ISkyblockPlugin.java#L1-L400).
  - `ConfigManager`, `PluginConfig`, `MessagesConfigManager` : gestion de la configuration et messages.

- **com.sloyni.main.island**
  - `IslandService` : logique métier pour créer/teleporter/réinitialiser/mettre à jour une île. Fait appel à `IslandRepository` et `IslandTemplateManager`. Voir [IslandService.java](src/main/java/com/sloyni/main/island/IslandService.java#L1-L400).
  - `IslandRepository` : stockage JSON (Gson) des îles, accès thread-safe via `ConcurrentHashMap`. Voir [IslandRepository.java](src/main/java/com/sloyni/main/island/IslandRepository.java#L1-L400).
  - `IslandData` : modèle persisté pour une île (propriétaire, membres, position grille, nom du monde, paramètres). Voir [src/main/java/com/sloyni/main/island/IslandData.java](src/main/java/com/sloyni/main/island/IslandData.java#L1-L400).
  - `GridAllocator` / `PrefabIslandGenerator` / `IslandTemplateManager` : allocation de grille et génération d'îles à partir de templates.
  - Repositories additionnels : `PlayerResetRepository`, `PlayerIslandSizeRepository`, `PlayerHomeLimitRepository` (support fonctionnel lié aux joueurs).

- **com.sloyni.main.addons**
  - `AddonManager`, `AddonStorage` : système d'extension, charge les addons (levels, homes, warps, islandwalls, mobsmanager, oregenerator). Chaque addon suit un pattern : ConfigManager, Repository, Service, CommandHandler.
  - Exemples : [addons/warps/WarpsService.java](src/main/java/com/sloyni/main/addons/warps/WarpsService.java#L1-L200), [addons/homes/HomesService.java](src/main/java/com/sloyni/main/addons/homes/HomesService.java#L1-L200)

- **com.sloyni.main.protection**
  - Systèmes de protection enregistrés auprès de l'EntityStore (ECS-like). Exemples : `IslandBreakProtection`, `IslandPlaceProtection`, `VisitorDamageImmunityProtection`, `IslandMobProtectionHandler`.

- **com.sloyni.main.interactions**
  - Code pour enregistrer codecs d'interaction (PlaceFluid, RefillContainer, HarvestCrop, BreakBlock, DestroyBlock). Ces classes exposent un codec pour la persistance/serialization d'interactions.

- **com.sloyni.main.menu`et`com.sloyni.main.ui`**
  - UI runtime pour le HUD et les pages (fichiers .ui en `resources`). Managers pour HUD et keybinds : `IslandMenuHudManager`, `IslandMenuKeybindManager`.

- **com.sloyni.main.lang**
  - Internationalisation, `MessageManager`, `LanguageService`, _KnownPlayerRepository_, _PlayerPreferenceRepository_.

- **com.sloyni.main.asset**
  - Asset/Template managers : `TemplateImageManager`, `ByteArrayCommonAsset`.

## Flux d'initialisation (démarrage du plugin)

1. `ISkyblockPlugin.setup()` construit les managers/configs, initialise `TemplateImageManager`, crée `IslandService` et `AddonManager`.
2. Enregistrement des systèmes de protection dans l'EntityStore via `getEntityStoreRegistry().registerSystem(...)`.
3. Enregistrement des codecs d'interaction via `getCodecRegistry(...).register(...)`.
4. Enregistrement des handlers d'événements globaux (PlayerReadyEvent, PlayerConnectEvent, PlayerDisconnectEvent).
5. Démarrage des managers UI (HUD, keybinds) et synchronisation des commandes.

## Modèle de données (points saillants)

- `IslandData` (persisté dans `islands.json` par `IslandRepository`) contient typiquement :
  - `ownerUuid` (UUID du créateur), `leaderUuid` (si différent), `members` (liste/permissions),
  - coordonnées de la grille (gridX, gridZ) et `worldName`,
  - paramètres d'île (locked, visitor flags, spawn, default member permissions),
  - références à templates et états d'addons (wall state, warps, homes).
- Le repository sérialize en JSON via Gson et utilise `LockFolderStorage` pour éviter conflits d'accès disque.

## Concurrence et IO

- `IslandRepository` utilise `ConcurrentHashMap` pour accès en mémoire, mais la méthode `save()` effectue une écriture synchrone vers disque à chaque `put()/remove()`.
- Risques : latence IO sur `put()`, exceptions silencieuses (catch vide), absence d'écriture asynchrone/batch.

## Points techniques et patterns observés

- Pattern Factory / Manager : `AddonManager`, `IslandTemplateManager`, `TemplateImageManager` centralisent des responsabilités liées.
- Service Locator / Singleton : `ISkyblockPlugin.getInstance()` est utilisé comme point d'accès global — facile mais couplage fort.
- Event-driven / ECS : utilisation de `EntityStoreRegistry` et `EventRegistry` pour enregistrer systèmes et handlers.
- Persistance simple et transparente (JSON) — bonne pour lisibilité et debug, moins pour scalabilité.

## Forces du projet

- Bonne séparation par packages ; structure modulaire pour addons.
- Utilisation moderne de Java (records dans `IslandService` pour résultats).
- UI et assets clairement séparés des services métier.

## Faiblesses et risques

- `ISkyblockPlugin` fait trop d'initialisation (violation SRP) ; difficile à tester.
- Couplage via `getInstance()` empêche l'injection et rend les tests unitaires plus durs.
- `IslandRepository` écrit synchroniquement et ignore certaines exceptions — risque de corruption/perte d'erreurs.
- Peu (ou pas) d'abstraction (interfaces) pour substituer repos/services en tests.

## Recommandations actionnables (ordre de priorité)

1. Extraire une couche d'injection légère / ServiceContainer et réduire `ISkyblockPlugin` à l'orchestration minimale.
2. Remplacer usages de `ISkyblockPlugin.getInstance()` par injection explicite dans les constructeurs pour `IslandService`, addons, etc.
3. Transformer `IslandRepository.save()` en écriture asynchrone / batch avec flush périodique et gestion d'erreurs explicite.
4. Remplacer prints `System.out/err` par un logger centralisé et structuré.
5. Ajouter interfaces pour `IslandRepository` et `IslandService` pour faciliter tests (in-memory implementations).
6. Ajouter tests unitaires pour `IslandService`, `GridAllocator`, `PrefabIslandGenerator` et les repos.

## Checklist pour les futures IA-prompts

- Pour analyser une fonctionnalité métier : regarder d'abord `IslandService`, `IslandRepository`, `IslandData`.
- Pour comprendre l'initialisation : ouvrir [ISkyblockPlugin.java](src/main/java/com/sloyni/main/ISkyblockPlugin.java#L1-L400).
- Pour étudier un addon : aller dans `src/main/java/com/sloyni/main/addons/<addonName>` et lire `*Service`, `*Repository`, `*Config`.
- UI : consulter `src/main/resources/Common/UI/Custom` et les pages Java correspondantes dans `com.sloyni.main.ui`.

## Fichiers et points d'entrée recommandés pour démarrer

- [src/main/java/com/sloyni/main/ISkyblockPlugin.java](src/main/java/com/sloyni/main/ISkyblockPlugin.java#L1-L400) — démarrage et enregistrements.
- [src/main/java/com/sloyni/main/island/IslandService.java](src/main/java/com/sloyni/main/island/IslandService.java#L1-L400) — logique métier principale.
- [src/main/java/com/sloyni/main/island/IslandRepository.java](src/main/java/com/sloyni/main/island/IslandRepository.java#L1-L400) — doctrine de persistance.
- [src/main/resources/Common/UI/Custom/Pages/IslandMenu.ui](src/main/resources/Common/UI/Custom/Pages/IslandMenu.ui) — exemple d'UI.

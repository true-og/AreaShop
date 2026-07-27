# AreaShop-OG

Fork of [AreaShop](https://github.com/NLthijs48/AreaShop) by NLThijs48 (continuing
[md5sha256's](https://github.com/md5sha256/AreaShop) `dev/bleeding` line), maintained by the
[TrueOG Network](https://true-og.net) for use on `true-og.net`.

AreaShop rents and sells WorldGuard regions to players. Players interact with signs to rent, buy, extend and give up
shops, and admins manage everything with `/as` commands. This fork keeps all of that and replaces the money, permission
and shop-flow layers with the ones TrueOG runs, after migrating the network off AdvancedRegionMarket (ARM).

The plugin display name, jar and data folder are now `AreaShop-OG`, but the commands (`/as`, `/areashop`) and the
permission namespace (`areashop.*`) are unchanged.

## Differences from upstream

### Economy and permissions

- **Diamonds instead of Vault money** — the Vault dependency is gone. Every rent, extend, buy, resell and refund goes
  through [DiamondBank-OG](https://github.com/true-og/DiamondBank-OG), so shop payments come out of (and go back into)
  a player's diamond bank and show up in their bank history. Prices are shown as `12.5 Diamonds`, where the fractional
  digit is diamond shards (9 shards = 1 diamond).
- **LuckPerms instead of Vault permissions** — rank and limit lookups talk to LuckPerms 5.5 directly.
- **Jubilee mode** (`jubilee: true` in `config.yml`) — while it is on, the first rent of a shop costs Diamonds and
  immediately grants the maximum rent time; extending, buying and reselling are free, and unrenting or selling pays
  nothing back. Set it to `false` for the normal pay-per-period economy.
- **Rank-based shop limits shipped in the config** — players without a rank cannot own a shop; the `og`, `og-pro` and
  `og-master` ranks raise the totals and cap how many shops of one region group (for example `shop` or `union-shop`) a
  player may hold. Ranks stack, the highest number wins, and the rules are documented inline in `config.yml`.

### Playing with shops

- **Shift-click sign menus** — shift-clicking a sign opens an in-game confirmation menu instead of firing the action
  straight away. Renting, buying, selling back and reselling all confirm through a GUI, and the shop's renter gets a
  rent menu with extend and sell-back options. New commands behind those menus: `/as confirmrent`, `/as confirmbuy`,
  `/as confirmsell`, `/as rentoptions` and `/as payrent` (pay the rent of someone else's shop).
- **Shop announcements** — renting, buying, reselling and claiming a shop is broadcast to everyone in the worlds listed
  under `announceWorlds` in `config.yml`. An empty list turns broadcasts off.
- **New-owner celebration** — a player who takes over a shop is teleported into it facing away from the sign, with
  particles and a sound.
- **ARM-style signs** — sign layouts, colors and click actions were carried over from AdvancedRegionMarket, including
  the compact `%durationshort%` placeholder (`30d`, `1d 12h`, `45min`). Sign text was also darkened and given a dye
  color fallback so it stays readable on light sign materials and on older clients.
- **Reworded messages** — limit, sign and rent messages explain what to do next ("sell or unrent one before taking
  another") instead of only stating the rule, and prices are colored.

### Migration from AdvancedRegionMarket

- ARM region files are converted to the AreaShop-OG format on startup, ARM region groups are merged into `groups.yml`,
  and incomplete ARM migrations in `config.yml` and `default.yml` are repaired automatically.
- The jar ships TrueOG's 89 migrated shop definitions, which are used to fill in the parts of an ARM region file that
  ARM's own export left out.

### Platform and build

- Targets Paper/Spigot 1.19.4+ on Java 17. Versioning restarts at `1.0` for this fork.
- **FastAsyncWorldEdit is not supported** — its adapter has been dropped, so region saving and restoring always goes
  through plain WorldEdit.
- Update checks are off by default (`checkForUpdates: false`) and point at this repository when enabled.
- Gradle 8.14.3 build with Spotless and TrueOG's Eclipse formatter, DiamondBank-OG pulled in as a git submodule.

## Upgrading an existing AreaShop install

The plugin name changed, so the plugin now reads and writes `plugins/AreaShop-OG/` instead of `plugins/AreaShop/`.
Rename (or copy) the old folder before the first start to keep your regions, groups and configs. The chat prefixes in
an existing `config.yml` are rebranded automatically on startup, as long as they were left at their default values.

## Required dependencies

* Java 17 or higher (latest recommended)
* Bukkit/Spigot 1.19.4 or newer (hybrids such as Mohist are not supported)
* [WorldGuard](https://dev.bukkit.org/bukkit-plugins/worldguard/): 7.0.7 or newer
* [WorldEdit](https://dev.bukkit.org/bukkit-plugins/worldedit/): 7.2.12 or newer
* [DiamondBank-OG](https://github.com/true-og/DiamondBank-OG): diamond-based economy provider
* [LuckPerms](https://luckperms.net/): 5.5 or newer

## Building

```
git clone --recurse-submodules https://github.com/true-og/AreaShop-OG.git
cd AreaShop-OG
./gradlew build
```

The usable jar is `AreaShop-OG/build/libs/AreaShop-OG-<version>.jar` (the `-original` and `-sources` jars are not the
plugin). More detail in [documentation/compiling.md](documentation/compiling.md).

## Documentation

The upstream wiki still describes the region setup, config system and command reference, minus the changes listed
above:

* [Commands and permissions](https://github.com/NLthijs48/AreaShop/wiki/Commands-and-Permissions)
* [Basic regions setup](https://github.com/NLthijs48/AreaShop/wiki/Basic-regions-setup) &
  [advanced regions setup](https://github.com/NLthijs48/AreaShop/wiki/Advanced-regions-setup)
* [Configuration files](https://github.com/NLthijs48/AreaShop/wiki/The-config-system)
* [Limit groups](https://github.com/NLthijs48/AreaShop/wiki/Limitgroups-information-and-examples)
* [Save/restore region blocks](https://github.com/NLthijs48/AreaShop/wiki/Region-blocks-save-restore)
* [Language support](https://github.com/NLthijs48/AreaShop/wiki/Language-support)
* [Frequently asked questions](https://github.com/NLthijs48/AreaShop/wiki/Frequently-Asked-Questions) &
  [common errors](https://github.com/NLthijs48/AreaShop/wiki/Common-errors)

## Upstream links

* [Original repository](https://github.com/NLthijs48/AreaShop) by NLThijs48
* [Fork this branch continues](https://github.com/md5sha256/AreaShop) by md5sha256
* [Spigot resource page](https://www.spigotmc.org/resources/areashop.2991/)

## License

GPL-3.0, same as upstream. See [LICENSE](LICENSE).

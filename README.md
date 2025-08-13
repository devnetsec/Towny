**Notice:** The lead [developer](https://github.com/LlmDl) of the upstream project is requesting donations. You can sponsor them [here](https://github.com/sponsors/LlmDl).

## CityState - A Heavily Modified [Towny](https://townyadvanced.github.io) Fork

The goal of this project is to add and remove functionality to the Towny [Spigot](https://www.spigotmc.org) plugin, a Minecraft Java Edition server mod, to improve its performance, simplicity, and features, potentially for MMO city-building gameplay.

### Project Status

To be removed:
- [ ] Nations and War: to adapt the plugin for cooperative, mostly non-pvp gameplay, there should be only one player government (with administrative subdivisions) per server; realtime combat gameplay is unpleasant with very large amounts of online players.
    - [x] Creating/Deleting/Merging Nations: there should be only one - automatically created - nation per server world.
    - [x] Neutral/Peaceful status
    - [x] Embassy and Arena block types
    - [x] PVP block status
    - [x] Allies/Enemies
    - [x] Conquering
    - [x] Ruined town status
    - [ ] War plugins
- [ ] ~~Banking: item-based currencies and trading would be more seamless and engaging, logically complement Plot Deeds (described below), and open the possibility for player-made finance industries.~~ **Economy plugins can be completely disabled in `config.yml`.**
- [ ] Teleportation: this spoils the simulation of transportation and storage systems.
- [ ] Telemetry and update checking

To be added:
- [ ] Voting: simple approval voting using commands.
- [ ] Item-based Trading
- [ ] Custom currency items
- [ ] Plot Deeds: custom items that transfer ownership of land.
- [ ] Dueling: resident-based (not plot-based) PVP permissions.

### Upstream Resources
- [Towny Wiki](https://github.com/TownyAdvanced/Towny/wiki)
- [Changelog](https://github.com/TownyAdvanced/Towny/blob/master/Towny/src/main/resources/ChangeLog.txt)
- **[Installing Towny](https://github.com/TownyAdvanced/Towny/wiki/Installation)**
- **[Frequently Asked Questions](https://github.com/TownyAdvanced/Towny/wiki/Frequently-Asked-Questions)**
- **[How Towny Works](https://github.com/TownyAdvanced/Towny/wiki/How-Towny-Works)**
- [Issue Tracker](https://github.com/TownyAdvanced/Towny/issues)
- [Bug Reports](https://github.com/TownyAdvanced/Towny/issues/new?assignees=&labels=&template=bug_report.md&title=)
- [Feature Requests](https://github.com/TownyAdvanced/Towny/issues/new?assignees=&labels=&template=feature_request.md&title=Suggestion%3A+)
- [General Questions](https://github.com/TownyAdvanced/Towny/discussions/new?category=Q-A)
- [Discord server](https://discord.gg/gnpVs5m)
- [SponsorPlugins](https://github.com/LlmDl/SponsorPlugins#readme)


### Contributing
I am not accepting pull requests for this project at the moment, but please open issues if you encounter any. If you'd like to contribute to the upstream Towny code, see [here](https://github.com/LlmDl/Towny/blob/master/.github/CONTRIBUTING.MD).


### Licensing
Towny is licensed under the [Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported (CC BY-NC-ND 3.0) License ](https://creativecommons.org/licenses/by-nc-nd/3.0/)

The upstream developer requests that their work not be used in derivative works or commercial products. I'm not yet sure how well this fork complies with the license. Clone/fork at your own risk.

### Building
If you would like to build from a specific branch yourself, you can do so with [Apache Maven](https://maven.apache.org/). Open your terminal / command prompt and navigate to the Towny Directory (either extracted, or cloned). Run `mvn clean package` to generate the plugin in the `target` directory within the Towny folder. 


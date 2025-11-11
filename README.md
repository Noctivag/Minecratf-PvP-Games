# NextLevelPvP - Minecraft PvP Plugin

A comprehensive Minecraft PvP plugin featuring multiple game modes and arena management system for Paper 1.21.1+.

## Features

### Game Modes

1. **FFA (Free-For-All)**
   - Every player for themselves
   - Configurable kill limit and time limit
   - Automatic respawning
   - Custom starter kits

2. **1v1 Duel**
   - Honorable 1v1 combat
   - Best-of-X rounds system
   - Separate spawn points for duelers
   - Enhanced duel kits

3. **Team Deathmatch**
   - Red vs Blue team combat
   - Team-colored armor
   - Team spawn points
   - Friendly fire protection
   - Team kill tracking

4. **King of the Hill**
   - Capture and hold the hill
   - Point-based scoring system
   - Dynamic king status
   - Hill capture mechanics

### Arena Management

- **File-based arena storage** with automatic saving
- **Multiple spawn points** per arena
- **Team-specific spawns** for team-based modes
- **Region-based arena boundaries**
- **Lobby and spectator spawn** configuration
- **Per-arena game mode restrictions**
- **Min/max player limits** per arena

### Player Statistics

- Persistent stat tracking (kills, deaths, wins, losses)
- K/D ratio calculation
- Win rate tracking
- Games played counter
- Leaderboard support

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart or reload your server
4. Configure the plugin in `plugins/NextLevelPvP/config.yml`

## Building from Source

Requirements:
- Java 21 or higher
- Maven
- Paper 1.21.1 or higher

```bash
git clone <repository-url>
cd Minecratf-PvP-Games
mvn clean package
```

The compiled JAR will be in the `target` directory.

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/pvp help` | Show help menu | `nextlevelpvp.play` |
| `/pvp join <mode> [arena]` | Join a game | `nextlevelpvp.play` |
| `/pvp leave` | Leave current game | `nextlevelpvp.play` |
| `/pvp stats [player]` | View player statistics | `nextlevelpvp.stats` |
| `/pvp list <modes\|arenas\|games>` | List game information | `nextlevelpvp.play` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/pvp create <mode> <arena>` | Create a new game instance | `nextlevelpvp.admin` |
| `/pvp arena create <name>` | Create a new arena | `nextlevelpvp.create` |
| `/pvp arena setlobby <arena>` | Set arena lobby spawn | `nextlevelpvp.create` |
| `/pvp arena addspawn <arena>` | Add a spawn point | `nextlevelpvp.create` |
| `/pvp arena setregion <arena>` | Set arena boundaries | `nextlevelpvp.create` |
| `/pvp arena delete <arena>` | Delete an arena | `nextlevelpvp.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `nextlevelpvp.admin` | Access to all admin commands | op |
| `nextlevelpvp.play` | Join and play PvP games | true |
| `nextlevelpvp.create` | Create and manage arenas | op |
| `nextlevelpvp.stats` | View player statistics | true |

## Configuration

The plugin configuration file is located at `plugins/NextLevelPvP/config.yml`.

### Game Settings
```yaml
game:
  min-players: 2          # Minimum players to start
  max-players: 16         # Maximum players per game
  countdown-time: 10      # Countdown before game starts
  game-duration: 600      # Game duration in seconds
  respawn-time: 5         # Respawn delay in seconds
```

### Arena Settings
```yaml
arena:
  auto-load: true         # Auto-load arenas on startup
  save-interval: 300      # Auto-save interval in seconds
```

### Game Mode Settings
```yaml
gamemodes:
  ffa:
    enabled: true
    kill-limit: 20        # Kills needed to win
    time-limit: 600       # Time limit in seconds
  duel:
    enabled: true
    best-of: 3            # Best of X rounds
  tdm:
    enabled: true
    team-size: 4          # Players per team
    kill-limit: 50        # Team kills to win
  koth:
    enabled: true
    capture-time: 60      # Seconds to capture hill
    point-value: 1        # Points per second
```

### Player Settings
```yaml
player:
  keep-inventory: false   # Keep inventory on death
  keep-exp: false         # Keep experience on death
  restore-after-game: true
  save-stats: true
```

## Arena Setup Guide

1. **Create an Arena**
   ```
   /pvp arena create MyArena
   ```

2. **Set the Lobby Spawn**
   ```
   /pvp arena setlobby MyArena
   ```
   Stand where you want players to spawn in the lobby.

3. **Add Spawn Points**
   ```
   /pvp arena addspawn MyArena
   ```
   Add multiple spawn points throughout the arena.

4. **Set Arena Boundaries**
   ```
   /pvp arena setregion MyArena
   ```
   This sets the region around your current location.

5. **Test Your Arena**
   ```
   /pvp join ffa MyArena
   ```

## Game Modes Explained

### FFA (Free-For-All)
Players compete individually. First to reach the kill limit or player with most kills when time runs out wins.

### 1v1 Duel
Two players face off in a best-of series. Perfect for honorable combat and tournaments.

### Team Deathmatch
Red vs Blue team combat. Teams work together to reach the kill limit first. Features team-colored armor and friendly fire protection.

### King of the Hill
Capture and hold the hill to earn points. Standing on the hill earns points over time. First to reach the score limit wins.

## Data Storage

The plugin stores data in YAML format:
- `arenas.yml` - Arena configurations and spawn points
- `stats.yml` - Player statistics

All data is automatically saved on server shutdown and periodically during runtime.

## API for Developers

The plugin provides a comprehensive API for other plugins to integrate:

```java
NextLevelPvP plugin = (NextLevelPvP) Bukkit.getPluginManager().getPlugin("NextLevelPvP");

// Get managers
ArenaManager arenaManager = plugin.getArenaManager();
GameManager gameManager = plugin.getGameManager();
PlayerManager playerManager = plugin.getPlayerManager();

// Example: Create a custom game
Arena arena = arenaManager.getArena("MyArena");
GameMode game = gameManager.createGame("ffa", arena);
gameManager.joinGame(player, game);
```

## Support & Contributing

For bugs, feature requests, or contributions, please visit the GitHub repository.

## License

[Add your license information here]

## Credits

Developed by NextLevel Team

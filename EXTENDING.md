# How to add your own minigame

If you want to add your own minigame to the plugin, you have to follow these steps:



## 1. Add your configuration `yml` file

   The configuration file contains attributes about your minigame that get read by the plugin on start. Add your values to a file called `<alias>.yml` in `src/main/resources` where `<alias>` is your plugin name or alias in [snake_case](https://en.wikipedia.org/wiki/Snake_case).
   
   Required values are:
   
   - `origin` - This holds the x, y, and z coordinate for the player spawn. Players get teleported to this location when your minigame starts
   - `world_name` - The name of your world directory. Specify the value from the `config.yml` file -> `default_world.name` to use the default world.
   - `reward` - This value represents the reward factor associated with your game (not implemented yet). Specify any number for the time being.

### Example:

```yml
reward: 1
origin:
  x: 0
  y: 65
  z: 0
world_name: my_plugin_world
# add more key-value pairs here
```



## 2. Add your plugin to the `MinigameType` enum

   You have to register your plugin in the `MinigameType` enum. Required arguments are `displayName` and `alias`.
   
   - `displayName` - The text that gets displayed in minecraft when your minigame starts.
   - `alias` - Your plugin alias. **Must be the name of your plugin node in the `config.yml`**.

### Example:
```java
public enum MinigameType {
    // ...
    MY_MINIGAME(ChatColor.GOLD + "" + ChatColor.BOLD + "My Minigame", "your_plugin_alias");
    // ...
}
```

## 3. Add your minigame class

   - Create your java class containing the code of your minigame in `me.timwastaken.minecraftparty.models.minigames` and make it extend one of the classes from `me.timwastaken.minecraftparty.models.templates`
   - Each minigame class has a field `type` which is the `MinigameType` enum value corresponding to your minigame. This type gets passed on to the super class and is important for loading basic attributes, such as the world that you specified.
   - Ech minigame class has a field `flags` which is of type `List<MinigameFlag>`. This list can contain useful flags to add certain functionalities, the flag `MinigameFlag.NO_PVP` disables pvp for example (see enum `MinigameFlag` for more detail). Pass null for no flags.
   - If you want to implement events, such as `onGameStart` which gets called when your game starts, you can implement the `GameEventListener` interface and add the required methods to your class. To register your class as a listener, simply call `super.addGameEventListeners(this);`.

### Example:

```java
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;

public class MyMinigame extends Minigame implements GameEventListener {

    private static MinigameType type = MinigameType.MY_MINIGAME;

    public MyMinigame() {
        super(type, List.of(MinigameFlag.NO_BLOCK_PLACEMENT, MinigameFlag.NO_BLOCK_BREAKING));
        super.addGameEventListeners(this); // required for your events to work
    }

    @Override
    public void onGameStart() {
        // gets executed when the game launches
    }

    @Override
    public void onGameEnd() {
        // gets executed when the game ends
    }

    @Override
    public void onWorldLoaded() {
        // gets executed as soon as your world is loaded

        // use this to load desired config options that influence the behavior of your plugin
        // example below:
        int value1 = getConfig().getInt("value1"); // must exist in the your_plugin_alias.yml file
        String str1 = getConfig().getString("str1");
        // ...
    }

    @Override
    public void onPlayerLeave(Player p) {
        // gets executed when a player quits the server during your game
    }

}
```

## 4. Register your minigame in the `GameManager` class

   - Add a branch to the switch statement for your `MinigameType` enum value. The switch statement is in the `me.timwastaken.minecraftparty.managers.GameManager` class (method `loadMinigame`). The code inside this branch stores the `activeMinigame` field for the GameManager. Store a new instance of your minigame into this field.

### Example:
    
```java
public static void loadMinigame(MinigameType type, Player... players) {
        boolean successful = true;
        switch (type) {
            // ...
            case MY_MINIGAME -> {
                activeMinigame = new MyMinigame(); // instantiate your minigame here and store it in the activeMinigame field
            }
            default -> successful = false;
        }
        if (successful) {
            activeMinigame.loadWorld();
            activeMinigame.startCountdown();
        }
    }
```
**To test out your minigame, you can use the command `/mg load your_plugin_alias` on a suitable server.**


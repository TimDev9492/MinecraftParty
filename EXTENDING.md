# How to add your own minigame

If you want to add your own minigame to the plugin, you have to follow these steps:

---

1. **Add to the plugin.yml**
   The `plugin.yml` file contains attributes about your minigame that get read by the plugin on start. Add your values to the `minigames` list.
   
   Required values are:
   
   - `origin` - This holds the x, y, and z coordinate for the player spawn. Players get teleported to this location when your minigame starts
   - `world_name` - The name of your world directory. Specify the value from `default_world.name` to use the default world.
   - `reward` - This value represents the reward factor associated with your game (Not implemented yet). Specify any number for the time being.

### Example section:

```yml
# ...
minigames:
# ...
  your_plugin_alias:
    reward: 1
    origin:
      x: 0
      y: 65
      z: 0
    world_name: my_plugin_world
```

---

2. **Add your plugin to the `MinigameType` enum**
   You have to register your 


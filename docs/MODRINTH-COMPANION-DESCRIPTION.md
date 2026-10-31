**Optional server add-on for [animal's final music replacer](https://modrinth.com/mod/animals-final-music-replacer).** Install it on a multiplayer server so AFMR players get the same music triggers they have in single-player.

AFMR works on any server without this. The companion only adds things a client can't see on its own:

- **Structures condition:** music for villages, strongholds, ancient cities and other structures works in multiplayer
- **Combat tracking:** combat music keeps playing while a hostile mob is targeting you, instead of using a simple timer
- **1.18.2 and 1.19.2:** combat also starts when you get hit or land a ranged hit, not only when you attack

---

## 🚀 Installing

1. Stop the server
2. Put the companion `.jar` matching your server's Minecraft version and loader in its `mods` folder
3. **Fabric servers** also need **Fabric API**
4. Start the server

Players install the normal [AFMR](https://modrinth.com/mod/animals-final-music-replacer) on their own game. Players without AFMR can still join and play as usual.

> **Don't** install the companion and the normal AFMR mod in the same game or server. They share a mod ID. Single-player doesn't need the companion.

---

## 📋 Requirements

| Minecraft | Loader | Also needs | Java |
| ---: | --- | --- | ---: |
| 26.3 | NeoForge 26.3.0.90 | — | 25 |
| 26.3 | Fabric Loader 0.20.1 | Fabric API | 25 |
| 1.21.1 | NeoForge 21.1.265 | — | 21 |
| 1.21.1 | Fabric Loader 0.19.5 | Fabric API | 21 |
| 1.20.1 | Forge 47.4.0 | — | 17 |
| 1.20.1 | Fabric Loader 0.19.5 | Fabric API | 17 |
| 1.19.2 | Forge 43.5.0 | — | 17 |
| 1.19.2 | Fabric Loader 0.19.5 | Fabric API | 17 |
| 1.18.2 | Forge 40.3.0 | — | 17 |
| 1.18.2 | Fabric Loader 0.19.5 | Fabric API | 17 |

---

## ❓ FAQ

**Does it play music or need music files?**

No. The companion has no music or audio code. Each player's music stays on their own computer.

**What does it share with players?**

Only what AFMR needs: nearby structure locations and which mobs are targeting that player. Nothing about anyone's music.

**Do I need it?**

Only if you run a server and your players use AFMR's Structures condition or want more accurate combat music.

---

### 💬 Help & feedback

Found a bug? **[Open an issue on GitHub](https://github.com/prawnlover07/AFMR/issues/new/choose)**. Questions? Join the **[AFMR Discord](https://discord.gg/48DFR6Zt4A)**.

### 📄 License

Free and open source under the **MIT license**, like AFMR.

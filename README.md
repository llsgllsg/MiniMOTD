![MiniMOTD logo](resources/minimotd-logo.png)

[![build](https://img.shields.io/github/checks-status/jpenilla/MiniMOTD/master?label=build)](https://github.com/jpenilla/MiniMOTD/actions) [![latest release](https://img.shields.io/github/v/release/jpenilla/MiniMOTD)](https://github.com/jpenilla/MiniMOTD/releases)

### MiniMOTD is a basic server list MOTD plugin/mod for Minecraft servers and proxies

- MiniMOTD supports RGB colors and gradients through [MiniMessage](https://docs.papermc.io/adventure/minimessage/), which is also where MiniMOTD gets its name.
- For more detailed info on formatting text, refer to the [MiniMessage docs](https://docs.papermc.io/adventure/minimessage/format/).
- RGB colors are automatically downsampled for outdated clients.
- RGB colors are only able to be sent by proxies and 1.16+ servers, and can only be seen by 1.16+ clients.

#### Server Platforms
- [Paper](https://papermc.io/)
- [Sponge API 8](https://www.spongepowered.org/)
- [Sponge API 7](https://www.spongepowered.org/)
- [Fabric](https://fabricmc.net/) (requires [Fabric API](https://modrinth.com/mod/fabric-api))
- [NeoForge](https://neoforged.net/)

#### Proxy Platforms
- [Velocity](https://velocitypowered.com/)
- [Waterfall](https://papermc.io/downloads#Waterfall) / Bungeecord

#### Downloads
Downloads can be obtained from any of:
 - [Modrinth](https://modrinth.com/plugin/minimotd)
 - [Hangar](https://hangar.papermc.io/jmp/MiniMOTD)
 - [GitHub releases](https://github.com/jpenilla/MiniMOTD/releases)

There is a separate jar for each platform. Waterfall and Bungeecord share the same jar.
There are two distributions for Bukkit-based servers: one for Paper >=1.21.8 only (`-paper` jar),
and one for all other supported versions (1.8.8 - 1.21.7, Spigot and Paper) (`-bukkit` jar).

#### Configuration
See the [wiki](https://github.com/jpenilla/MiniMOTD/wiki) for configuration details

#### Screenshots
![demo motd image](resources/minimotd-demo.png)

---

## 按请求域名显示不同的 MOTD（虚拟主机）

> 本分支新增功能。当玩家用不同的域名/地址 ping 服务器时，可以按请求的地址返回不同的 MOTD。
>
> **当前实现平台：Paper**（`-paper` jar 用于 Paper ≥ 1.21.8，`-bukkit` jar 用于 Paper 1.12 ~ 1.21.7）。

### 配置方法

在 `main.conf` 中新增 `motds-by-virtual-host` 配置段，格式为 `"hostname:port"` → MOTD 列表：

```hocon
motds-by-virtual-host {
  # 精确匹配（key 是玩家填写的服务器地址，必须包含端口）
  "play.example.com:25565" = [
    { line1 = "<green>Welcome to Play!", line2 = "<bold>Join us", icon = "random" }
  ]

  # 通配符：* 可匹配域名任意一级，按声明顺序匹配
  "*.mydomain.com:25565" = [
    { line1 = "<green><italic>Skyblock", line2 = "<bold><rainbow>Skyblock Server" }
  ]
}
```

每个条目是一个 MOTD 列表（可放多条，随机选择一条显示），每条支持与普通 MOTD 相同的字段：`line1` / `line2` / `icon`。

修改配置后执行 `/minimotd reload` 即可生效。

### 匹配规则

1. **精确匹配优先**：请求的 `hostname:port` 与配置 key 完全一致（不区分大小写）时命中。
2. **通配符匹配**：无精确匹配时，按配置声明顺序检查包含 `*` 的 key，`*` 匹配域名中任意一级。
3. **回落默认**：都没有命中时，使用普通的 `motds` 列表。
4. 兼容 TCPShield 伪装域名（`hostname///user-ip///timestamp///signature` 格式会自动还原 hostname）。

### 工作原理

- Paper 的 `PaperServerListPingEvent` 通过 `event.getClient().getVirtualHost()` 拿到客户端请求时使用的地址。
- `MiniMOTD.createMOTD(...)` 依据该地址调用 `MOTDConfig.motdsForVirtualHost(...)` 选择 MOTD 列表。
- 域名匹配逻辑抽在 `common/.../util/VirtualHostMatching.java`，与代理端（Velocity / BungeeCord）的 `virtual-host-configs` 复用同一套逻辑。

---

## 构建

项目使用 Gradle wrapper（gradle-9.6.1），构建指定平台 jar：

```bash
# 构建 Paper（Paper ≥ 1.21.8）
./gradlew :minimotd-paper:jar

# 构建 Bukkit 兼容包（Spigot / Paper 1.8.8 ~ 1.21.7）
./gradlew :minimotd-bukkit:jar
```

如果网络环境导致 wrapper 无法下载 Gradle 发行版，可下载 [gradle-9.6.1-bin.zip](https://services.gradle.org/distributions/gradle-9.6.1-bin.zip) 后解压，直接调用本地的 gradle：

```bash
# Windows 示例
gradle-9.6.1/bin/gradle.bat -p <项目路径> :minimotd-common:test
```

> 注意：`quiet-fabric-loom` 插件要求 Gradle ≥ 9.5，请勿使用低于 9.5 的版本。构建 `:minimotd-common` 等轻量模块时可用 `--configure-on-demand` 跳过 fabric / neoforge 模块的重型配置。

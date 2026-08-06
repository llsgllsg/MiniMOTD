/*
 * This file is part of MiniMOTD, licensed under the MIT License.
 *
 * Copyright (c) 2020-2025 Jason Penilla
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.minimotd.common.config;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import xyz.jpenilla.minimotd.common.PingResponse;
import xyz.jpenilla.minimotd.common.util.VirtualHostMatching;

import static xyz.jpenilla.minimotd.common.PingResponse.PlayerCount.playerCount;

@ConfigSerializable
public final class MOTDConfig {

  public MOTDConfig() {
    this(
      new MOTD(),
      new MOTD("<blue>Another <bold><red>MOTD", "<italic><underlined><gradient:red:green>much wow")
    );
  }

  public MOTDConfig(final @NonNull MOTD @NonNull ... defaults) {
    this.motds.addAll(Arrays.asList(defaults));
  }

  @Comment("The list of MOTDs to display\n"
    + "\n"
    + " - Supported placeholders: <online_players>, <max_players>\n"
    + " - Putting more than one will cause one to be randomly chosen each refresh")
  private final List<MOTD> motds = new ArrayList<>();

  @Comment("Optional: different MOTDs to display when the server is pinged using a specific address.\n"
    + "This allows showing different MOTDs depending on the domain the player used to connect.\n"
    + "\n"
    + "Format is \"hostname:port\"=[list of MOTDs], where each MOTD uses the same fields as above.\n"
    + "    ex: \"play.example.com:25565\"=[{line1=\"<green>Welcome to Play!\", line2=\"<bold>Join us\", icon=\"random\"}]\n"
    + "Parts of domains can be substituted for wildcards, i.e. \"*.mydomain.com:25565\"=[...].\n"
    + "Wildcard-containing entries are checked in the order they are declared if there are no exact matches.\n"
    + "If the pinged address does not match any entry, the MOTDs listed above will be used.")
  private final Map<String, List<MOTD>> motdsByVirtualHost = new LinkedHashMap<>();

  private transient @Nullable Map<String[], List<MOTD>> splitMotdsByVirtualHost;

  @Comment("Enable MOTD-related features")
  private boolean motdEnabled = true;

  @Comment("Enable server list icon related features")
  private boolean iconEnabled = true;

  private PlayerCountSettings playerCountSettings = new PlayerCountSettings();

  @ConfigSerializable
  public static final class MOTD {

    public MOTD() {
    }

    public MOTD(final @NonNull String line1, final @NonNull String line2) {
      this.line1 = line1;
      this.line2 = line2;
    }

    private String line1 = "<rainbow>MiniMOTD Default";

    private String line2 = "MiniMessage <gradient:blue:red>Gradients";

    @Comment("Set the icon to use with this MOTD\n"
      + "  Either use 'random' to randomly choose an icon, or use the name\n"
      + "  of a file in the icons folder (excluding the '.png' extension)\n"
      + "    ex: icon=\"myIconFile\"")
    private String icon = "random";

    public @NonNull String line1() {
      return this.line1;
    }

    public @NonNull String line2() {
      return this.line2;
    }

    public @NonNull String icon() {
      return this.icon;
    }

  }

  @ConfigSerializable
  public static final class PlayerCountSettings {

    @Comment("Enable modification of the max player count")
    private boolean maxPlayersEnabled = true;

    @Comment("Changes the Max Players value")
    private int maxPlayers = 69;

    @Comment("Setting this to true will disable the hover text showing online player usernames")
    private boolean disablePlayerListHover = false;

    @Comment("Setting this to true will disable the player list hover (same as 'disable-player-list-hover'),\n"
      + "but will also cause the player count to appear as '???'")
    private boolean hidePlayerCount = false;

    @Comment("Settings for the fake player count feature")
    private FakePlayers fakePlayers = new FakePlayers();

    @Comment("Changes the Max Players to be X more than the online players\n"
      + "ex: x=3 -> 16/19 players online.")
    private JustXMore justXMoreSettings = new JustXMore();

    @Comment("Should the displayed online player count be allowed to exceed the displayed maximum player count?\n"
      + "If false, the online player count will be capped at the maximum player count")
    private boolean allowExceedingMaximum = false;

    @Comment("The list of server names that affect player counts/listing.\n"
      + "Only applicable when running the plugin on a proxy (Velocity or Waterfall/Bungeecord).\n"
      + "When set to an empty list, the default count & list as determined by the proxy will be used.")
    private final List<String> servers = new ArrayList<>();

    @ConfigSerializable
    public static final class JustXMore {

      @Comment("Enable this feature")
      private boolean justXMoreEnabled = false;

      private int xValue = 3;

    }

    @ConfigSerializable
    public static final class FakePlayers {

      @Comment("Enable fake player count feature")
      private boolean fakePlayersEnabled = false;

      @Comment("Modes: add, constant, minimum, random, percent\n"
        + "\n"
        + " - add: This many fake players will be added\n"
        + "     ex: fake-players=\"3\"\n"
        + " - constant: A constant value for the player count\n"
        + "     ex: fake-players=\"=42\"\n"
        + " - minimum: The minimum bound of the player count\n"
        + "     ex: fake-players=\"7+\"\n"
        + " - random: A random number of fake players in this range will be added\n"
        + "     ex: fake-players=\"3:6\"\n"
        + " - percent: The player count will be inflated by this much, rounding up\n"
        + "     ex: fake-players=\"25%\"")
      private PlayerCountModifier fakePlayers = PlayerCountModifier.parse("25%");

    }

  }

  public List<String> targetServers() {
    return this.playerCountSettings.servers;
  }

  public boolean iconEnabled() {
    return this.iconEnabled;
  }

  public @NonNull List<MOTD> motds() {
    return this.motds;
  }

  /**
   * Returns the MOTDs to use for the given virtual host.
   *
   * <p>If the client pinged using an address configured in {@link #motdsByVirtualHost}, its MOTDs
   * are returned; otherwise the main {@link #motds} list is returned.
   *
   * @param address the address the client used to ping the server, or {@code null}
   * @return the MOTDs to display
   */
  public @NonNull List<MOTD> motdsForVirtualHost(final @Nullable InetSocketAddress address) {
    if (address == null) {
      return this.motds;
    }
    final @Nullable List<MOTD> override = VirtualHostMatching.find(
      this.motdsByVirtualHost,
      this.splitMotdsByVirtualHost,
      address.getHostString(),
      address.getPort()
    );
    return override != null ? override : this.motds;
  }

  @PostProcessor
  private void processVirtualHosts() {
    final Map<String, List<MOTD>> virtualHosts = new LinkedHashMap<>(this.motdsByVirtualHost);
    this.motdsByVirtualHost.clear();
    virtualHosts.forEach((host, motds) -> this.motdsByVirtualHost.put(host.toLowerCase(Locale.ENGLISH), motds));

    this.splitMotdsByVirtualHost = new LinkedHashMap<>();
    this.motdsByVirtualHost.forEach((host, motds) -> {
      if (!host.contains("*")) {
        return;
      }
      this.splitMotdsByVirtualHost.put(host.split("\\."), motds);
    });
  }

  public boolean motdEnabled() {
    return this.motdEnabled;
  }

  public boolean disablePlayerListHover() {
    return this.playerCountSettings.disablePlayerListHover;
  }

  public boolean hidePlayerCount() {
    return this.playerCountSettings.hidePlayerCount;
  }

  private @NonNull PlayerCountModifier playerCountModifier() {
    return this.playerCountSettings.fakePlayers.fakePlayers;
  }

  private int calculateOnlinePlayers(final int onlinePlayers) {
    if (this.playerCountSettings.fakePlayers.fakePlayersEnabled) {
      return this.playerCountModifier().apply(onlinePlayers);
    }
    return onlinePlayers;
  }

  private int calculateMaxPlayers(final int onlinePlayers, final int maxPlayers) {
    if (this.playerCountSettings.maxPlayersEnabled) {
      if (this.playerCountSettings.justXMoreSettings.justXMoreEnabled) {
        return onlinePlayers + this.playerCountSettings.justXMoreSettings.xValue;
      }
      return this.playerCountSettings.maxPlayers;
    }
    return maxPlayers;
  }

  public PingResponse.@NonNull PlayerCount modifyPlayerCount(final int onlinePlayers, final int maxPlayers) {
    final int online = this.calculateOnlinePlayers(onlinePlayers);
    final int max = this.calculateMaxPlayers(online, maxPlayers);
    if (!this.playerCountSettings.allowExceedingMaximum) {
      return playerCount(Math.min(online, max), max);
    }
    return playerCount(online, max);
  }
}

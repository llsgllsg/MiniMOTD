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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MOTDConfigVirtualHostTest {
  @TempDir
  Path tempDir;

  @Test
  void testPerVirtualHostMotds() throws IOException, ConfigurateException {
    final Path conf = this.tempDir.resolve("test.conf");
    Files.writeString(conf, """
      motds-by-virtual-host {
        "play.example.com:25565" = [
          { line1 = "Play line1", line2 = "Play line2", icon = "play" }
        ]
        "*.mydomain.com:25565" = [
          { line1 = "Wild line1", line2 = "Wild line2" }
          { line1 = "Wild line1 alt", line2 = "Wild line2 alt" }
        ]
      }
      """);

    final ConfigLoader<MOTDConfig> loader = new ConfigLoader<>(MOTDConfig.class, conf);
    final MOTDConfig config = loader.load();

    // exact match
    final List<MOTDConfig.MOTD> exact = config.motdsForVirtualHost(InetSocketAddress.createUnresolved("play.example.com", 25565));
    assertEquals(1, exact.size());
    assertEquals("Play line1", exact.get(0).line1());
    assertEquals("Play line2", exact.get(0).line2());
    assertEquals("play", exact.get(0).icon());

    // exact match is case-insensitive
    assertSame(exact, config.motdsForVirtualHost(InetSocketAddress.createUnresolved("PLAY.EXAMPLE.COM", 25565)));

    // wildcard match
    final List<MOTDConfig.MOTD> wild = config.motdsForVirtualHost(InetSocketAddress.createUnresolved("sub.mydomain.com", 25565));
    assertEquals(2, wild.size());
    assertEquals("Wild line1", wild.get(0).line1());
    assertEquals("Wild line2 alt", wild.get(1).line2());

    // different port does not match
    assertSame(config.motds(), config.motdsForVirtualHost(InetSocketAddress.createUnresolved("play.example.com", 25566)));

    // no match falls back to the main motds list
    final List<MOTDConfig.MOTD> noMatch = config.motdsForVirtualHost(InetSocketAddress.createUnresolved("other.com", 25565));
    assertSame(config.motds(), noMatch);

    // null host falls back to the main motds list
    assertSame(config.motds(), config.motdsForVirtualHost(null));
  }
}

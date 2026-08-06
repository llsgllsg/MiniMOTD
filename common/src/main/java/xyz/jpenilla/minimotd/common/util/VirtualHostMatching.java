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
package xyz.jpenilla.minimotd.common.util;

import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Matches a requested virtual host ({@code "hostname:port"}) against a configured map of hosts,
 * supporting exact matches, wildcard patterns, and TCPShield-mangled hostnames.
 */
public final class VirtualHostMatching {
  private VirtualHostMatching() {
  }

  /**
   * Finds the value associated with the given host &amp; port.
   *
   * <p>Exact matches take priority. If no exact match is found, wildcard-containing patterns are
   * checked in iteration order, where a {@code *} part matches any single part of the hostname.
   *
   * @param exactMatches    map of exact {@code "hostname:port"} keys to values
   * @param wildcardMatches map of split {@code "hostname:port"} patterns to values, or {@code null}
   *                        if no wildcard patterns have been processed
   * @param host            the hostname used by the client
   * @param port            the port used by the client
   * @param <V>             the value type
   * @return the matched value, or {@code null} if no entry matches
   */
  public static <V> @Nullable V find(
    final @NonNull Map<String, V> exactMatches,
    final @Nullable Map<String[], V> wildcardMatches,
    final @NonNull String host,
    final int port
  ) {
    final String normalized = processTcpShieldHostname(host).toLowerCase(Locale.ENGLISH) + ':' + port;

    final @Nullable V exactMatch = exactMatches.get(normalized);
    if (exactMatch != null) {
      return exactMatch;
    }

    if (wildcardMatches == null || wildcardMatches.isEmpty()) {
      return null;
    }

    final String[] splitHost = normalized.split("\\.");

    configs:
    for (final Map.Entry<String[], V> e : wildcardMatches.entrySet()) {
      final String[] splitKey = e.getKey();
      if (splitKey.length != splitHost.length) {
        continue;
      }
      for (int i = 0; i < splitHost.length; i++) {
        final String keyPart = splitKey[i];
        if (!keyPart.equals(splitHost[i]) && !keyPart.equals("*")) {
          continue configs;
        }
      }
      return e.getValue();
    }

    return null;
  }

  private static String processTcpShieldHostname(final @NonNull String hostname) {
    if (hostname.contains("///")) {
      final String[] split = hostname.split("///");
      if (split.length == 4) {
        // <actual hostname>///<user-ip>:<user-port>///<unix timestamp>///<signature>
        return split[0];
      }
    }
    return hostname;
  }
}

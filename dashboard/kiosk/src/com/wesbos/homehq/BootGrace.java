package com.wesbos.homehq;

/**
 * Right after boot, Android is up a few seconds before Wi-Fi rejoins, so the first page load fails
 * and the offline screen would flash "Connect to Wi-Fi?" (and then, as Wi-Fi connects, "Can't
 * reach the dashboard") on every boot, as seen on the Runxion frames. During this window the
 * offline screen stays dark and wordless; once it's over, the real reason shows as before.
 */
public final class BootGrace {
  private BootGrace() {}

  /** How long after boot a failed load is treated as "still connecting". */
  public static final long WIFI_JOIN_MS = 60_000;

  /**
   * Whether the offline screen should be blank rather than explain what's wrong.
   *
   * @param sinceBootMs milliseconds since boot ({@code SystemClock.elapsedRealtime()})
   */
  public static boolean quietOffline(long sinceBootMs) {
    return sinceBootMs < WIFI_JOIN_MS;
  }
}

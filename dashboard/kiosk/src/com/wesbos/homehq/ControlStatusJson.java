package com.wesbos.homehq;

public final class ControlStatusJson {
  private ControlStatusJson() {}

  static String quote(String s) {
    StringBuilder b = new StringBuilder("\"");
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"' || c == '\\') b.append('\\').append(c);
      else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
      else b.append(c);
    }
    return b.append('"').toString();
  }

  public static String build(boolean screen, int brightness, String url, long uptime,
      Integer rssi, String appVersion, boolean canReboot) {
    return "{\"screen\":" + screen
        + ",\"brightness\":" + brightness
        + ",\"url\":" + quote(url == null ? "" : url)
        + ",\"uptime\":" + uptime
        + ",\"rssi\":" + (rssi == null ? "null" : rssi.toString())
        + ",\"appVersion\":" + quote(appVersion == null ? "" : appVersion)
        + ",\"capabilities\":" + (canReboot ? "[\"reboot\"]" : "[]")
        + "}";
  }
}

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

  /** Drops the client key from a reported URL's query string; anything unparseable is returned as is. */
  public static String redactKey(String url) {
    if (url == null) return null;
    int q = url.indexOf('?');
    if (q < 0) return url;
    int h = url.indexOf('#', q);
    String query = h < 0 ? url.substring(q + 1) : url.substring(q + 1, h);
    String frag = h < 0 ? "" : url.substring(h);
    StringBuilder kept = new StringBuilder();
    for (String part : query.split("&")) {
      if (part.isEmpty() || part.equals("key") || part.startsWith("key=")) continue;
      if (kept.length() > 0) kept.append('&');
      kept.append(part);
    }
    return url.substring(0, q) + (kept.length() > 0 ? "?" + kept : "") + frag;
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

package com.wesbos.homehq;

/** One command from the homeboard server. Unknown or malformed commands parse to null. */
public final class ControlCommand {
  public final String type;
  public final int value;
  public final boolean on;
  public final String url;

  private ControlCommand(String type, int value, boolean on, String url) {
    this.type = type;
    this.value = value;
    this.on = on;
    this.url = url;
  }

  public static ControlCommand parse(String event, String data) {
    String d = data == null ? "" : data.trim();
    switch (event) {
      case "brightness":
        try {
          int v = Integer.parseInt(d);
          return v >= 0 && v <= 100 ? new ControlCommand(event, v, false, null) : null;
        } catch (NumberFormatException e) {
          return null;
        }
      case "screen":
        if ("on".equals(d)) return new ControlCommand(event, 0, true, null);
        if ("off".equals(d)) return new ControlCommand(event, 0, false, null);
        return null;
      case "url":
        if (d.isEmpty()) return new ControlCommand(event, 0, false, null);
        return d.startsWith("http://") || d.startsWith("https://")
            ? new ControlCommand(event, 0, false, d) : null;
      case "reload":
      case "restart-kiosk":
      case "reboot":
      case "ping":
        return new ControlCommand(event, 0, false, null);
      default:
        return null;
    }
  }
}

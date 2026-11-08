package com.wesbos.homehq;

/** Incremental parser for the server-sent events homeboard sends: event/data lines, blank line dispatches. */
public final class ControlSse {
  public interface Sink {
    void onEvent(String event, String data);
  }

  private final Sink sink;
  private String event = "message";
  private StringBuilder data;

  public ControlSse(Sink sink) {
    this.sink = sink;
  }

  public void line(String line) {
    if (line.isEmpty()) {
      if (data != null) sink.onEvent(event, data.toString());
      event = "message";
      data = null;
      return;
    }
    if (line.startsWith(":")) return;
    int colon = line.indexOf(':');
    String field = colon < 0 ? line : line.substring(0, colon);
    String value = colon < 0 ? "" : line.substring(colon + 1);
    if (value.startsWith(" ")) value = value.substring(1);
    if (field.equals("event")) {
      event = value;
    } else if (field.equals("data")) {
      if (data == null) data = new StringBuilder(value);
      else data.append('\n').append(value);
    }
  }
}

package com.wesbos.homehq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Holds one SSE connection to homeboard's control stream; reconnects with backoff. Pure Java. */
public final class ControlClient implements Runnable {
  public interface Listener {
    void onCommand(ControlCommand c);
    void onConnected(boolean up);
  }

  private final String base, id, key;
  private final Listener listener;
  private final ControlBackoff backoff = new ControlBackoff(1000, 30000);
  private volatile boolean stopped;
  private volatile HttpURLConnection conn;
  private Thread thread;

  public ControlClient(String base, String id, String key, Listener listener) {
    this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    this.id = id;
    this.key = key;
    this.listener = listener;
  }

  public void start() {
    thread = new Thread(this, "homeboard-control");
    thread.setDaemon(true);
    thread.start();
  }

  public void stop() {
    stopped = true;
    HttpURLConnection c = conn;
    if (c != null) c.disconnect();
    if (thread != null) thread.interrupt();
  }

  @Override
  public void run() {
    while (!stopped) {
      try {
        HttpURLConnection c = (HttpURLConnection) new URL(base + "/api/clients/" + id + "/control")
            .openConnection();
        conn = c;
        c.setRequestProperty("Accept", "text/event-stream");
        c.setRequestProperty("X-Client-Key", key);
        c.setConnectTimeout(10000);
        c.setReadTimeout(60000); // server sends a keep-alive every 25 s
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        backoff.reset();
        listener.onConnected(true);
        ControlSse sse = new ControlSse(new ControlSse.Sink() {
          @Override
          public void onEvent(String event, String data) {
            ControlCommand cmd = ControlCommand.parse(event, data);
            if (cmd != null) listener.onCommand(cmd);
          }
        });
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        String line;
        while (!stopped && (line = r.readLine()) != null) sse.line(line);
      } catch (IOException e) {
        // retry below
      } finally {
        HttpURLConnection c = conn;
        if (c != null) c.disconnect();
        conn = null;
      }
      listener.onConnected(false);
      if (stopped) return;
      try {
        Thread.sleep(backoff.next());
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  public static int postStatus(String base, String id, String key, String json) throws IOException {
    String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    HttpURLConnection c = (HttpURLConnection) new URL(b + "/api/clients/" + id + "/status").openConnection();
    try {
      c.setRequestMethod("POST");
      c.setDoOutput(true);
      c.setConnectTimeout(10000);
      c.setReadTimeout(10000);
      c.setRequestProperty("Content-Type", "application/json");
      c.setRequestProperty("X-Client-Key", key);
      byte[] body = json.getBytes("UTF-8");
      OutputStream os = c.getOutputStream();
      os.write(body);
      os.close();
      return c.getResponseCode();
    } finally {
      c.disconnect();
    }
  }
}

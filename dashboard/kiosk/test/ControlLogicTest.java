import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ControlLogicTest {
  static int failures = 0;

  static void check(boolean ok, String what) {
    if (!ok) {
      failures++;
      System.out.println("FAIL " + what);
    } else {
      System.out.println("PASS " + what);
    }
  }

  public static void main(String[] args) throws Exception {
    final List<String> got = new ArrayList<>();
    com.wesbos.homehq.ControlSse sse = new com.wesbos.homehq.ControlSse(
        (event, data) -> got.add(event + "|" + data));
    for (String l : new String[] {": open", "", "event: brightness", "data: 40", "",
        "event: reload", "data: ", "", "event: url", "data: http://a/b", ""}) {
      sse.line(l);
    }
    check(got.size() == 3, "sse dispatches three events, ignores comments");
    check("brightness|40".equals(got.get(0)), "sse event + data");
    check("reload|".equals(got.get(1)), "sse empty data");

    com.wesbos.homehq.ControlCommand c = com.wesbos.homehq.ControlCommand.parse("brightness", "40");
    check(c != null && c.value == 40, "parse brightness");
    check(com.wesbos.homehq.ControlCommand.parse("brightness", "140") == null, "reject brightness > 100");
    check(com.wesbos.homehq.ControlCommand.parse("screen", "off").on == false, "parse screen off");
    check(com.wesbos.homehq.ControlCommand.parse("url", "").url == null, "empty url = home");
    check(com.wesbos.homehq.ControlCommand.parse("url", "javascript:x") == null, "reject non-web url");
    check(com.wesbos.homehq.ControlCommand.parse("nope", "") == null, "unknown event");

    com.wesbos.homehq.ControlBackoff b = new com.wesbos.homehq.ControlBackoff(1000, 30000);
    long a1 = b.next(), a2 = b.next(), a3 = b.next();
    for (int i = 0; i < 10; i++) b.next();
    check(a1 == 1000 && a2 == 2000 && a3 == 4000 && b.next() == 30000, "backoff doubles to max");
    b.reset();
    check(b.next() == 1000, "backoff reset");

    String json = com.wesbos.homehq.ControlStatusJson.build(true, 55, "http://x/\"q\"", 12, null, "1.3", true);
    check(json.equals("{\"screen\":true,\"brightness\":55,\"url\":\"http://x/\\\"q\\\"\",\"uptime\":12,"
        + "\"rssi\":null,\"appVersion\":\"1.3\",\"capabilities\":[\"reboot\"]}"), "status json");

    // ControlClient against a local SSE server.
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    final String[] seenKey = new String[1];
    server.createContext("/api/clients/kitchen/control", ex -> {
      seenKey[0] = ex.getRequestHeaders().getFirst("X-Client-Key");
      ex.getResponseHeaders().add("content-type", "text/event-stream");
      ex.sendResponseHeaders(200, 0);
      OutputStream os = ex.getResponseBody();
      os.write("event: reload\ndata: \n\n".getBytes("UTF-8"));
      os.flush();
      os.close();
    });
    server.start();
    final CountDownLatch latch = new CountDownLatch(1);
    final List<String> cmds = new ArrayList<>();
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    com.wesbos.homehq.ControlClient client = new com.wesbos.homehq.ControlClient(base, "kitchen", "k1",
        new com.wesbos.homehq.ControlClient.Listener() {
          public void onCommand(com.wesbos.homehq.ControlCommand cmd) {
            cmds.add(cmd.type);
            latch.countDown();
          }
          public void onConnected(boolean up) {}
        });
    client.start();
    check(latch.await(5, TimeUnit.SECONDS) && "reload".equals(cmds.get(0)), "client receives command");
    check("k1".equals(seenKey[0]), "client sends key header");
    client.stop();
    server.stop(0);

    if (failures > 0) {
      System.out.println(failures + " failed");
      System.exit(1);
    }
    System.out.println("all passed");
  }
}

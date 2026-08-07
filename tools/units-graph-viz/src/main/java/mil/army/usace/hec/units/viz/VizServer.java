package mil.army.usace.hec.units.viz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Serves the generated visualization over HTTP.
*/
public final class VizServer {

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "build/reports/viz")
                        .toAbsolutePath().normalize();
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        if (!Files.isDirectory(root)) {
            System.err.println("Nothing generated at " + root
                + " - run './gradlew :units-graph-viz:visualize' first.");
            System.exit(2);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> serve(exchange, root));
        server.start();

        System.out.println("Serving " + root);
        System.out.println();
        System.out.println("    http://localhost:" + port + "/");
        System.out.println();
        System.out.println("Ctrl+C to stop.");
    }

    private static void serve(HttpExchange exchange, Path root) throws IOException {
        String requested = exchange.getRequestURI().getPath();
        if (requested.endsWith("/")) {
            requested = requested + "index.html";
        }

        Path file = root.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            respond(exchange, 404, "text/plain; charset=utf-8",
                    "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }

        respond(exchange, 200, contentType(file), Files.readAllBytes(file));
    }

    private static void respond(HttpExchange exchange, int status, String type, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().add("Content-Type", type);
        // don't cache
        exchange.getResponseHeaders().add("Cache-Control", "no-store");

        // The page is one big self-contained file of markup and text, which
        // compresses to a fraction of its size. Worth it even over localhost.
        if (compressible(type) && accepts(exchange, "gzip")) {
            body = gzip(body);
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
        }

        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static boolean compressible(String type) {
        return type.startsWith("text/") || type.startsWith("application/json")
            || type.startsWith("image/svg");
    }

    private static boolean accepts(HttpExchange exchange, String encoding) {
        String header = exchange.getRequestHeaders().getFirst("Accept-Encoding");
        return header != null && header.contains(encoding);
    }

    private static byte[] gzip(byte[] body) throws IOException {
        var buffer = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(buffer)) {
            out.write(body);
        }
        return buffer.toByteArray();
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
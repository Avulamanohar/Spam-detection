import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SpamCollectorServer {

    static class CheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{ \"error\": \"Only GET allowed\" }");
                return;
            }

            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();

            if (query == null || !query.startsWith("message=")) {
                sendResponse(exchange, 400, "{ \"error\": \"Missing ?message= parameter\" }");
                return;
            }

            String rawMessage = query.substring(8);
            String decodedMessage = URLDecoder.decode(rawMessage, StandardCharsets.UTF_8);

            String verdict = SpamDatabase.findVerdict(decodedMessage);
            String response = "{ \"verdict\": " + (verdict == null ? "null" : "\"" + verdict + "\"") + " }";

            sendResponse(exchange, 200, response);
        }
    }

    static class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{ \"error\": \"Only POST allowed\" }");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) body.append(line);

            try {
                JSONObject json = new JSONObject(body.toString());
                String message = json.getString("message");
                String verdict = json.getString("verdict");

                SpamDatabase.insertMessage(message, verdict);
                sendResponse(exchange, 200, "{ \"status\": \"saved\" }");

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 400, "{ \"error\": \"Invalid JSON\" }");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/messages/check", new CheckHandler());
        server.createContext("/api/messages/save", new SaveHandler());

        server.setExecutor(null);
        System.out.println("Java API running:");
        System.out.println(" GET  http://localhost:8080/api/messages/check?message=Free%20iPhone");
        System.out.println("POST http://localhost:8080/api/messages/save { \"message\": \"...\", \"verdict\": \"spam/ham\" }");
        server.start();
    }
}

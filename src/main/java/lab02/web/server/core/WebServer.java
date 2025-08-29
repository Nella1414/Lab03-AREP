package lab02.web.server.core;

import lab02.web.server.http.HttpMethod;
import lab02.web.server.http.Request;
import lab02.web.server.http.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

public class WebServer {
    private int listeningPort;
    private String assetsRoot;
    private final Map<String, Map<HttpMethod, Handler>> registry;

    public WebServer() {
        this(8000, "src/main/resources/static");
    }

    public WebServer(int port, String staticPath) {
        this.listeningPort = port;
        this.assetsRoot = staticPath;
        this.registry = new HashMap<>();
    }

    public WebServer port(int port) {
        this.listeningPort = port;
        return this;
    }

    public WebServer staticPath(String staticPath) {
        this.assetsRoot = staticPath;
        return this;
    }

    public void register(HttpMethod method, String path, Handler handler) {
        map(path, method, handler);
    }

    private void map(String path, HttpMethod method, Handler handler) {
        registry.computeIfAbsent(path, k -> new HashMap<>()).put(method, handler);
    }

    public void get(String path, Handler handler) {
        map(path, HttpMethod.GET, handler);
    }

    public void post(String path, Handler handler) {
        map(path, HttpMethod.POST, handler);
    }

    public void put(String path, Handler handler) {
        map(path, HttpMethod.PUT, handler);
    }

    public void patch(String path, Handler handler) {
        map(path, HttpMethod.PATCH, handler);
    }

    public void delete(String path, Handler handler) {
        map(path, HttpMethod.DELETE, handler);
    }

    private boolean exists(String path) {
        return Files.isRegularFile(Paths.get(path));
    }

    private byte[] readBytes(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private String mime(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm"))
            return "text/html";
        if (lower.endsWith(".css"))
            return "text/css";
        if (lower.endsWith(".js"))
            return "application/javascript";
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".gif"))
            return "image/gif";
        if (lower.endsWith(".svg"))
            return "image/svg+xml";
        if (lower.endsWith(".ico"))
            return "image/x-icon";
        if (lower.endsWith(".mp4"))
            return "video/mp4";
        if (lower.endsWith(".json"))
            return "application/json";
        return "text/plain";
    }

    public Response handleRequest(Request request) {
        String path = request.getPath();
        int q = path == null ? -1 : path.indexOf('?');
        String cleanPath = path == null ? "/" : (q >= 0 ? path.substring(0, q) : path);
        Map<HttpMethod, Handler> byMethod = registry.get(cleanPath);
        Handler handler = byMethod != null ? byMethod.get(request.getMethod()) : null;
        Response response = new Response();
        if (path == null || path.isEmpty())
            path = "/";
        if (cleanPath.endsWith("/"))
            cleanPath = cleanPath + "index.html";
        String candidate = this.assetsRoot + cleanPath;

        if (handler != null) {
            handler.handle(request, response);
        } else if (exists(candidate)) {
            response.setStatusCode(200);
            response.setHeader("Content-Type", mime(candidate));
            response.setBody(readBytes(candidate));
        } else {
            response.setStatusCode(404);
            response.setStatusMessage("Not Found");
            response.setBody("Static file or handler for " + request.getPath() + " not found");
        }

        return response;
    }

    public void writeResponse(OutputStream output, Response res) throws IOException {
        output.write(("HTTP/1.1 " + res.getStatusCode() + " " + res.getStatusMessage() + "\r\n").getBytes());
        for (Map.Entry<String, String> h : res.getHeaders().entrySet()) {
            output.write((h.getKey() + ": " + h.getValue() + "\r\n").getBytes());
        }
        output.write("\r\n".getBytes());
        output.write(res.getBody());
    }

    public void start() {
        log("INFO", "server_start", "port=" + listeningPort + " static_root=" + assetsRoot);

        try (ServerSocket server = new ServerSocket(listeningPort)) {
            while (true) {
                try (Socket client = server.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        OutputStream out = client.getOutputStream()) {

                    Request req = Request.fromBufferedReader(in);
                    log("INFO", "http_request", "method=" + req.getMethod() + " path=" + req.getPath());
                    Response res = handleRequest(req);
                    writeResponse(out, res);
                    log("INFO", "http_response", "status=" + res.getStatusCode() + " path=" + req.getPath());
                } catch (IOException e) {
                    log("WARN", "connection_error", "error=" + safe(e.getMessage()));
                }
            }
        } catch (IOException e) {
            log("ERROR", "server_boot_failure", "error=" + safe(e.getMessage()));
        }
    }

    private void log(String level, String event, String details) {
        String ts = Instant.now().toString();
        System.out.println("[web] ts=" + ts + " level=" + level + " event=" + event
                + (details == null || details.isEmpty() ? "" : " " + details));
    }

    private String safe(String s) {
        return s == null ? "(null)" : s.replaceAll("\r|\n", " ");
    }
}

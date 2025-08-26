package lab02.web;

import lab02.web.server.WebServer;

/**
 * Demo app spinning up the lightweight HTTP server.
 */
public class App {
    public static void main(String[] args) {
        // You can also chain: new
        // WebServer().port(8080).staticPath("src/main/resources/static")
        WebServer http = new WebServer(8080, "src/main/resources/static");

        http.get("/health", (req, res) -> {
            res.setBody("The server is healthy");
        });

        http.start();
    }
}

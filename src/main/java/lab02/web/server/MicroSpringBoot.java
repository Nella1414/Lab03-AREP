package lab02.web.server;

import lab02.web.server.WebServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MicroSpringBoot {
    public static void main(String[] args) {

        WebServer server = new WebServer();
        server.start();
    }
}

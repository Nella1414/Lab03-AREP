package lab02.web.server.core;

import lab02.web.server.http.Request;
import lab02.web.server.http.Response;

@FunctionalInterface
public interface Handler {
    void handle(Request req, Response res);
}

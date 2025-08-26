package lab02.web.server;

@FunctionalInterface
public interface Handler {
    void handle(Request req, Response res);
}

# Lab 03 — Lightweight Java Web Framework (TDSE)

A minimal web framework built in Java that serves static files and lets you define REST endpoints with concise lambda handlers. It includes query parameter parsing and a tiny routing table.

Author: Marianella Polo Peña — for the TDSE course at Escuela Colombiana de Ingeniería, under Professor Luis Daniel Benavides.

## What’s here
- Static file server (HTML, JS, CSS, images) from a configurable folder.
- Simple REST routing with lambda-style handlers for HTTP methods (GET, POST, PUT, PATCH, DELETE).
- Query parameter extraction available to handlers.
- Small example app with a health check.

## How to run
Prerequisites: Java 17+ and Maven.

1) Clone the repository
```
git clone https://github.com/Nella1414/Lab02-AREP.git
cd Lab02-AREP
```
2) Build and run
```
mvn clean package
mvn exec:java
```
3) Try it in your browser
- Static file: http://localhost:8080/index.html
- Health check: http://localhost:8080/health

## Example: defining REST services
In `lab02.web.App` you can register routes and set the static folder. For example:

```java
WebServer http = new WebServer(8080, "src/main/resources/static");

http.get("/hello", (req, res) -> {
    String name = req.getQueryParams().getOrDefault("name", "world");
    res.setBody("Hello " + name);
});

http.get("/pi", (req, res) -> {
    res.setBody(String.valueOf(Math.PI));
});

http.start();
```

Then call:
- http://localhost:8080/hello?name=Pedro
- http://localhost:8080/pi

## Project structure
- `src/main/java/lab02/web/App.java` — Application entry point; wires the server and routes.
- `src/main/java/lab02/web/server/` — Minimal framework:
  - `WebServer` — Routing table, static file serving, request loop.
  - `Request` — Parses method, path, version, headers, query params, and JSON body.
  - `Response` — Status, headers, and body writer.
  - `Handler` — Functional interface for lambda handlers.
  - `HttpMethod` — Supported verbs.
- `src/main/resources/static/` — Public web assets (e.g., `index.html`).
- `src/test/java/lab02/` — Unit tests (JUnit 5).

## Architecture at a glance
- Networking: A basic `ServerSocket` accept loop handles one connection at a time.
- Parsing: `Request` builds from the raw HTTP request, extracting headers, query params, and optional JSON body.
- Routing: `WebServer` keeps a `Map<String, Map<HttpMethod, Handler>>`; the best-matching handler is invoked.
- Static files: If no handler matches, the server resolves files under the configured static root and returns bytes with a simple MIME guesser.
- Responses: `Response` aggregates status, headers, and payload; the server serializes it to HTTP/1.1.

## Notes
- Default static folder in this project is `src/main/resources/static`. When packaged, Maven also copies resources under `target/classes/static/`.
- The current demo includes a `/health` endpoint and serves `index.html` from the static folder.

---
Made by Marianella Polo Peña for the TDSE course at Escuela Colombiana de Ingeniería — Professor Luis Daniel Benavides.
# MicroSpringBoot — Annotation-based Micro Web Framework

MicroSpringBoot is a tiny annotation-driven Java web framework with a minimal HTTP server. It serves static files and lets you define REST endpoints using annotations (recommended) or simple lambda handlers.

Author: Marianella Polo Peña — for the TDSE course at Escuela Colombiana de Ingeniería, under Professor Luis Daniel Benavides.

## Features
- Static file server from `src/main/resources/static`.
- Annotation-driven REST controllers: `@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`.
- Query parameter binding with `@QueryParam` supporting: `String`, `int/Integer`, `long/Long`, `double/Double`, `boolean/Boolean`.
- Minimal bootstrap with `MicroSpringBoot.run(App.class, 8080)`.
- Optional low-level API: `WebServer#get/post/put/patch/delete` for manual routing.

## Clone the repository
Prerequisites: Git.

```bash
git clone https://github.com/Nella1414/Lab03-AREP.git
cd Lab03-AREP
```

## How to run
Prerequisites: Java 17+ and Maven.

1) Build
```bash
mvn -q -DskipTests clean package
```
2) Run
```bash
mvn -q -Dexec.mainClass=lab02.web.App exec:java
```
3) Try it
- Static file: http://localhost:8080/index.html
- Demo endpoint: http://localhost:8080/hello
- With query: http://localhost:8080/hello?name=YourName

## Example (annotation-based)
`src/main/java/lab02/web/controller/HelloController.java`

```java
import lab02.web.server.annotations.RestController;
import lab02.web.server.annotations.GetMapping;
import lab02.web.server.annotations.QueryParam;

@RestController
public class HelloController {
  @GetMapping("/hello")
  public String hello(@QueryParam(value = "name", defaultValue = "World") String name) {
    return "Hola, " + name;
  }
}
```

`src/main/java/lab02/web/App.java`

```java
import lab02.web.server.annotations.MicroSpringbootApp;
import lab02.web.server.core.MicroSpringBoot;

@MicroSpringbootApp
public class App {
  public static void main(String[] args) {
    MicroSpringBoot.run(App.class, 8080);
  }
}
```

## Example (manual routing, optional)
If you prefer explicit routing without annotations, use the low-level `WebServer`:

```java
import lab02.web.server.core.WebServer;

WebServer http = new WebServer(8080, "src/main/resources/static");
http.get("/ping", (req, res) -> {
  res.setStatusCode(200);
  res.setBody("pong");
});
http.start();
```

## Project structure
- `src/main/java/lab02/web/App.java` — App entry point annotated with `@MicroSpringbootApp`.
- `src/main/java/lab02/web/controller/` — Application controllers.
- `src/main/java/lab02/web/server/annotations/` — Annotations.
- `src/main/java/lab02/web/server/http/` — HTTP primitives.
- `src/main/java/lab02/web/server/core/` — Router, server, bootstrap.
- `src/main/resources/static/` — Public assets (e.g., `index.html`).
- `src/test/java/lab02/web/` — Unit tests.

## Notes
- Controllers are discovered by scanning compiled classes under the base package of your `App` class (here: `lab02.web`). Keep controllers under the same root package to be found.
- Static files are served from the filesystem path `src/main/resources/static` relative to the project root.
- Paths are literal (no path params). Use `@QueryParam` for query parameters.

## Credits
MicroSpringBoot was created by Marianella Polo Peña for the TDSE course at Escuela Colombiana de Ingeniería, under Professor Luis Daniel Benavides.

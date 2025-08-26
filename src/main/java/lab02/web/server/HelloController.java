package lab02.web.server;

import lab02.web.server.WebServer.*;

@RestController

public class HelloController {
    @GetMapping("/hello")
    public static String greeting(@QueryParam(value = "name", defaultValue = "World") String name) {
        return "Hi from SpringBoot!";

    }
}

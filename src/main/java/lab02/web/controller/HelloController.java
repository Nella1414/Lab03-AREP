package lab02.web.controller;

import lab02.web.server.annotations.RestController;
import lab02.web.server.annotations.GetMapping;
import lab02.web.server.annotations.QueryParam;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public static String greeting(@QueryParam(value = "name", defaultValue = "World") String name) {
        return "Hi from SpringBoot!";
    }
}

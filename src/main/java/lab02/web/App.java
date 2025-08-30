package lab02.web;

import lab02.web.server.annotations.MicroSpringbootApp;
import lab02.web.server.core.MicroSpringBoot;

@MicroSpringbootApp
public class App {
    public static void main(String[] args) {
        MicroSpringBoot.run(App.class, 8080);
    }
}

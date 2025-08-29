package lab02.web.server.core;

import lab02.web.server.annotations.*;
import lab02.web.server.http.HttpMethod;
import lab02.web.server.http.Request;
import lab02.web.server.http.Response;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MicroSpringBoot {

    public static void run(Class<?> appClass, int port) {
        String basePackage = appClass.getPackage().getName();
        String basePath = basePackage.replace('.', '/');

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(basePath);
        if (url == null) {
            System.out.println("[msb] base package not found: " + basePackage);
            WebServer ws = new WebServer(port, "src/main/resources/static");
            ws.start();
            return;
        }

        File root;
        try {
            String dec = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
            root = new File(dec);
        } catch (Exception e) {
            System.out.println("[msb] failed to resolve base path: " + e.getMessage());
            WebServer ws = new WebServer(port, "src/main/resources/static");
            ws.start();
            return;
        }

        List<Class<?>> controllers = new ArrayList<>();
        for (File f : listClassFiles(root)) {
            String className = toClassName(basePackage, root, f);
            if (className == null || className.contains("$"))
                continue;
            try {
                Class<?> c = Class.forName(className);
                if (c.isAnnotationPresent(RestController.class)) {
                    controllers.add(c);
                }
            } catch (Throwable ignored) {
            }
        }

        WebServer ws = new WebServer(port, "src/main/resources/static");

        for (Class<?> ctrl : controllers) {
            Object instance = instantiate(ctrl);
            for (Method m : ctrl.getDeclaredMethods()) {
                Route route = routeFromMethod(m);
                if (route == null)
                    continue;
                String normPath = normalizePath(route.path);
                Handler h = (req, res) -> invokeHandler(instance, m, req, res);
                ws.register(route.method, normPath, h);
                System.out.println("[msb] route: " + route.method + " " + normPath + " -> " + ctrl.getSimpleName() + "."
                        + m.getName());
            }
        }

        ws.start();
    }

    private static record Route(HttpMethod method, String path) {
    }

    private static Route routeFromMethod(Method m) {
        if (m.isAnnotationPresent(GetMapping.class)) {
            return new Route(HttpMethod.GET, m.getAnnotation(GetMapping.class).value());
        }
        if (m.isAnnotationPresent(PostMapping.class)) {
            return new Route(HttpMethod.POST, m.getAnnotation(PostMapping.class).value());
        }
        if (m.isAnnotationPresent(PutMapping.class)) {
            return new Route(HttpMethod.PUT, m.getAnnotation(PutMapping.class).value());
        }
        if (m.isAnnotationPresent(PatchMapping.class)) {
            return new Route(HttpMethod.PATCH, m.getAnnotation(PatchMapping.class).value());
        }
        if (m.isAnnotationPresent(DeleteMapping.class)) {
            return new Route(HttpMethod.DELETE, m.getAnnotation(DeleteMapping.class).value());
        }
        return null;
    }

    private static String normalizePath(String p) {
        if (p == null || p.isEmpty())
            return "/";
        String out = p.startsWith("/") ? p : "/" + p;
        if (out.length() > 1 && out.endsWith("/"))
            out = out.substring(0, out.length() - 1);
        return out;
    }

    private static Object instantiate(Class<?> c) {
        try {
            return c.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            return null;
        }
    }

    private static void invokeHandler(Object instance, Method m, Request req, Response res) {
        try {
            Object[] args = resolveArgs(m, req, res);
            m.setAccessible(true);
            Object target = java.lang.reflect.Modifier.isStatic(m.getModifiers()) ? null : instance;
            Object result = m.invoke(target, args);
            if (result instanceof String s) {
                res.setStatusCode(200);
                res.setHeader("Content-Type", "text/plain; charset=utf-8");
                res.setBody(s);
            }
        } catch (BadRequest br) {
            res.setStatusCode(400);
            res.setStatusMessage("Bad Request");
            res.setBody(br.getMessage());
        } catch (Throwable t) {
            res.setStatusCode(500);
            res.setStatusMessage("Internal Server Error");
            res.setBody("Handler error");
        }
    }

    private static Object[] resolveArgs(Method m, Request req, Response res) {
        Parameter[] params = m.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> t = p.getType();
            if (t.equals(Request.class)) {
                args[i] = req;
                continue;
            }
            if (t.equals(Response.class)) {
                args[i] = res;
                continue;
            }
            QueryParam qp = p.getAnnotation(QueryParam.class);
            if (qp == null) {
                args[i] = null;
                continue;
            }
            String raw = req.getQueryParams() != null ? req.getQueryParams().get(qp.value()) : null;
            if (raw == null || raw.isEmpty())
                raw = qp.defaultValue();
            args[i] = convert(raw, t);
        }
        return args;
    }

    private static Object convert(String raw, Class<?> t) {
        if (t.equals(String.class))
            return raw;
        try {
            if (t.equals(int.class) || t.equals(Integer.class))
                return raw == null || raw.isEmpty() ? 0 : Integer.parseInt(raw);
            if (t.equals(long.class) || t.equals(Long.class))
                return raw == null || raw.isEmpty() ? 0L : Long.parseLong(raw);
            if (t.equals(double.class) || t.equals(Double.class))
                return raw == null || raw.isEmpty() ? 0.0 : Double.parseDouble(raw);
            if (t.equals(boolean.class) || t.equals(Boolean.class))
                return raw != null && Boolean.parseBoolean(raw);
        } catch (Exception e) {
            throw new BadRequest("Invalid value for type " + t.getSimpleName());
        }
        return null;
    }

    private static List<File> listClassFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null)
            return out;
        for (File f : files) {
            if (f.isDirectory())
                out.addAll(listClassFiles(f));
            else if (f.getName().endsWith(".class"))
                out.add(f);
        }
        return out;
    }

    private static String toClassName(String basePackage, File root, File classFile) {
        try {
            String basePath = root.getCanonicalPath();
            String fullPath = classFile.getCanonicalPath();
            if (!fullPath.startsWith(basePath))
                return null;
            String rel = fullPath.substring(basePath.length() + 1);
            String noExt = rel.substring(0, rel.length() - ".class".length());
            return basePackage + '.' + noExt.replace(File.separatorChar, '.');
        } catch (Exception e) {
            return null;
        }
    }

    private static class BadRequest extends RuntimeException {
        public BadRequest(String msg) {
            super(msg);
        }
    }
}

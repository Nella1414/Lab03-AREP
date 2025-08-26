package lab02.web.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Request {

    private final HttpMethod verb;
    private final String uri;
    private final String httpVer;
    private final HashMap<String, String> query;
    private final HashMap<String, String> hdrs;
    private final HashMap<String, ?> data;

    public Request(HttpMethod method, String path, String version, HashMap<String, String> queryParams,
            HashMap<String, String> headers, HashMap<String, ?> body) {
        this.verb = method;
        this.uri = path;
        this.httpVer = version;
        this.query = queryParams;
        this.hdrs = headers;
        this.data = body;
    }

    public HttpMethod getMethod() {
        return verb;
    }

    public String getPath() {
        return uri;
    }

    public String getVersion() {
        return httpVer;
    }

    public HashMap<String, String> getQueryParams() {
        return query;
    }

    public HashMap<String, String> getHeaders() {
        return hdrs;
    }

    public HashMap<String, ?> getBody() {
        return data;
    }

    public static HashMap<String, String> parseRequestLine(String raw) {
        HashMap<String, String> out = new HashMap<>();
        int firstSpace = raw.indexOf(' ');
        if (firstSpace <= 0)
            return out;
        int secondSpace = raw.indexOf(' ', firstSpace + 1);
        if (secondSpace <= firstSpace)
            return out;

        String method = raw.substring(0, firstSpace).trim();
        String restOfLine = raw.substring(firstSpace + 1, raw.indexOf("\r\n")).trim();

        String path;
        String version;
        int spaceInRest = restOfLine.lastIndexOf(' ');
        if (spaceInRest > 0) {
            path = restOfLine.substring(0, spaceInRest);
            version = restOfLine.substring(spaceInRest + 1);
        } else {
            // Fallback if malformed
            String[] pieces = restOfLine.split(" ", 2);
            path = pieces.length > 0 ? pieces[0] : "/";
            version = pieces.length > 1 ? pieces[1] : "HTTP/1.1";
        }

        out.put("method", method);
        out.put("path", path);
        out.put("version", version);
        return out;
    }

    public static HashMap<String, String> parseQueryParams(String path) {
        HashMap<String, String> map = new HashMap<>();
        try {
            URI u = new URI(path);
            String qs = u.getQuery();
            if (qs == null || qs.isEmpty())
                return map;
            for (String pair : qs.split("&")) {
                if (pair.isEmpty())
                    continue;
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.put(key, val);
            }
        } catch (Exception ignored) {
            // ignore malformed URI
        }
        return map;
    }

    public static HashMap<String, String> parseHeaders(String raw) {
        HashMap<String, String> map = new HashMap<>();
        int split = raw.indexOf("\r\n\r\n");
        String head = split >= 0 ? raw.substring(0, split) : raw;
        String[] lines = head.split("\r\n");
        for (int i = 1; i < lines.length; i++) { // skip request-line
            String line = lines[i];
            if (line.isEmpty())
                break;
            int colon = line.indexOf(':');
            if (colon > 0) {
                String k = line.substring(0, colon).trim();
                String v = line.substring(colon + 1).trim();
                map.put(k, v);
            }
        }
        return map;
    }

    public static String getRawRequestBody(String raw) {
        int split = raw.indexOf("\r\n\r\n");
        return split >= 0 ? raw.substring(split + 4) : "";
    }

    public static HashMap<String, ?> parseJsonBody(String rawBody) {
        if (rawBody == null || rawBody.isEmpty())
            return new HashMap<>();
        char first = rawBody.trim().isEmpty() ? '\0' : rawBody.trim().charAt(0);
        if (first != '{' && first != '[')
            return new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(rawBody, new TypeReference<HashMap<String, ?>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static Request fromString(String raw) {
        HashMap<String, String> start = parseRequestLine(raw);
        HashMap<String, String> headers = parseHeaders(raw);
        HashMap<String, String> params = parseQueryParams(start.get("path"));

        String rawBody = getRawRequestBody(raw);
        HashMap<String, ?> body = parseJsonBody(rawBody);

        return new Request(
                HttpMethod.valueOf(start.get("method").toUpperCase()),
                start.get("path"),
                start.get("version"),
                params,
                headers,
                body);
    }

    public static Request fromBufferedReader(BufferedReader in) throws IOException {
        StringBuilder head = new StringBuilder();
        String line = in.readLine();
        if (line == null)
            line = "";
        head.append(line).append("\r\n");

        int contentLength = 0;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            head.append(line).append("\r\n");
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        head.append("\r\n");

        if (contentLength > 0) {
            char[] buf = new char[contentLength];
            int read = 0;
            while (read < contentLength) {
                int r = in.read(buf, read, contentLength - read);
                if (r == -1)
                    break;
                read += r;
            }
            head.append(buf, 0, read);
        }

        return Request.fromString(head.toString());
    }

    public String toString() {
        String nl = System.lineSeparator();
        return new StringBuilder()
                .append("Request {").append(nl)
                .append("  method:      '").append(verb).append("',").append(nl)
                .append("  path:        '").append(uri).append("',").append(nl)
                .append("  version:     '").append(httpVer).append("',").append(nl)
                .append("  queryParams: ").append(query).append(",").append(nl)
                .append("  headers:     ").append(hdrs).append(",").append(nl)
                .append("  body:        ").append(data).append(nl)
                .append("}")
                .toString();
    }
}

package lab02.web.server;

import java.util.HashMap;

public class Response {
    private int code;
    private String message;
    private HashMap<String, String> headers;
    private byte[] payload;

    public Response() {
        this.code = 200;
        this.message = "OK";
        this.headers = new HashMap<>();
        this.payload = new byte[0];
    }

    public int getStatusCode() {
        return code;
    }

    public String getStatusMessage() {
        return message;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return payload;
    }

    public void setStatusCode(int statusCode) {
        this.code = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.message = statusMessage;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void removeHeader(String name) {
        headers.remove(name);
    }

    public void setBody(String body) {
        this.payload = body == null ? new byte[0] : body.getBytes();
    }

    public void setBody(byte[] body) {
        this.payload = body == null ? new byte[0] : body;
    }
}

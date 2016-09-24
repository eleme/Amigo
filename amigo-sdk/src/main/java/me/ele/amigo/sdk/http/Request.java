package me.ele.amigo.sdk.http;


import java.util.Map;

public class Request {
    private String url;
    private Method method;
    private Map<String, String> headers;
    private Map<String, String> params;
    private byte[] body;
    private String bodyContentType;

    public static Request newRequest(String url) {
        return new Request(url);
    }

    public String url() {
        return url;
    }

    public Method method() {
        return method;
    }

    public Map<String, String> params() {
        return params;
    }

    public byte[] body() {
        return body;
    }

    private Request(String url) {
        this.url = url;
    }

    public Request method(Method method) {
        this.method = method;
        return this;
    }

    public Request params(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public Request body(byte[] body) {
        this.body = body;
        return this;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Request headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public String bodyContentType() {
        return bodyContentType;
    }

    public Request bodyContentType(String bodyContentType) {
        this.bodyContentType = bodyContentType;
        return this;
    }
}

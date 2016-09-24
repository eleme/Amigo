package me.ele.amigo.sdk.http;


public class Response {
    private int code;
    private byte[] body;

    public Response(int code, byte[] body) {
        this.code = code;
        this.body = body;
    }

    public int code() {
        return code;
    }

    public byte[] body() {
        return body;
    }
}

package me.ele.amigo.sdk.http;


public class Error {

    private int code;
    private String msg;
    private String des;

    public Error(int code, String msg, String des) {
        this.code = code;
        this.msg = msg;
        this.des = des;
    }

    public int code() {
        return code;
    }

    public String msg() {
        return msg;
    }

    public String des() {
        return des;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("code: ").append(code).append(", ")
                .append("msg: ").append(msg).append(", ")
                .append("des: ").append(des).append("\n")
                .toString();
    }
}

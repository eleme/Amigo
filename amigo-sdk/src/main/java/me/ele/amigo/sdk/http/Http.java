package me.ele.amigo.sdk.http;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import me.ele.amigo.sdk.AmigoSdk;

import static me.ele.amigo.sdk.utils.CommonUtil.byteArray2String;

public class Http {
    private static final boolean DEBUG = true;

    private static final int TIMEOUT = 30 * 1000;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    // todo
    private static SSLSocketFactory mSslSocketFactory;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static String userAgent;

    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    public static void performRequest(final Request request, final Callback callback) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onStart();
                            }
                        });
                    }

                    Uri.Builder uriBuilder = Uri.parse(request.url()).buildUpon();
                    if (request.method() == null || request.method() == Method.GET) {
                        Map<String, String> params = request.params();
                        if (params != null) {
                            for (Map.Entry<String, String> entry : params.entrySet()) {
                                uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    URL url = new URL(uriBuilder.build().toString());
                    HttpURLConnection connection = openConnection(url);

                    Map<String, String> headers = request.headers();
                    if (headers != null) {
                        for (Map.Entry<String, String> entry : headers.entrySet()) {
                            connection.addRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    setConnectionParameters(connection, request);

                    final int responseCode = connection.getResponseCode();
                    if (responseCode == -1) {
                        // -1 is returned by getResponseCode() if the response code could not be retrieved.
                        // Signal to the caller that something was wrong with the connection.
                        if (callback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFail(new Error(-1, null, null));
                                    callback.onComplete();
                                }
                            });
                        }
                        return;
                    }

                    InputStream is = connection.getInputStream();
                    if (is == null) {
                        if (callback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFail(new Error(responseCode, null, null));
                                    callback.onComplete();
                                }
                            });
                        }
                        return;
                    }

                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4 * 1024];
                    long contentLength = connection.getContentLength();
                    long length = 0;
                    int count;
                    while ((count = is.read(buffer)) != -1) {
                        os.write(buffer, 0, count);
                        length += count;
                        final float percent = (float) (length * 1.0 / contentLength);
                        if (responseCode >= 200 && responseCode <= 299) {
                            if (callback != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onUpdate(percent);
                                    }
                                });
                            }
                        }
                    }

                    if (callback != null) {
                        if (responseCode >= 200 && responseCode <= 299) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSucc(new Response(responseCode, os.toByteArray()));
                                    callback.onComplete();
                                }
                            });
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFail(new Error(responseCode, null, byteArray2String(os.toByteArray())));
                                    callback.onComplete();
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    if (DEBUG) e.printStackTrace();
                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(new Error(-1, null, null));
                                callback.onComplete();
                            }
                        });
                    }
                }
            }
        });
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

    private static void setConnectionParameters(HttpURLConnection connection, Request request) throws IOException {
        // add general headers
        connection.setRequestProperty("User-Agent", getUserAgent());

        Method method = request.method();
        if (method == null || method == Method.GET) {
            connection.setRequestMethod("GET");
            return;
        }
        if (method == Method.POST) {
            connection.setRequestMethod("POST");
            byte[] body = request.body();
            if (body != null) {
                connection.setDoOutput(true);
                connection.addRequestProperty(HEADER_CONTENT_TYPE, TextUtils.isEmpty(request.bodyContentType()) ? "application/json" : request.bodyContentType());
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.write(body);
                out.close();
            }
            return;
        }
    }

    private static String getUserAgent() {
        if (TextUtils.isEmpty(userAgent)) {
            String separator = "/";
            userAgent = new StringBuilder()
                    .append("app_id").append(separator).append(AmigoSdk.appId())
                    .append(" ")
                    .append("device_id").append(separator).append(AmigoSdk.deviceId())
                    .toString();
        }
        return userAgent;
    }

    public interface Callback {
        void onStart();

        void onUpdate(float percent);

        void onSucc(Response response);

        void onFail(Error error);

        void onComplete();
    }

    public static class SimpleCallback implements Callback {
        @Override
        public void onStart() {

        }

        @Override
        public void onUpdate(float percent) {

        }

        @Override
        public void onSucc(Response response) {

        }

        @Override
        public void onFail(Error error) {

        }

        @Override
        public void onComplete() {

        }
    }
}

package ru.romentiy.gacgs8manual;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String APP_HOST = "appassets.local";
    private static final String APP_PREFIX = "/gac-gs8-manual/";
    private static final String START_URL = "https://" + APP_HOST + APP_PREFIX;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(16, 42, 67));
        getWindow().setNavigationBarColor(Color.rgb(16, 42, 67));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(243, 246, 248));
        setContentView(webView);

        configureWebView();
        configureServiceWorker();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return localResponse(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_HOST.equals(uri.getHost())) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (APP_HOST.equals(Uri.parse(url).getHost())) {
                    view.evaluateJavascript(
                            "window.open=function(url){window.location.href=url;return window;};",
                            null
                    );
                }
            }
        });
    }

    private void configureServiceWorker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return localResponse(request.getUrl());
                }
            });
        }
    }

    private WebResourceResponse localResponse(Uri uri) {
        if (!APP_HOST.equals(uri.getHost())) {
            return null;
        }

        String path = Uri.decode(uri.getPath());
        String relativePath;

        if (path == null || path.isEmpty() || "/".equals(path) || APP_PREFIX.equals(path)) {
            relativePath = "index.html";
        } else if (path.startsWith(APP_PREFIX)) {
            relativePath = path.substring(APP_PREFIX.length());
        } else if (path.startsWith("/manual/") || path.startsWith("/assets/")) {
            relativePath = path.substring(1);
        } else {
            return notFound();
        }

        if (relativePath.isEmpty()) {
            relativePath = "index.html";
        }
        if (relativePath.contains("..")) {
            return notFound();
        }

        try {
            InputStream stream = getAssets().open("site/" + relativePath);
            return responseFor(relativePath, stream);
        } catch (IOException ignored) {
            return notFound();
        }
    }

    private WebResourceResponse responseFor(String path, InputStream stream) {
        String mimeType = mimeType(path);
        String encoding = mimeType.startsWith("text/") ||
                mimeType.contains("javascript") ||
                mimeType.contains("json") ||
                mimeType.contains("svg") ? "UTF-8" : null;

        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-cache");
        headers.put("Access-Control-Allow-Origin", "*");

        return new WebResourceResponse(mimeType, encoding, 200, "OK", headers, stream);
    }

    private WebResourceResponse notFound() {
        byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse(
                "text/plain",
                "UTF-8",
                404,
                "Not Found",
                new HashMap<>(),
                new ByteArrayInputStream(body)
        );
    }

    private String mimeType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".js")) return "text/javascript";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".webmanifest")) return "application/manifest+json";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".html")) return "text/html";

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        String detected = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return detected != null ? detected : "application/octet-stream";
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            // Если подходящего внешнего приложения нет, ссылка просто не открывается.
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        String currentUrl = webView.getUrl();
        if (currentUrl != null && currentUrl.contains("#section-")) {
            webView.evaluateJavascript(
                    "document.querySelector('.brand') && document.querySelector('.brand').click();",
                    null
            );
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}

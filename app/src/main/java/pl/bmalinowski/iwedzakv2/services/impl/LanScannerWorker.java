package pl.bmalinowski.iwedzakv2.services.impl;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.LanScannerPort;

class LanScannerWorker implements LanScannerPort {

    private final RequestQueue queue;
    private final Optional<URL> currentIp;
    private final int port;

    public LanScannerWorker(final Context context, final int port) {
        this.queue = Volley.newRequestQueue(context);
        this.currentIp = getIPAddress(context);
        this.port = port;
    }

    @Override
    public Optional<URL> findExternalIP() {
        return currentIp.map(ip -> IntStream.range(100, 130)
                .boxed()
                .map(idx -> ip.replaceSubnetWith(idx, port).toString())
                .map(this::scanLan)
                .collect(Collectors.toList())
        )
                .flatMap(futures -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(URL::parseOrNull)
                );
    }

    private CompletableFuture<String> scanLan(final String url) {
        final CompletableFuture<String> completableFuture = new CompletableFuture<>();
        final StringRequest ipRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (response.contains("Smoking house")) {
                        completableFuture.complete(url);
                    } else {
                        completableFuture.complete(null);
                    }
                },
                error -> {
                    if (Optional.ofNullable(error.getMessage()).orElse("").contains("Smoking house")) {
                        completableFuture.complete(url);
                    } else {
                        completableFuture.complete(null);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                final Map<String, String> map = new HashMap<>();
                map.put("Connection", "close");
                return map;
            }
        };
        ipRequest.setShouldCache(false);
        ipRequest.setRetryPolicy(new DefaultRetryPolicy(
                50,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(ipRequest);
        return completableFuture;
    }

    public Optional<URL> getIPAddress(final Context context) {
        try {
            final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return Optional.ofNullable(URL.parseOrNull("http://" +
                    Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress())));
        } catch (final Exception e) {
            Log.e("System.err", "No internet connection");
            return Optional.empty();
        }
    }
}

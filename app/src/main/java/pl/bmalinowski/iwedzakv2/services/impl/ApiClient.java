package pl.bmalinowski.iwedzakv2.services.impl;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pl.bmalinowski.iwedzakv2.model.URL;

class ApiClient {

    private final RequestQueue queue;

    public ApiClient(final Context context) {
        this.queue = Volley.newRequestQueue(context);

    }

    public Optional<String> getCurrentSmokingHouseState(final URL url) {
        try {
            final CompletableFuture<String> completableFuture = new CompletableFuture<>();
            final StringRequest ipRequest = new StringRequest(Request.Method.GET, url.toString(),
                    completableFuture::complete,
                    error -> completableFuture.complete(error.getMessage())
            );
            ipRequest.setShouldCache(false);
            ipRequest.setRetryPolicy(new DefaultRetryPolicy(
                    200,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            queue.add(ipRequest);
            return Optional.ofNullable(completableFuture.get(10, TimeUnit.SECONDS));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}

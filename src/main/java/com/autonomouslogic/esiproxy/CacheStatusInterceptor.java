package com.autonomouslogic.esiproxy;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class CacheStatusInterceptor implements Interceptor {
	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var req = chain.request();
		final var response = chain.proceed(req);
		final var networkResponse = response.networkResponse();
		final var cacheResponse = response.cacheResponse();
		var cached = cacheResponse != null && networkResponse == null;
		return response.newBuilder()
				.header(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, cached ? "HIT" : "MISS")
				.build();
	}
}

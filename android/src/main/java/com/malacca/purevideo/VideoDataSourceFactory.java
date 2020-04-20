package com.malacca.purevideo;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.JavaNetCookieJar;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.network.CookieJarContainer;
import com.facebook.react.modules.network.OkHttpClientProvider;
import com.facebook.react.modules.network.ForwardingCookieHandler;

import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;

class VideoDataSourceFactory implements DataSource.Factory {
    private final Context context;

    private VideoDataSourceFactory(Context context) {
        this.context = context;
    }

    @Override
    public DataSource createDataSource() {
        return new RawResourceDataSource(context);
    }

    private static DataSource.Factory rawDataSourceFactory = null;
    private static DataSource.Factory httpDataSourceFactory = null;

    static DataSource.Factory getRawDataSourceFactory(ReactContext context) {
        if (rawDataSourceFactory == null) {
            rawDataSourceFactory = new VideoDataSourceFactory(context.getApplicationContext());
        }
        return rawDataSourceFactory;
    }

    static DataSource.Factory getHttpDataSourceFactory(
            ReactContext context,
            DefaultBandwidthMeter bandwidthMeter,
            Map<String, String> requestHeaders
    ) {
        if (httpDataSourceFactory == null) {
            OkHttpClient client = OkHttpClientProvider.getOkHttpClient();
            CookieJarContainer container = (CookieJarContainer) client.cookieJar();
            ForwardingCookieHandler handler = new ForwardingCookieHandler(context);
            container.setCookieJar(new JavaNetCookieJar(handler));
            httpDataSourceFactory = new OkHttpDataSourceFactory(
                    client,
                    Util.getUserAgent(context, "RNPureVideo"),
                    bandwidthMeter
            );
        }
        if (requestHeaders == null) {
            requestHeaders = new HashMap<>();
        }
        ((OkHttpDataSourceFactory) httpDataSourceFactory).getDefaultRequestProperties().set(requestHeaders);
        return httpDataSourceFactory;
    }
}

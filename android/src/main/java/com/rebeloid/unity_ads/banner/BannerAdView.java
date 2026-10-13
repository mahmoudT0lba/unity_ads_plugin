package com.rebeloid.unity_ads.banner;

import android.app.Activity;
import android.view.View;

import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

import com.rebeloid.unity_ads.UnityAdsConstants;

public class BannerAdView implements PlatformView {
    private static final UnityBannerSize STANDARD_SIZE = new UnityBannerSize(320, 50);
    private final BannerView bannerView;

    /**
     * VIBE ZO FORK: guards against a double destroy(). Flutter can call
     * dispose() more than once for the same platform view (hot restart, an
     * engine detach racing a widget teardown), and BannerView.destroy() is not
     * documented as idempotent.
     */
    private boolean destroyed = false;

    public BannerAdView(Activity activity, int id, Map<?, ?> args, BinaryMessenger messenger) {
        MethodChannel channel = new MethodChannel(messenger,
                UnityAdsConstants.BANNER_AD_CHANNEL + "_" + id);

        Integer width = (Integer) args.get(UnityAdsConstants.WIDTH_PARAMETER);
        Integer height = (Integer) args.get(UnityAdsConstants.HEIGHT_PARAMETER);
        UnityBannerSize size = width == null || height == null ? STANDARD_SIZE : new UnityBannerSize(width, height);

        bannerView = new BannerView(activity, (String) args.get(UnityAdsConstants.PLACEMENT_ID_PARAMETER), size);

        bannerView.setListener(new BannerAdListener(channel));
        bannerView.load();
    }

    @Override
    public View getView() {
        return bannerView;
    }

    /**
     * VIBE ZO FORK vs upstream unity_ads_plugin 0.4.0, which ships this method
     * EMPTY.
     *
     * Unity's SDK keeps its own reference to every BannerView it creates and
     * drives that view's auto-refresh cycle from it. Without an explicit
     * destroy() the view survives the Flutter platform-view teardown and keeps
     * refreshing — and therefore keeps firing onBannerShown, i.e. billable
     * impressions — for an ad no longer attached to anything on screen. In an
     * app that mounts banners in carousels and feeds, that leaks one live
     * BannerView per mount for the rest of the process lifetime, wrecks
     * viewability, and is an invalid-traffic exposure.
     *
     * The listener is cleared first so no late callback can reach a
     * MethodChannel whose Dart side is already gone.
     *
     * Upstream issue: the same gap exists on iOS (BannerAdView.swift has no
     * teardown); both are patched in this fork.
     */
    @Override
    public void dispose() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        bannerView.setListener(null);
        bannerView.destroy();
    }
}
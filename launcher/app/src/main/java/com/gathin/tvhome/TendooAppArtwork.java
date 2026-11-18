package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The same artwork lookup strategy as CleanYourTV's AppAdapter:
 * use the actual TV launch activity's *wide* banner, not only the package icon.
 * That distinction is crucial for apps such as Netflix, YouTube and TV players.
 */
final class TendooAppArtwork {
    private final Context context;
    private final PackageManager pm;
    private final Map<String, Drawable.ConstantState> wideBanners = new HashMap<>();
    private final Map<String, Boolean> noBanner = new HashMap<>();

    TendooAppArtwork(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
    }

    Drawable banner(String packageName) {
        Drawable.ConstantState state = wideBanners.get(packageName);
        if (state != null) return state.newDrawable(context.getResources()).mutate();
        if (Boolean.TRUE.equals(noBanner.get(packageName))) return null;

        Drawable image = lookupBanner(packageName);
        if (image == null) {
            noBanner.put(packageName, true);
            return null;
        }
        Drawable.ConstantState constant = image.getConstantState();
        if (constant != null) wideBanners.put(packageName, constant);
        return image;
    }

    private Drawable lookupBanner(String pkg) {
        // 1. Preferred: launchable LEANBACK activity's drawable banner.
        Drawable d = activityBanner(pkg, Intent.CATEGORY_LEANBACK_LAUNCHER);
        if (d != null) return d;

        // 2. The TV launch activity from the package manager.
        try {
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i != null && i.getComponent() != null) {
                d = drawableFromActivity(pm.getActivityInfo(i.getComponent(), 0));
                if (validBanner(d)) return d;
            }
        } catch (Exception ignored) { }

        // 3. Conventional Android app launcher activity, if it has a wide banner.
        d = activityBanner(pkg, Intent.CATEGORY_LAUNCHER);
        if (d != null) return d;

        // 4. Application-level banner only as a last resort.
        try {
            d = pm.getApplicationBanner(pkg);
            if (validBanner(d)) return d;
        } catch (Exception ignored) { }
        return null;
    }

    private Drawable activityBanner(String pkg, String category) {
        try {
            Intent filter = new Intent(Intent.ACTION_MAIN);
            filter.addCategory(category);
            filter.setPackage(pkg);
            List<ResolveInfo> activities = pm.queryIntentActivities(filter, 0);
            for (ResolveInfo info : activities) {
                if (info.activityInfo == null) continue;
                Drawable banner = drawableFromActivity(info.activityInfo);
                if (validBanner(banner)) return banner;
                banner = info.loadBanner(pm);
                if (validBanner(banner)) return banner;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private Drawable drawableFromActivity(ActivityInfo info) {
        if (info == null) return null;
        int res = info.banner;
        if (res == 0 && info.applicationInfo != null) res = info.applicationInfo.banner;
        if (res == 0) return null;
        try {
            Resources appResources = pm.getResourcesForApplication(info.applicationInfo);
            int dpi = Math.max(DisplayMetrics.DENSITY_XHIGH,
                    context.getResources().getDisplayMetrics().densityDpi);
            return appResources.getDrawableForDensity(res, dpi, null);
        } catch (Exception ignored) { return null; }
    }

    private static boolean validBanner(Drawable d) {
        if (d == null) return false;
        int w = d.getIntrinsicWidth(), h = d.getIntrinsicHeight();
        // An icon pretending to be a banner looks distorted on a 16:9 card.
        return w <= 0 || h <= 0 || w >= 1.4f * h;
    }
}

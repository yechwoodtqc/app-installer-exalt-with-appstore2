package com.lge.lginstallservie;

/** Set this to the deployed HTTPS Cloudflare Worker URL before releasing builds. */
public final class ApiConfig {
    private ApiConfig() {}
    public static final String BASE_URL = "https://REPLACE-WITH-YOUR-WORKER.workers.dev";
}

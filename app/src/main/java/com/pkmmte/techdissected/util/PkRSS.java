package com.pkmmte.techdissected.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.pkmmte.techdissected.model.Article;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okio.Timeout;

public class PkRSS {
	// Static constants
	public static final String KEY_ARTICLE = "ARTICLE";
	public static final String KEY_ARTICLE_ID = "ARTICLE ID";
	public static final String KEY_ARTICLE_URL = "ARTICLE URL";
	public static final String KEY_FEED_URL = "FEED URL";
	public static final String KEY_CATEGORY_NAME = "CATEGORY NAME";
	public static final String KEY_CATEGORY = "CATEGORY";
	public static final String KEY_SEARCH = "SEARCH TERM";

	// Global singleton instance
	private static PkRSS singleton = null;

	// For issue tracking purposes
	private volatile boolean loggingEnabled;
	protected static final String TAG = "PkRSS";

	// Context is always useful for some reason.
	private final Context mContext;

	// Our handy client for getting XML feed data
	private final OkHttpClient httpClient = new OkHttpClient();
	private final String httpCacheDir = "/okhttp";
	private final int httpCacheSize = 1024 * 1024;
	private final int httpCacheMaxAge = 60 * 60;
	private final long httpConnectTimeout = 15;
	private final long httpReadTimeout = 30;

	// Reusable XML Parser
	private RSSParser rssParser = new RSSParser(this);

	// List of stored articles
	private Map<String, List<Article>> articleMap = new HashMap<String, List<Article>>();

	// Keep track of pages already loaded on specific feeds
	private Map<String, Integer> pageTracker = new HashMap<String, Integer>();

	public static PkRSS with(Context context) {
		if(singleton == null)
			singleton = new PkRSS(context.getApplicationContext());
		return singleton;
	}

	protected PkRSS(Context context) {
		this.mContext = context;
		try {
			File cacheDir = new File(context.getCacheDir().getAbsolutePath() + httpCacheDir);
			this.httpClient.setCache(new Cache(cacheDir, httpCacheSize));
			this.httpClient.setConnectTimeout(httpConnectTimeout, TimeUnit.SECONDS);
			this.httpClient.setReadTimeout(httpReadTimeout, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			Log.e(TAG, "Error configuring OkHttp client! \n" + e.getMessage());
		}
	}

	public void setLoggingEnabled(boolean enabled) {
		loggingEnabled = enabled;
	}

	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	public RequestCreator load(String url) {
		return new RequestCreator(this, url);
	}

	protected void load(String url, String search, boolean individual, boolean skipCache, int page, Callback callback) {
		// Can't load empty URLs, do nothing
		if(url == null || url.isEmpty()) {
			log("Invalid URL!", Log.ERROR);
			return;
		}

		// Start tracking load time
		long time = System.currentTimeMillis();

		if(individual) // Append feed URL if individual article
			url += "feed/?withoutcomments=1";
		else if(search != null) // Append search query if available and not individual
			url += "?s=" + Uri.encode(search);

		// Create a copy for pagination & handle cache
		String requestUrl = url;
		int maxCacheAge = skipCache ? 0 : httpCacheMaxAge;

		//
		pageTracker.put(requestUrl, page);
		if(page > 1 && !individual)
			requestUrl += (search == null ? "?paged=" : "&paged=") + String.valueOf(page);

		Request request = new Request.Builder()
			.addHeader("Cache-Control", "public, max-age=" + maxCacheAge)
			.url(requestUrl)
			.build();

		String xmlString = null;

		try {
			log("Making a request to " + requestUrl + (skipCache ? " [SKIP-CACHE]" : " [MAX-AGE " + maxCacheAge + "]"));
			Response response = httpClient.newCall(request).execute();

			if(response.cacheResponse() != null)
				log("Response retrieved from cache");

			xmlString = response.body().string();
			log("Request took " + (System.currentTimeMillis() - time) + "ms");
		}
		catch (Exception e) {
			log("Error executing/reading http request!", Log.ERROR);
			e.printStackTrace();
		}

		InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes());

		List<Article> newArticles = rssParser.parse(inputStream);
		insert(url, newArticles);

		if(callback != null)
			callback.postParse(newArticles);
	}

	public Map<String, List<Article>> get() {
		return articleMap;
	}

	public List<Article> get(String url) {
		return articleMap.get(url);
	}

	public List<Article> get(String url, String search) {
		if(search == null)
			return articleMap.get(url);

		return articleMap.get(url + "?s=" + Uri.encode(search));
	}

	public Article get(int id) {
		long time = System.currentTimeMillis();

		for(List<Article> articleList : articleMap.values()) {
			for(Article article : articleList) {
				if(article.getId() == id) {
					log("get(" + id + ") took " + (System.currentTimeMillis() - time) + "ms");
					return article;
				}
			}
		}

		log("Could not find Article with id of " + id, Log.WARN);
		log("get(" + id + ") took " + (System.currentTimeMillis() - time) + "ms");
		return null;
	}

	protected Map<String, Integer> getPageTracker() {
		return pageTracker;
	}

	private void insert(String url, List<Article> newArticles) {
		if(!articleMap.containsKey(url))
			articleMap.put(url, new ArrayList<Article>());

		List<Article> articleList = articleMap.get(url);
		articleList.addAll(newArticles);

		log("New size for " + url + " is " + articleList.size());
	}

	protected void log(String message) {
		log(TAG, message, Log.DEBUG);
	}

	protected void log(String tag, String message) {
		log(tag, message, Log.DEBUG);
	}

	protected void log(String message, int type) {
		log(TAG, message, type);
	}

	protected void log(String tag, String message, int type) {
		if(!loggingEnabled)
			return;

		switch(type) {
			case Log.VERBOSE:
				Log.v(tag, message);
				break;
			case Log.DEBUG:
				Log.d(tag, message);
				break;
			case Log.INFO:
				Log.i(tag, message);
				break;
			case Log.WARN:
				Log.w(tag, message);
				break;
			case Log.ERROR:
				Log.e(tag, message);
				break;
			case Log.ASSERT:
			//	break;
			default:
				Log.wtf(tag, message);
				break;
		}
	}

	public interface Callback {
		public void postParse(List<Article> articleList);
	}
}
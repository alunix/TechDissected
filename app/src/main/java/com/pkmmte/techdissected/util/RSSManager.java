package com.pkmmte.techdissected.util;

import android.content.Context;
import android.util.Log;
import com.pkmmte.techdissected.model.Article;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSSManager {
	// Global singleton instance
	private static RSSManager rssManager = null;

	// For issue tracking purposes
	private volatile boolean loggingEnabled;
	protected static final String TAG = "RSSManager";

	// Context is always useful for some reason.
	private final Context mContext;

	// Our handy client for getting XML feed data
	private final OkHttpClient httpClient = new OkHttpClient();
	private final int httpCacheSize = 1024 * 1024;

	// Reusable XML Parser
	private RSSParser rssParser = new RSSParser(this);

	// List of stored articles
	private Map<String, List<Article>> articleMap = new HashMap<String, List<Article>>();

	// Keep track of pages already loaded on specific feeds
	private Map<String, Integer> pageTracker = new HashMap<String, Integer>();

	//
	private Callback mCallback;

	public static  RSSManager with(Context context) {
		if(rssManager == null)
			rssManager = new RSSManager(context.getApplicationContext());
		return rssManager;
	}

	protected RSSManager(Context context) {
		this.mContext = context;
		try {
			this.httpClient.setCache(new Cache(context.getCacheDir(), httpCacheSize));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
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

	protected void load(String url, String search, int page) {
		if(search != null)
			url += "?s=" + search;
		String requestUrl = url;

		//
		pageTracker.put(requestUrl, page);
		if(page > 1)
			requestUrl += "?paged=" + String.valueOf(page);

		Request request = new Request.Builder()
			.url(requestUrl)
			.build();

		String xmlString = null;

		try {
			// 1. get a http response
			Response response = httpClient.newCall(request).execute();

			// 2. construct a string from the response
			xmlString = response.body().string();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// 3. construct an InputSource from the string
		InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes());

		// 4. start parsing with SAXParser and handler object
		// ( both must have been created before )
		List<Article> newArticles = rssParser.parse(inputStream);
		insert(url, newArticles);

		if(mCallback != null)
			mCallback.postParse(newArticles);
	}

	public Map<String, List<Article>> get() {
		return articleMap;
	}

	public List<Article> get(String url) {
		return articleMap.get(url);
	}

	public Article get(int id) {
		long time = System.currentTimeMillis();

		for(List<Article> articleList : articleMap.values()) {
			for(Article article : articleList) {
				if(article.getId() == id) {
					if(loggingEnabled)
						Log.d(TAG, "get(" + id + ") took " + (System.currentTimeMillis() - time) + "ms");
					return article;
				}
			}
		}

		if(loggingEnabled)
			Log.d(TAG, "get(" + id + ") took " + (System.currentTimeMillis() - time) + "ms");

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

		if(isLoggingEnabled())
			Log.d(TAG, "New size for " + url + " is " + articleList.size());
	}

	public interface Callback {
		public void postParse(List<Article> articleList);
	}
}
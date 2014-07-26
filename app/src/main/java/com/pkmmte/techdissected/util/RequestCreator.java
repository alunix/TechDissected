package com.pkmmte.techdissected.util;

import android.net.Uri;
import android.os.AsyncTask;
import com.pkmmte.techdissected.model.Article;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.Map;

// TODO Assign an id to each request (could be by AtomicLong.getInstance()) and
// TODO ... handle them in threads appropriately.
// TODO Also, make a builder for the RSSManager to set configuration such as parallel/serial threading.
public class RequestCreator {
	private final RSSManager rssManager;
	private final Request.Builder data;

	protected RequestCreator(RSSManager rssManager, String url) {
		this.rssManager = rssManager;
		this.data = new Request.Builder(url);
	}

	public RequestCreator search(String search) {
		this.data.search(search);
		return this;
	}

	public RequestCreator page(int page) {
		this.data.page(page);
		return this;
	}

	public RequestCreator nextPage() {
		Request request = data.build();
		String url = request.getUrl();
		int page = request.getPage();

		if(request.getSearchTerm() != null)
			url += "?s=" + request.getSearchTerm();

		Map<String, Integer> pageTracker = rssManager.getPageTracker();
		if(pageTracker.containsKey(url))
			page = pageTracker.get(url);

		this.data.page(page + 1);
		return this;
	}

	public RequestCreator callback(RSSManager.Callback callback) {
		this.data.callback(callback);
		return this;
	}

	public List<Article> get() {
		final Request request = data.build();
		rssManager.load(request.getUrl(), request.getSearchTerm(), request.getPage());
		return rssManager.get(request.getUrl());
	}

	public void async() {
		final Request request = data.build();
		rssManager.setCallback(request.getCallback());

		// TODO Create thread handler class to keep track of all these
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				rssManager.load(request.getUrl(), request.getSearchTerm(), request.getPage());
				return null;
			}
		}.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}
}
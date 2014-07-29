package com.pkmmte.techdissected.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import com.pkmmte.techdissected.R;
import com.pkmmte.techdissected.adapter.ArticlePagerAdapter;
import com.pkmmte.techdissected.fragment.ArticleFragment;
import com.pkmmte.techdissected.model.Article;
import com.pkmmte.techdissected.util.Constants;
import com.pkmmte.techdissected.util.RSSManager;
import java.util.List;

public class ArticleActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_article);


	    getActionBar().setTitle(getIntent().getStringExtra(Constants.KEY_CATEGORY_NAME));
	    Article currentArticle = null;
	    List<Article> articleList = RSSManager.with(this).get(getIntent().getStringExtra(Constants.KEY_FEED_URL));

	    int id = getIntent().getIntExtra(Constants.KEY_ARTICLE_ID, -1);
	    for(Article article : articleList) {
			if(article.getId() == id) {
				currentArticle = article;
				break;
			}
	    }

	    FragmentManager fragmentManager = getSupportFragmentManager();
	    fragmentManager.beginTransaction().replace(R.id.articleContent, ArticleFragment.newInstance(currentArticle)).commit();
    }
}

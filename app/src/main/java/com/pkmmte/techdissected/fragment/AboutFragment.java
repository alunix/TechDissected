package com.pkmmte.techdissected.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.pkmmte.techdissected.R;
import com.pkmmte.techdissected.activity.MainActivity;
import com.pkmmte.techdissected.adapter.AuthorAdapter;
import com.pkmmte.techdissected.util.Constants;
import com.pkmmte.techdissected.view.HeaderGridView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.picasso.Picasso;

public class AboutFragment extends Fragment {
	// Views
	private HeaderGridView mGrid;
	private ImageView imgFamily;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_about, container, false);
		initViews(view);
		adjustPadding();
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		mGrid.setAdapter(new AuthorAdapter(getActivity(), Constants.AUTHORS));
		Picasso.with(getActivity()).load("http://techdissected.com/wp-content/uploads/2014/02/family.png").into(imgFamily);

	}

	private void initViews(View v) {
		mGrid = (HeaderGridView) v.findViewById(R.id.mGrid);

		// Add header
		View headerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.fragment_about_header, mGrid, false);
		mGrid.addHeaderView(headerView, null, false);

		// Extra views
		imgFamily = (ImageView) headerView.findViewById(R.id.imgFamily);
	}

	/**
	 * Adjust bottom GridView padding if System Bar
	 * Tint is set and not null.
	 */
	private void adjustPadding()
	{
		try {
			SystemBarTintManager mTintManager = ((MainActivity) getActivity()).getTintManager();
			if(mTintManager != null) {
				int bottomPadding = mTintManager.getConfig().getPixelInsetBottom();
				mGrid.setPadding(mGrid.getPaddingLeft(), mGrid.getPaddingTop(), mGrid.getPaddingRight(), bottomPadding + mGrid.getPaddingBottom());
			}
		}
		catch(Exception e) { }
	}
}
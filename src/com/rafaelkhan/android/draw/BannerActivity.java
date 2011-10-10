package com.rafaelkhan.android.draw;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class BannerActivity extends Activity {
	private AdView adView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Create the adView
		adView = new AdView(this, AdSize.BANNER,  "");
		adView.setGravity(Gravity.CENTER);

		LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

		// Add the adView to it
		layout.addView(adView);

		// Initiate a generic request to load it with an ad
		adView.loadAd(new AdRequest());
	}

	@Override
	public void onDestroy() {
		adView.destroy();
		super.onDestroy();
	}
}
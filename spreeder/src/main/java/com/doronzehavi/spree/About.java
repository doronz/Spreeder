package com.doronzehavi.spree;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class About extends Activity {

	private ImageButton mSButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		TextView link = (TextView) findViewById(R.id.how_to_text);
		String linkText = "This app was created to help you speed read more easily! To start copy some text (ex: <a href='http://news.google.com/'>Google News</a> article) and hit the '+' button. Then hit the paste button, and done. When you\'re ready to begin hit Spreed!";
		link.setText(Html.fromHtml(linkText));
		link.setMovementMethod(LinkMovementMethod.getInstance());
		
		mSButton = (ImageButton) findViewById(R.id.S_logo);
		mSButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));				
			}
		});
		
	}
	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.about, menu); return true; }
	 */

}

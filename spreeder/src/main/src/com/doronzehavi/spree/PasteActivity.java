package com.doronzehavi.spree;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.gravity.goose.Article;
import com.gravity.goose.Configuration;
import com.gravity.goose.Goose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

public class PasteActivity extends Activity {
	public static LongOperation LongOperationTask;

	public static final String KEY_PASTE = "paste";
	public static final String KEY_TEXT = "text";
	public static final String TAG = "SPREE";
	private static final String UNSUPPORTED_WEBSITE = "BAD WEBSITE";
	private static final String WEBSITE_ERROR = "An error has occured and your email has not been sent.";
	private static final int FILE_SELECT_CODE = 0;

	public static final String PREF_FIRST_EBOOK = "first_ebook_pref";

	private EditText mPasteField;
	private Button mDoneButton;
	private static Book book;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_paste);
		Log.i("SPREE", "onCreated");

		mPasteField = (EditText) findViewById(R.id.pasteField);
		mDoneButton = (Button) findViewById(R.id.doneButton);
		mDoneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.i(TAG, "EnteredOnClick");
				Intent intent = getIntent();
				if (Intent.ACTION_SEND.equals(intent.getAction())) {
					final Intent returnToMain = new Intent(
							getApplicationContext(), MainActivity.class);
					returnToMain.putExtra(KEY_PASTE, mPasteField.getText()
							.toString());
					returnToMain.setAction(Intent.ACTION_DEFAULT);
					Log.e(TAG, "RETURNS TO MAIN");
					startActivity(returnToMain);
					finish();
				} else {
					Intent data = new Intent();
					data.putExtra(KEY_PASTE, mPasteField.getText().toString());
					setResult(RESULT_OK, data);
					finish();
				}
			}
		});
		Bundle received = getIntent().getExtras();
		String receievedString = received.getString(MainActivity.KEY_TO_READ);
		mPasteField.setText(receievedString);
		if (received.getParcelable("Book") != null)
			book = (MyBook) received.getParcelable("Book");
		if (savedInstanceState != null) {
			// maintains typed text
			mPasteField.setText(savedInstanceState.getString(KEY_TEXT));
		}

	}

	void textShared() {
		Log.i("SREE", "textShared()");
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (intent != null && type != null && Intent.ACTION_SEND.equals(action)) {
			if ("text/plain".equals(type)) {
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (sharedText != null
						&& (sharedText.contains("http://") || sharedText
								.contains("www."))) {
					try {
						LongOperationTask = new LongOperation();
						LongOperationTask.execute(this.getApplicationContext()
								.getFilesDir().getAbsolutePath(), sharedText,
								intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else
					handleSendText(sharedText);
			}
		}
	}

	void handleSendText(String webText) {
		if (webText.equals(UNSUPPORTED_WEBSITE)
				|| webText.contains(WEBSITE_ERROR)) {
			Log.i(TAG, "bad website...");
			Toast.makeText(
					this,
					"Couldn't get text, try copy/pasting or a different website.",
					Toast.LENGTH_LONG).show();
		} else {
			mPasteField.setText(webText);
			book = null;
		}
	}

	private ArrayList<String> pullLinks(String text) {
		ArrayList<String> links = new ArrayList<String>();

		String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		while (m.find()) {
			String urlStr = m.group();
			if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
				urlStr = urlStr.substring(1, urlStr.length() - 1);
			}
			links.add(urlStr);
		}
		return links;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState
				.putString(KEY_TEXT, mPasteField.getText().toString());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.paste, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.clearText:
			mPasteField.setText("");
			book = null;
			return true;
		case R.id.pasteText:
			getPaste();
			return true;
		case R.id.addFile:
			ebookWarning();
			return true;
		case R.id.changeChapter:
			showChapterPicker(book);
			return true;
		default:
			return true;
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		try {
			book = readBook();
			Log.i(TAG, "reading book from file successful");
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		textShared();
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			saveBook();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (LongOperationTask != null
				&& LongOperationTask.getStatus() == AsyncTask.Status.RUNNING)
			LongOperationTask.cancel(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FILE_SELECT_CODE:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				Log.d(TAG, "File Uri: " + uri.toString());
				// Get the path
				String path = getPath(this, uri);
				if (path.contains(".epub")) {
					book = getBook(data);
					if (book == null)
						Toast.makeText(this,
								"Error: Try a different file.",
								Toast.LENGTH_LONG).show();
					else
						showChapterPicker(book);
					break;
				}
				else if (path.contains(".txt")) {
					try {
						FileInputStream inputStream;
						inputStream = new FileInputStream(path);
						String txtFile = IOUtils
								.toString(inputStream, null);
						inputStream.close();
						if (txtFile != null)
							mPasteField.setText(txtFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;

				} else {
					Toast.makeText(this, "Error: Unsupported file type, choose a different file.",
							Toast.LENGTH_LONG).show();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private Book getBook(Intent data) {
		book = null;
		Uri uri = data.getData();
		Log.d(TAG, "File Uri: " + uri.toString());
		// Get the path
		String path = getPath(this, uri);
		if (!path.contains(".epub"))
			return book;
		// Get the file instance
		File file = new File(path);
		String fileString = file.getAbsolutePath();
		Log.e(TAG, "file: " + fileString);
		try {
			InputStream epubInputStream = new FileInputStream(file);
			book = (new EpubReader()).readEpub(epubInputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			saveBook();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "book not saved to file!");
		}
		return book;
	}

	private String getChapter(Book book, int chapter) {
		String line, line1 = "", finalstr = "";
		try {
			List<TOCReference> tocReferences = book.getTableOfContents()
					.getTocReferences();
			InputStream is = tocReferences.get(chapter).getResource()
					.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));

			while ((line = r.readLine()) != null) {
				line1 = line1.concat(Html.fromHtml(line).toString());
			}
			finalstr = finalstr.concat("\n").concat(line1);
			finalstr = finalstr.substring(finalstr.indexOf('\n') + 1);
			finalstr = finalstr.substring(finalstr.indexOf('\n') + 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return finalstr;
	}

	private void showChapterPicker(final Book book) {
		AlertDialog.Builder alert = new AlertDialog.Builder(PasteActivity.this);
		List<TOCReference> TOC = book.getTableOfContents().getTocReferences();
		int maxVal = TOC.size() - 1;
		if (maxVal <= 0) {
			Toast.makeText(this, "Ebook must have chapters.", Toast.LENGTH_LONG)
					.show();
			return;
		}
		String[] chapterTitles = new String[TOC.size()];
		for (int i = 0; i < TOC.size(); i++) {
			chapterTitles[i] = TOC.get(i).getTitle();
			Log.i(TAG, "Chapter: " + chapterTitles[i]);
		}

		alert.setTitle("Select a chapter");
		final NumberPicker np = new NumberPicker(PasteActivity.this);
		np.setMinValue(0);
		np.setMaxValue(maxVal);
		np.setWrapSelectorWheel(false);
		np.setValue(0);
		if (chapterTitles[maxVal] != null)
			np.setDisplayedValues(chapterTitles);
		alert.setView(np);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do something with value!
				mPasteField.setText(getChapter(book, np.getValue()));
			}
		});
		String encoding = Constants.CHARACTER_ENCODING;
		Log.e(TAG, "encoding:" + encoding);
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancel.
					}
				});
		alert.show();
	}

	void saveBook() throws IOException {
		if (book != null) {
			EpubWriter writer = new EpubWriter();
			FileOutputStream outputStream = openFileOutput("book.dat",
					Context.MODE_PRIVATE);
			writer.write(book, outputStream);
			outputStream.close();
		}
	}

	private Book readBook() throws IOException {
		EpubReader reader = new EpubReader();
		FileInputStream file = new FileInputStream("book.dat");
		book = reader.readEpub(file);
		return book;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem changeChapter = menu.findItem(R.id.changeChapter);
		if (book != null && book.getTitle().length() > 1) {
			changeChapter.setVisible(true);
		} else {
			changeChapter.setVisible(false);
		}
		return true;
	}

	private void getPaste() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		if (clipboard.hasPrimaryClip()) {
			@SuppressWarnings("deprecation")
			String pasteText = clipboard.getText().toString();
			mPasteField.setText(pasteText);
			book = null;
		} else
			Toast.makeText(this, "Nothing to paste.", Toast.LENGTH_SHORT)
					.show();
	}

	private void showFileChooser() {
		Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
		chooserIntent.setType("*/*");
		chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(Intent.createChooser(chooserIntent,
					"Select a File to Read"), FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}

	}

	// /////////////// FILE CHOOSER \\\\\\\\\\\\\\\\\\\\\\\\\\\

	/**
	 * Get a file path from a Uri. This will get the the path for Storage Access
	 * Framework Documents, as well as the _data field for the MediaStore and
	 * other file-based ContentProviders.
	 * 
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @author paulburke
	 */
	@SuppressLint("NewApi")
	public static String getPath(final Context context, final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/"
							+ split[1];
				}

			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 * 
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @param selection
	 *            (Optional) Filter used in the query.
	 * @param selectionArgs
	 *            (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri,
			String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri
				.getAuthority());
	}

	// //// ASYNC TASK
	// / Problems: Dialog not dismissing it self.
	// First run, bad website, second run fine.
	private class LongOperation extends AsyncTask<Object, Void, String> {
		ProgressDialog dialog;

		@Override
		protected String doInBackground(Object... params) {

			try {
				Log.e(TAG, "entered...");
				ArrayList<String> links;
				links = pullLinks((String) params[1]);
				Configuration config = new Configuration();
				config.setLocalStoragePath((String) params[0]);
				// Get desktop versions only
				config.setBrowserUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/22.0.1229.94 Safari/537.4");
				Goose goose = new Goose(config);
				Article article = goose.extractContent(links.get(0));
				return article.cleanedArticleText();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}

		@Override
		protected void onPostExecute(String webText) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			Log.i(TAG, "EXECUTED");
			if (webText.equals(""))
				handleSendText(UNSUPPORTED_WEBSITE);
			else
				handleSendText(webText);
			dialog.dismiss();
			super.onPostExecute(webText);
		}

		@Override
		protected void onPreExecute() {
			lockOrientation();
			dialog = ProgressDialog.show(PasteActivity.this, "Add to Spree",
					"Getting article from website, please wait...", true, true,
					new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							dialog.dismiss();
							LongOperationTask.cancel(true);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
						}
					});
			dialog.setCancelable(true);
		}

		@Override
		protected void onCancelled() {
			Toast.makeText(PasteActivity.this, "Adding cancelled.",
					Toast.LENGTH_SHORT).show();

		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	protected void lockOrientation() {
		switch (getResources().getConfiguration().orientation) {
		case android.content.res.Configuration.ORIENTATION_PORTRAIT:
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.FROYO) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
				int rotation = getWindowManager().getDefaultDisplay()
						.getRotation();
				if (rotation == android.view.Surface.ROTATION_90
						|| rotation == android.view.Surface.ROTATION_180) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
				} else {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			}
			break;

		case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.FROYO) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				int rotation = getWindowManager().getDefaultDisplay()
						.getRotation();
				if (rotation == android.view.Surface.ROTATION_0
						|| rotation == android.view.Surface.ROTATION_90) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
				}
			}
			break;
		}
	}

	private void ebookWarning() {
		boolean firstrun = getSharedPreferences(PREF_FIRST_EBOOK, MODE_PRIVATE)
				.getBoolean("firstebook", true);
		if (firstrun) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						// Yes button clicked
						getSharedPreferences(PREF_FIRST_EBOOK, MODE_PRIVATE)
								.edit().putBoolean("firstebook", false)
								.commit();
						showFileChooser();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						getSharedPreferences(PREF_FIRST_EBOOK, MODE_PRIVATE)
								.edit().putBoolean("firstebook", true).commit();
						break;
					}
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Add a file?");
			builder.setMessage(
					"Files supported are .epub and .txt, currently. Epubs must have chapters, and proper encoding.")
					.setPositiveButton("Got It", dialogClickListener)
					.setNegativeButton("Nevermind", dialogClickListener).show();
		} else
			showFileChooser();

	}
}

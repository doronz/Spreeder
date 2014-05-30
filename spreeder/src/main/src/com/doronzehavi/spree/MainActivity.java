package com.doronzehavi.spree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PorterDuff.Mode;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
		OnSharedPreferenceChangeListener {

	// Key-Value Pairs
	public static final String KEY_IS_PLAYING = "is_playing";
	public static final String KEY_INDEX = "index";
	public static final String KEY_TO_READ = "toread";
	public static final String KEY_WORD_COUNT = "word_count";
	public static final String KEY_FONT_SIZE = "pref_fontSize";
	public static final String KEY_TEXT_ALIGNMENT = "pref_textAlignment";
	public static final String KEY_CHUNK_SIZE = "pref_textChunk";
	public static final String KEY_PLAY_PRESSED = "play_pressed";
	public static final String KEY_IS_DONE = "key_is_done";
	public static final String WPM_PREF = "wpm_pref";
	public static final String PREF_FIRST_RUN = "first_run_pref";
	public static final String PREF_FIRST_REWIND = "first_rewind_pref";

	// Constants
	public static final String TAG = "SPREE";
	private static final int DEFAULT_WPM = 250;
	private static final int LONG_WORD = 7;
	private static final double PAUSE_PERIOD = 2.0;
	private static final double PAUSE_COMMA = 1.5;

	// Font Size
	public int LARGE = 52;
	public int MEDIUM = 40;
	public int SMALL = 28;

	// Text Alignment
	public int CENTER = Gravity.CENTER_HORIZONTAL;
	public int LEFT = Gravity.LEFT;
	public int RIGHT = Gravity.RIGHT;

	// Variables
	private static String toRead = "Welcome to Spree! Press the '+' button to add your reading material!";

	private static MyBook book = null;

	public ArrayList<Sentence> sentences = new ArrayList<Sentence>();
	ArrayList<Word> words = new ArrayList<Word>();

	private int mWPM = DEFAULT_WPM;
	private int i = 0; // keeps track of what word to display

	public static TextView mReadView;
	public TextView mWPMView;
	public TextView mWordCount;
	public TextView mTimeLeft;

	private Button mPlayButton;
	private ImageButton mRewindButton;
	private SeekBar mSeekBar;

	public RelativeLayout mBackground;

	private boolean mIsPlaying = false;
	private boolean mIsDone = false;
	private boolean mActivityResulted = false;
	private boolean mPlayPressed = false;
	private boolean mUiShowing = true;

	protected ProgressBar mProgressBar;

	private final Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.app_name);
		setContentView(R.layout.activity_main);
		firstRunDialog();
		Log.i(TAG, "onCreate");
		// Get layout element references
		mReadView = (TextView) findViewById(R.id.readView);
		mWPMView = (TextView) findViewById(R.id.wpm);
		mWordCount = (TextView) findViewById(R.id.wordCount);
		mTimeLeft = (TextView) findViewById(R.id.timeLeft);
		mPlayButton = (Button) findViewById(R.id.play_button);
		mRewindButton = (ImageButton) findViewById(R.id.rewind_button);
		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		mBackground = (RelativeLayout) findViewById(R.id.background);

		// Initializing values
		if (i == 0) {
			updateWords();
		}
		initPlayButton();
		initSeekWords();
		initSeekBar();
		initRewindButton();
		mWordCount.setText(words.size() + " words");
		updateTimeLeftView();
		getSavedWPM();
		if (savedInstanceState != null) {
			mIsPlaying = savedInstanceState.getBoolean(KEY_IS_PLAYING);
			i = savedInstanceState.getInt(KEY_INDEX);
			if (i != 0)
				i -= 1;
			toRead = savedInstanceState.getString(KEY_TO_READ);
			mPlayPressed = savedInstanceState.getBoolean(KEY_PLAY_PRESSED);
			mIsDone = savedInstanceState.getBoolean(KEY_IS_DONE);
			updateTimeLeftView();
			if (words.size() >= 0) {
				if (i >= words.size()) {
					mReadView.setText(words.get(words.size() - 1).getData());
				} else if (i > 0) {
					mReadView.setText(words.get(i - 1).getData());
				}
				if (i == 0) {
					mReadView.setText(words.get(0).getData());
				}
				if (mPlayPressed) {
					updateWPMView();
				}
			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		if (mIsDone) {
			mPlayButton.setText(R.string.reset);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");

		if (getIntent() != null
				&& !getIntent().getAction().equals(Intent.ACTION_DEFAULT)) {
			if (!mActivityResulted) {
				try {
					toRead = readFromFile();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			mActivityResulted = false;
		} else {
			String temp = getIntent().getStringExtra("paste");
			if (temp != null && toRead != temp) {
				toRead = temp;
				i = 0;
			}
		}
		updateWords();
		updateBackground();
		setFontSize();
		setTextAlignment();
		updateTimeLeftView();
		updateReadView();
		updateWordCountView();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		pause();
		writeToFile();
		saveWPM();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean(KEY_IS_PLAYING, mIsPlaying);
		savedInstanceState.putInt(KEY_INDEX, i);
		savedInstanceState.putString(KEY_TO_READ, toRead);
		savedInstanceState.putString(KEY_WORD_COUNT, words.size() + " words");
		savedInstanceState.putBoolean(KEY_PLAY_PRESSED, mPlayPressed);
		savedInstanceState.putBoolean(KEY_IS_DONE, mIsDone);
	}

	// Helper methods

	/*
	 * void saveWPM() 
	 * PRE: none 
	 * POST: Saves the user's selected WPM that is
	 * saved in mWPM.
	 */
	void saveWPM() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(WPM_PREF, mWPM);

		// Commit the edits!
		editor.commit();
	}

	/* void getSavedWPM() 
	 * PRE: None 
	 * POST: Updates the WPM seekbar with the saved
	 * WPM if it exists, or the default value otherwise.
	 */
	void getSavedWPM() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		mWPM = sharedPreferences.getInt(WPM_PREF, DEFAULT_WPM);
		mSeekBar.setProgress(mWPM / 10 - 1);
	}

	/* void writeToFile()
	 * PRE: None
	 * POST: Saves the currently in progress reading and position for use on 
	 * next run.
	 */
	private void writeToFile() {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					openFileOutput("save.txt", Context.MODE_PRIVATE));
			outputStreamWriter.write(String.valueOf(i) + "|;" + toRead);
			outputStreamWriter.close();

		} catch (IOException e) {
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}

	/* String readFromFile() 
	 * PRE: App has been run before and so "save.txt" exists.. exception caught 
	 * if it doesn't.
	 * POST: toRead is filled with the saved reading material from the previous
	 * session, and 'i' is set to the last position of the reading.
	 */
	private String readFromFile() {
		String ret = "";
		try {
			InputStream inputStream = openFileInput("save.txt");

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				char[] receiveString;
				StringBuilder stringBuilder = new StringBuilder();
				int temp;
				while (((temp = bufferedReader.read()) != -1)
						&& ((receiveString = Character.toChars(temp)) != null)) {
					stringBuilder.append(receiveString);
				}
				inputStream.close();
				ret = stringBuilder.toString();
			}
		} catch (FileNotFoundException e) {
			Log.e("login activity", "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e("login activity", "Can not read file: " + e.toString());
		}
		String[] parts = ret.split("\\|;");
		i = Integer.valueOf(parts[0]);
		if (parts.length > 1) // toRead isn't empty
			ret = parts[1];
		else
			ret = "";

		return ret;
	}

	/* double getPeriodPause()
	 * PRE: None
	 * POST: If the user has checked the "punctuation pause" preference, then
	 * this method will return the period delay factor, otherwise it will return
	 * 1 (i.e. no delay).
	 */
	private double getPeriodPause() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPreferences.getBoolean("punctuation_pause_checkbox", false))
			return PAUSE_PERIOD;
		else
			return 1;
	}

	/* double getCommaPause()
	 * PRE: None
	 * POST: If the user has checked the "punctuation pause" preference, then
	 * this method will return the comma delay factor, otherwise it will return
	 * 1 (i.e. no delay).
	 */
	private double getCommaPause() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPreferences.getBoolean("punctuation_pause_checkbox", false))
			return PAUSE_COMMA;
		else
			return 1;
	}

	/* boolean getVariableWPM()
	 * PRE: None
	 * POST: Returns true if user has check "Variable WPM" preference, false
	 * otherwise.
	 */
	private boolean getVariableWPM() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return sharedPreferences.getBoolean("variable_wpm_checkbox", false);
	}

	/* boolean getTimeLeft()
	 * PRE: None
	 * POST: Returns true if user has check "Show Time Left" preference, false
	 * otherwise.
	 */
	private boolean getTimeLeft() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return sharedPreferences.getBoolean("time_left_checkbox", true);
	}
	
	/* long convertVariableWPM(int mWPM)
	 * PRE: mWPM is the current WPM setting set by the user (or the default).
	 * POST: Returns a delay factor to be used in postDelayed().
	 * NOTE: If a word is 7 characters or longer than there will be an increased
	 * delay proportional to the amount of characters more than 7 the word has.
	 */
	private long convertVariableWPM(int mWPM) {
		if (words.get(i - 1).getData().length() > LONG_WORD) {
			double factor;
			factor = ((words.get(i - 1).getData().length() - LONG_WORD) / 10.0) + 1.1;
			return Math.round(convertWPM(mWPM) * factor);
		} else
			return convertWPM(mWPM);
	}

	/* void updateBackground()
	 * PRE: None
	 * POST: Sets the background color, progress bar, seek bar and font colors based
	 * on the user preference of dark or light (with the default being a light background).
	 */
	@SuppressWarnings("ConstantConditions")
	@SuppressLint("NewApi")
	private void updateBackground() {
		// Prefs
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Boolean prefDarkBackground = sharedPreferences.getBoolean(
				"background_checkbox", false);
		// DarkBackground
		if (prefDarkBackground) {
			mBackground.setBackgroundResource(R.drawable.background_holo_dark);
			mPlayButton.setTextColor(getResources().getColor(R.color.grey));
			mRewindButton
					.setColorFilter(getResources().getColor(R.color.white));
			mReadView.setTextColor(getResources().getColor(R.color.white));
			mWordCount.setTextColor(getResources().getColor(R.color.white));
			mWPMView.setTextColor(getResources().getColor(R.color.white));
			mTimeLeft.setTextColor(getResources().getColor(R.color.white));
			mProgressBar.getProgressDrawable().setColorFilter(
					getResources().getColor(R.color.white), Mode.SRC_IN);
			mSeekBar.getProgressDrawable().setColorFilter(
					getResources().getColor(R.color.white), Mode.SRC_IN);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mSeekBar.getThumb().setColorFilter(
						getResources().getColor(R.color.white), Mode.SRC_IN);
			}
		} else {
			// Light background
			mBackground.setBackgroundColor(getResources()
					.getColor(R.color.grey));
			mPlayButton.setTextColor(getResources().getColor(R.color.black));
			mRewindButton
					.setColorFilter(getResources().getColor(R.color.black));
			mReadView.setTextColor(getResources().getColor(R.color.black));
			mWordCount.setTextColor(getResources().getColor(R.color.black));
			mWPMView.setTextColor(getResources().getColor(R.color.black));
			mTimeLeft.setTextColor(getResources().getColor(R.color.black));
			mProgressBar.getProgressDrawable().setColorFilter(
					getResources().getColor(R.color.black), Mode.SRC_IN);
			mSeekBar.getProgressDrawable().setColorFilter(
					getResources().getColor(R.color.black), Mode.SRC_IN);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mSeekBar.getThumb().setColorFilter(
						getResources().getColor(R.color.black), Mode.SRC_IN);
			}
		}
	}

	/* void initSeekBar()
	 * PRE: None
	 * POST: Defines the WPM seekbar's behavior. When it is changed, by the user
	 * scrubbing the thumb, the mWPM variable it updated (in increments of 10 WPM),
	 * and the estimated time left is recalculated. All of this is done instantly 
	 * as the user changes the WPM.
	 */
	private void initSeekBar() {
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mWPM = (progress + 1) * 10;
				updateWPMView();
				updateTimeLeftView();
			}
		});
	}
	
	/* void initSeekWords()\
	 * PRE: None
	 * POST: Adds functionality to mReadView such that when a user clicks on the view (or the word),
	 * a dialog appears with that word, and a seek bar to seek through the words in the reading material.
	 * If the user hits OK, the 'i', mReadView and the progress bar are updated.
	 */
	private void initSeekWords() {
		final int[] changeWord = { i };
		mReadView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							// Yes button clicked
							i = changeWord[0];
							updateReadView();

							break;

						case DialogInterface.BUTTON_NEGATIVE:
							// No button clicked

							// Do nothing...

							break;
						}
					}
				};
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				@SuppressWarnings("ResourceType")
				View layout = inflater.inflate(R.layout.seek_reading_dialog,
						(ViewGroup) findViewById(R.layout.activity_main));
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				builder.setView(layout);
				builder.setTitle("Seek");
				builder.setPositiveButton("Done", dialogClickListener)
						.setNegativeButton("Cancel", dialogClickListener)
						.show();
				final TextView seekWord = (TextView) layout
						.findViewById(R.id.seekWord);
				SeekBar seekWordBar = (SeekBar) layout
						.findViewById(R.id.seekWordBar);
				seekWord.setText(mReadView.getText().toString());
				seekWordBar.setMax(words.size() - 1);
				if (mIsPlaying) {
					changeWord[0] = i - 1;
					seekWordBar.setProgress(i - 1);
				} else {
					changeWord[0] = i;
					seekWordBar.setProgress(i);
				}
				pause();
				seekWordBar
						.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
							@Override
							public void onProgressChanged(SeekBar seekBar,
									int progress, boolean fromUser) {
								seekWord.setText(words.get(progress).getData());
								changeWord[0] = progress;
							}

							@Override
							public void onStartTrackingTouch(SeekBar seekBar) {

							}

							@Override
							public void onStopTrackingTouch(SeekBar seekBar) {

							}
						});
			}
		});
	}

	
	/* void initPlayButton()
	 * PRE: none
	 * POST: Manages what the play button does. If there is reading material to be read,
	 * it will call run() to begin the speed reading, if the user reaches the last word,
	 * it will pause and change to 'reset' and if the user presses it it will reset the 
	 * reading. The play button changes to "Pause" while reading is in progress, and will
	 * pause the reading if the user clicks on it.
	 */
	private void initPlayButton() {
		mPlayButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mIsPlaying && !mIsDone) {
					play();
				} else if (mIsDone) {
					mIsDone = false;
					reset();
				} else {
					pause();
					if (i != 0)
						i--;
				}
			}
		});
	}

	
	private void initRewindButton() {
		// HANDLES PLAY AND DELAY, CALLS run()
		mRewindButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (i >= words.size())
					i = words.size() - 1;
				if (i != 0 && toRead != null) {
					rewindSentence();
				}
			}
		});
		mRewindButton.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (i >= words.size())
					i = words.size() - 1;
				if (i != 0 && toRead != null
						&& words.get(i).getSentenceIndex() != 0)
					rewindPrevSentence();
				return false;
			}
		});
	}

	public void rewindSentence() {
		boolean firstRewind = getSharedPreferences(PREF_FIRST_REWIND,
				MODE_PRIVATE).getBoolean("firstrewind", true);
		if (firstRewind) {
			Toast.makeText(
					this,
					"Tap once to rewind sentence, long press to rewind to previous sentence.",
					Toast.LENGTH_LONG).show();
			getSharedPreferences(PREF_FIRST_REWIND, MODE_PRIVATE).edit()
					.putBoolean("firstrewind", false).commit();
		}
		int newIndex = words.get(i).getSentenceIndex();
		if (newIndex == 0)
			reset();
		else
			i = newIndex;
		updateReadView();
	}

	public void rewindPrevSentence() {
		if (words.get(i).getSentenceIndex() == 0) {
			reset();
		} else
			i = words.get(words.get(i).getSentenceIndex() - 1)
					.getSentenceIndex();
		updateReadView();
	}

	// Starts spreeding, locks orientation, and keeps screen on
	@SuppressLint("InlinedApi")
	private void play() {
		hideSystemUI();
		mPlayPressed = true;
		if (toRead.isEmpty()) {
			Toast.makeText(this, "Add text to begin", Toast.LENGTH_SHORT)
					.show();
		} else {
			lockOrientation();
			updateWPMView();
			mIsPlaying = true;
			mPlayButton.setText(R.string.pause);
			mHandler.post(mBeginSpreed);
			mWordCount.setVisibility(View.GONE);
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	void updateWords() {
		initSentences(toRead);
		initWords(toRead);
	}

	void updateWPMView() {

		if (getVariableWPM())
			mWPMView.setText("~" + String.valueOf(mWPM) + " WPM");
		else
			mWPMView.setText(String.valueOf(mWPM) + " WPM");
	}

	void updateWordCountView() {
		if (words.get(0).getData().equals("") && words.size() <= 1) {
			reset();
			mWordCount.setText("No words entered.");
		} else {
			// reset();
			mWordCount.setText(words.size() + " words");
		}
	}

	void updateTimeLeftView() {
		if (getTimeLeft()) {
			if (getVariableWPM())
				mTimeLeft.setText("~" + calculateTimeLeft());
			else
				mTimeLeft.setText(calculateTimeLeft());
		} else
			mTimeLeft.setText("");
	}

	String calculateTimeLeft() {
		int seconds = 0;
		if (words.size() > 1)
			seconds = (int) (60 * ((double) words.size() - (double) i) / (mWPM));
		return String.format(
				"%02d:%02d",
				TimeUnit.SECONDS.toMinutes(seconds),
				TimeUnit.SECONDS.toSeconds(seconds)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS
								.toMinutes(seconds)));
	}

	// Pauses spreeding, unlock orientation, doesn't keep screen on
	private void pause() {
		showSystemUI();
		mIsPlaying = false;
		mPlayButton.setText(R.string.start);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	// Re-inits spreeder to first Word and stops
	private void reset() {
		showSystemUI();
		i = 0;
		mProgressBar.setProgress(0);
		mPlayButton.setText(R.string.start);
		mReadView.setText(words.get(0).getData());
		updateTimeLeftView();
		mIsPlaying = false;
		mIsDone = false;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mWordCount.setVisibility(View.VISIBLE);
		Log.e(TAG, "RESET");
	}

	private void updateReadView() {
		Log.i(TAG, "i: " + i);
		if (i > words.size())
			i = 0;
		int wordsLeft = words.size() - i - 1;
		if (getChunkSize() == 1) {
			if (i < words.size() && i >= 0) {
				mReadView.setText(words.get(i).getData());
				mProgressBar.setProgress(calculateProgress(words, i));
				updateTimeLeftView();
			}
		} else if (getChunkSize() == 2) {
			if (wordsLeft >= 2) {
				mReadView.setText(words.get(i).getData() + " "
						+ words.get(i + 1).getData());
				mProgressBar.setProgress(calculateProgress(words, i + 1));
				i += 1;
				updateTimeLeftView();
			} else if (i < words.size() && i >= 0) {
				mReadView.setText(words.get(i).getData());
				mProgressBar.setProgress(calculateProgress(words, i));
				updateTimeLeftView();
			}
		} else {
			if (wordsLeft >= 3) {
				mReadView.setText(words.get(i).getData() + " "
						+ words.get(i + 1).getData() + " "
						+ words.get(i + 2).getData());
				mProgressBar.setProgress(calculateProgress(words, i + 2));
				i += 2;
				updateTimeLeftView();
			} else if (wordsLeft >= 2) {
				mReadView.setText(words.get(i).getData() + " "
						+ words.get(i + 1).getData());
				mProgressBar.setProgress(calculateProgress(words, i + 1));
				i += 1;
				updateTimeLeftView();
			} else if (i < words.size() && i >= 0) {
				mReadView.setText(words.get(i).getData());
				mProgressBar.setProgress(calculateProgress(words, i));
				updateTimeLeftView();
			}
		}
	}

	// Converts Words per minute to milliseconds (for handler delay)
	private long convertWPM(int WPM) {
		double temp = ((double) WPM / (double) 60);
		temp = Math.pow(temp, -1) * 1000;
		return Math.round(temp);
	}

	// Calculates rough percentage progress by word# / total words
	private int calculateProgress(ArrayList<Word> word2, int index) {
		double length = word2.size();
		if (index == words.size() - 1)
			return 100;
		return (int) Math.round((index / length) * 100);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.resetText:
			reset();
			return true;
		case R.id.addText:
			Intent addTextIntent = new Intent(this, PasteActivity.class);
			if (book != null)
				addTextIntent.putExtra("Book", (Parcelable) book);
			addTextIntent.putExtra(KEY_TO_READ, toRead);
			pause();
			startActivityForResult(addTextIntent, 0);
			return true;
		case R.id.settings:
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			pause();
			startActivityForResult(settingsIntent, 3);
			return true;
		case R.id.about:
			pause();
			Intent aboutIntent = new Intent(this, About.class);
			startActivity(aboutIntent);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Manages the font size preference and changes the readView font size
	private void setFontSize() {
		int mFontSize;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		mFontSize = Integer.parseInt((prefs.getString(KEY_FONT_SIZE, "1")));
		switch (mFontSize) {
		case 3:
			mReadView.setTextSize(LARGE);
			break;
		case 2:
			mReadView.setTextSize(MEDIUM);
			break;
		case 1:
			mReadView.setTextSize(SMALL);
			break;
		}
	}

	private void setTextAlignment() {
		int mTextAlignment;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		mTextAlignment = Integer.parseInt((prefs.getString(KEY_TEXT_ALIGNMENT,
				"3")));
		switch (mTextAlignment) {
		case 3:
			mReadView.setGravity(CENTER);
			break;
		case 2:
			mReadView.setGravity(LEFT);
			break;
		case 1:
			mReadView.setGravity(RIGHT);
			break;
		}
	}

	private int getChunkSize() {
		int mChunkSize;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		mChunkSize = Integer.parseInt((prefs.getString(KEY_CHUNK_SIZE, "1")));
		return mChunkSize;
	}

	public void initSentences(String input) {
		sentences.clear();
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		String source = input.replaceAll("\\s+", "");
		iterator.setText(source);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
				.next()) {
			sentences
					.add(new Sentence(source.substring(start, end), start, end));
		}
	}

	public void initWords(String input) {
		String[] wordsArray = input.split("\\s+");
		words.clear();
		int index = 0;
		for (String word : wordsArray) {
			if (words.isEmpty()) {
				words.add(new Word(word, 0, word.length(), word.length(), 0,
						index));
			} else {
				// Word(String data, int start, int end, int length)
				words.add(new Word(word, words.get(index - 1).getEnd(), words
						.get(index - 1).getEnd() + word.length(),
						word.length(), 0, index));
			}
			index++;
		}
		initSentenceStart();
	}

	void initSentenceStart() {
		int index = 0;
		for (Sentence sentence : sentences) {
			for (Word word : words) {
				if (word.getStart() == sentence.getStart()) {
					index = word.getIndex();
					word.setSentenceIndex(index);
				} else if (word.getStart() >= sentence.getStart()
						&& word.getEnd() <= sentence.getEnd()) {
					word.setSentenceIndex(index);
				}
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		if (arg1.equals("pref_fontSize")) {
			setFontSize();
		}
		if (arg1.equals("pref_textAlignment"))
			setTextAlignment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// 0 = Returning from pasteActivity
		Log.i(TAG, "OAR: requestCode = " + requestCode + "; resultCode = "
				+ resultCode);
		if (requestCode == 0) { // PasteActivity
			if (data == null) {
				Log.e(TAG, "OAR: DATA == null");
				return;
			}
			// if the input has changed
			if (!toRead.equals(data.getStringExtra(PasteActivity.KEY_PASTE))) {
				toRead = data.getStringExtra(PasteActivity.KEY_PASTE);
				i = 0;
				updateWords();
				updateWordCountView();
				updateTimeLeftView();
				mActivityResulted = true;
			}
		} else if (requestCode == 3) { // settings menu
			updateBackground();
			updateWPMView();
			updateTimeLeftView();
		}
	}

	// Speed Reading runnable
	private final Runnable mBeginSpreed = new Runnable() {
		@Override
		public void run() {
			if (mIsPlaying && words.size() > 0) {
				if (i < words.size() && words.get(i).getData() != null
						&& i != 0) {
					updateReadView();
				} else {
					if (words != null && i != words.size()) {
						updateReadView();
					}
				}
				if (i < words.size()) {
					i++;
					if (i > 0 && words.get(i - 1).getData().endsWith(".")
							&& getChunkSize() == 1)
						mHandler.postDelayed(this,
								Math.round(convertWPM(mWPM) * getPeriodPause()));
					else if (i > 0 && words.get(i - 1).getData().contains(",")
							&& getChunkSize() == 1)
						mHandler.postDelayed(
								this,
								(int) Math.round(convertWPM(mWPM)
										* getCommaPause()));
					else if (getVariableWPM()) {
						mHandler.postDelayed(this, convertVariableWPM(mWPM)
								* getChunkSize());
					} else {
						mHandler.postDelayed(this, convertWPM(mWPM)
								* getChunkSize());
					}
				} else {
					// end of reading
					mIsPlaying = false;
					mIsDone = true;
					mPlayButton.setText(R.string.reset);
					getWindow().clearFlags(
							WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				}
			}
		}
	};

	protected void lockOrientation() {
		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_PORTRAIT:
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

		case Configuration.ORIENTATION_LANDSCAPE:
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

	// First run dialog
	void firstRunDialog() {
		boolean firstrun = getSharedPreferences(PREF_FIRST_RUN, MODE_PRIVATE)
				.getBoolean("firstrun", true);
		if (firstrun) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						// Yes button clicked
						getSharedPreferences(PREF_FIRST_RUN, MODE_PRIVATE)
								.edit().putBoolean("firstrun", false).commit();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						getSharedPreferences(PREF_FIRST_RUN, MODE_PRIVATE)
								.edit().putBoolean("firstrun", true).commit();
						break;
					}
				}
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Welcome to Spree");
			builder.setMessage(
					"Thanks for downloading Spree! You can add material by hitting the '+' button and then either pasting in material or going to the menu and adding a .epub file. You can also add material by sharing articles from other apps or even your browser via \"Add to Spree\". If you enjoy this app please review it, thanks!")
					.setPositiveButton("Got It", dialogClickListener)
					.setNegativeButton("Show Next Time", dialogClickListener)
					.show();
		}
	}

	// This snippet hides the system bars.
	private void hideSystemUI() {
		// Set the IMMERSIVE flag.
		// Set the content to appear under the system bars so that the content
		// doesn't resize when the system bars hide and show.
		if (android.os.Build.VERSION.SDK_INT >= 19 && mUiShowing) {
			mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
					| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
					| View.SYSTEM_UI_FLAG_IMMERSIVE);
			mUiShowing = false;
		}
	}

	// This snippet shows the system bars. It does this by removing all the
	// flags
	// except for the ones that make the content appear under the system bars.
	private void showSystemUI() {
		if (android.os.Build.VERSION.SDK_INT >= 19 && !mUiShowing) {
			mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
			mUiShowing = true;
		}

	}

} // End file

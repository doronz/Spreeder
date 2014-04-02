package com.doronzehavi.spree;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.andrewgiang.textspritzer.lib.SpritzerTextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements
        OnSharedPreferenceChangeListener {

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

    // Constants
    public static final String TAG = "SPREE";
    private static final int DEFAULT_WPM = 250;
    private static final int LONG_WORD = 7;

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
    SpritzerTextView spritzerTV = null;
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
    private boolean mSpritzing = false;
    private boolean mAllowSpritzing = false;
    private boolean mPlayPressed = false;

    protected ProgressBar mProgressBar;
    protected ProgressBar mSpritzProgressBar;

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
        spritzerTV = (SpritzerTextView) findViewById(R.id.spritzView);
        mWPMView = (TextView) findViewById(R.id.wpm);
        mWordCount = (TextView) findViewById(R.id.wordCount);
        mTimeLeft = (TextView) findViewById(R.id.timeLeft);
        mPlayButton = (Button) findViewById(R.id.play_button);
        mRewindButton = (ImageButton) findViewById(R.id.rewind_button);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mBackground = (RelativeLayout) findViewById(R.id.background);
        mSpritzProgressBar = (ProgressBar) findViewById(R.id.spritzProgressbar);

        // Initializing values
        if (i == 0) {
            updateWords();
        }
        initPlayButton();
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

/*                if (words.get(0).getData().equals("")) {
                    mWordCount.setText("No words entered.");
                } else {
                    mWordCount.setText(words.size() + " words");
                }*/

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

        //noinspection ConstantConditions
        if (getIntent() != null && !getIntent().getAction().equals(Intent.ACTION_DEFAULT)) {
            if (!mActivityResulted) {
                try {
                    toRead = readFromFile();
                    spritzerTV.setSpritzText(toRead);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            mActivityResulted = false;
        } else {
            String temp = getIntent().getStringExtra("paste");
            if (temp != null) {
                toRead = temp;
                spritzerTV.setSpritzText(toRead);
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

    void saveWPM() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(WPM_PREF, mWPM);

        // Commit the edits!
        editor.commit();
    }

    void getSavedWPM() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        mWPM = sharedPreferences.getInt(WPM_PREF, DEFAULT_WPM) / 10 - 1;
        mSeekBar.setProgress(mWPM);
        spritzerTV.setWpm(mWPM);
    }

    // Save reading material
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
        Log.i(TAG, "i = " + i);
        if (parts.length > 1) // toRead isn't empty
            ret = parts[1];
        else
            ret = "";

        return ret;
    }

    // Text shared to app

    // Checks if user selected variable wpm
    private boolean getVariableWPM() {

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(
                "variable_wpm_checkbox", false);
    }

    // Checks if user selected time left
    private boolean getTimeLeft() {

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(
                "time_left_checkbox", true);
    }

    private long convertVariableWPM(int mWPM) {
        if (words.get(i - 1).getData().length() > LONG_WORD) {
            double factor;
            factor = ((words.get(i - 1).getData().length() - LONG_WORD) / 10.0) + 1.1;
            return Math.round(convertWPM(mWPM) * factor);
        } else
            return convertWPM(mWPM);
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("NewApi")
    private void updateBackground() {
        // Prefs
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        Boolean prefDarkBackground = sharedPreferences.getBoolean(
                "background_checkbox", true);
        // DarkBackground
        if (prefDarkBackground) {
            mBackground.setBackgroundResource(R.drawable.background_holo_dark);
            mPlayButton.setTextColor(getResources().getColor(R.color.grey));
            mRewindButton
                    .setColorFilter(getResources().getColor(R.color.white));
            mReadView.setTextColor(getResources().getColor(R.color.white));
            spritzerTV.setTextColor(getResources().getColor(R.color.white));
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
            // TODO test this
            spritzerTV.setTextColor(getResources().getColor(R.color.black));
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
                if (spritzerTV != null)
                    spritzerTV.setWpm(mWPM);
                updateWPMView();
                updateTimeLeftView();
            }
        });
    }

    private void initPlayButton() {
        // HANDLES PLAY AND DELAY, CALLS run()
        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mIsPlaying && !mIsDone) {
                    play();
                    spritzerTV.play();
                } else if (mIsDone) {
                    mIsDone = false;
                    reset();
                    spritzerTV = (SpritzerTextView) findViewById(R.id.spritzView);
                    spritzerTV.setSpritzText(toRead);

                } else {
                    spritzerTV.pause();
                    pause();
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

    // rewinds current sentence
    public void rewindSentence() {
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
                        .toMinutes(seconds))
        );
    }

    // Pauses spreeding, unlock orientation, doesn't keep screen on
    private void pause() {
        if (spritzerTV != null)
            spritzerTV.pause();
        mIsPlaying = false;
        mPlayButton.setText(R.string.start);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Re-inits spreeder to first Word and stops
    private void reset() {
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
                if (!mSpritzing)
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
        double i;
        i = index;
        if (i == words.size() - 1) {
            return 100;
        }
        double prog = (i / length) * 100;
        return (int) Math.round(prog);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.resetText:
                spritzerTV.pause();
                spritzerTV.setSpritzText(toRead);
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
            case R.id.enableSpritzing:
                spritzerTV.pause();
                pause();
                if (mAllowSpritzing) { // Disable spritzing
                    mAllowSpritzing = false;
                    spritzingManager();
                } else { // Enable spritzing
                    spritzerTV.setSpritzText(toRead);
                    mAllowSpritzing = true;
                    spritzingDialog(item);
                }
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
                        word.length(), 0, index
                ));
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
            disableSpritzing();
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
                i++;
                if (i <= words.size()) {
                    if (i > 0 && words.get(i - 1).getData().endsWith(".")
                            && getChunkSize() == 1)
                        mHandler.postDelayed(this,
                                Math.round(convertWPM(mWPM) * 2));
                    else if (i > 0 && words.get(i - 1).getData().contains(",")
                            && getChunkSize() == 1)
                        mHandler.postDelayed(this,
                                (int) Math.round(convertWPM(mWPM) * 1.5));
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

    // SPRITZING
    void spritzingDialog(final MenuItem item) {
        if (mAllowSpritzing) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            // Yes button clicked
                            mAllowSpritzing = true;
                            mSpritzing = true;
                            reset();
                            spritzingManager();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // No button clicked
                            mAllowSpritzing = false;
                            mSpritzing = false;
                            spritzingManager();
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enable Red Letter Alignment?");
            builder.setMessage(
                    "This is an experimental feature and may cause unexpected behavior.")
                    .setPositiveButton("Got It", dialogClickListener)
                    .setNegativeButton("Nevermind", dialogClickListener).show();
        }
    }

    void spritzingManager() {
        Log.e(TAG, mSpritzing + " " + mAllowSpritzing);
        if (mAllowSpritzing) {
            enableSpritzing();
        } else {
            disableSpritzing();
        }
    }

    void disableSpritzing() {
        mAllowSpritzing = false;
        mSpritzing = false;
        reset();
        mProgressBar.setMax(100);
        spritzerTV.setVisibility(View.INVISIBLE);
        mReadView.setVisibility(View.VISIBLE);
        mTimeLeft.setVisibility(View.VISIBLE);
        mRewindButton.setVisibility(View.VISIBLE);
    }

    void enableSpritzing() {
        mSpritzing = true;
        reset();
        spritzerTV.setVisibility(View.VISIBLE);
        spritzerTV.setSpritzText(toRead);
        mReadView.setVisibility(View.INVISIBLE);
        mTimeLeft.setVisibility(View.INVISIBLE);
        mRewindButton.setVisibility(View.INVISIBLE);
        spritzerTV.attachProgressBar(mSpritzProgressBar);
        mSpritzProgressBar
                .setMax(spritzerTV.getSpritzer().getWordArray().length);

    }

} // End file

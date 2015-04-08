package com.example.amplayer;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MusicPlayActivity extends Activity {
	
	private static final String TAG = "MusicPlayActivity";
	private static final String ACTION_MUSIC_CHANGED = "com.amplayer.music_changed";
	private static final int PLAY_MODE_REPEAT_ALL = 0;
	private static final int PLAY_MODE_REPEAT_ONE = 1;
	private MusicService mMusicService = null;
	private SeekBar musicSeekBar;
	private Button prevButton, playButton, nextButton, playModeButton;
	private TextView curPositionTextView, durationTextView;
	private TextView titleTextView, artistTextView, albumTextView;
	private ImageView musicImageView;
	private SharedPreferences mPreferences;
	private MusicChangedReceiver receiver;

	private Handler mHandler = new Handler();
	private Runnable mRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				int curPosition = mMusicService.getCurPosition();
				int duration = mMusicService.getDuration();
				musicSeekBar.setMax(duration);
				musicSeekBar.setProgress(curPosition);
				curPositionTextView.setText(secondsToMinutes(curPosition));
				durationTextView.setText(secondsToMinutes(duration));
				titleTextView.setText(mMusicService.getMusicTitle());
				artistTextView.setText(mMusicService.getMusicArtist());
				albumTextView.setText(mMusicService.getMusicAlbum());
				playButton.setBackgroundResource(
						mMusicService.isPlaying()?
								R.drawable.img_appwidget_pause
								:R.drawable.img_appwidget_play);
			} catch (Exception e) {
				// TODO: handle exception
			}
			mHandler.postDelayed(mRunnable, 500);
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_play);
		
		musicSeekBar = (SeekBar)findViewById(R.id.seekBar_music_play);
		prevButton = (Button)findViewById(R.id.button_prev_music);
		playButton = (Button)findViewById(R.id.button_play_pause);
		nextButton = (Button)findViewById(R.id.button_next_music);
		playModeButton = (Button)findViewById(R.id.button_play_mode);
		curPositionTextView = (TextView)findViewById(R.id.textView_played_time);
		durationTextView = (TextView)findViewById(R.id.textView_music_duration);
		titleTextView = (TextView)findViewById(R.id.textView_title_play_activity);
		albumTextView = (TextView)findViewById(R.id.textView_album_play_activity);
		artistTextView = (TextView)findViewById(R.id.textView_artist_play_activity);
		musicImageView = (ImageView)findViewById(R.id.imageView_music_picture);
		
		musicSeekBar.setOnSeekBarChangeListener(new MusicSeekBarListener());

		mPreferences = getSharedPreferences("config", 0);
		switch (mPreferences.getInt("play_mode", 0)) {
		case PLAY_MODE_REPEAT_ALL:
			playModeButton.setBackgroundResource(R.drawable.img_appwidget_playmode_repeat);
			break;
		case PLAY_MODE_REPEAT_ONE:
			playModeButton.setBackgroundResource(R.drawable.img_appwidget_playmode_repeatone);
			break;

		default:
			break;
		}
		
		IntentFilter intentFilter= new IntentFilter();
		intentFilter.addAction(ACTION_MUSIC_CHANGED);
		receiver = new MusicChangedReceiver();
		registerReceiver(receiver, intentFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.music_play, menu);
		return true;
	}
	
	@Override
	protected void onStart() {
		Intent intent = new Intent();
		intent.setClass(MusicPlayActivity.this, MusicService.class);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
		
		mHandler.post(mRunnable);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		unbindService(mServiceConnection);
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	public ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mMusicService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mMusicService = ((MusicService.MusicBinder)service).getService();
			displayMusicPicture();
		}
	};
	
	public void onButtonClicked(View view){
		switch (view.getId()) {
		case R.id.button_play_pause:
			mMusicService.playOrPause();
			playButton.setBackgroundResource(mMusicService.isPlaying()?
					R.drawable.img_appwidget_pause
					:R.drawable.img_appwidget_play);
			break;
		case R.id.button_next_music:
			mMusicService.switchMusic(true);
			playButton.setBackgroundResource(R.drawable.img_appwidget_play);
			break;
		case R.id.button_prev_music:
			mMusicService.switchMusic(false);
			playButton.setBackgroundResource(R.drawable.img_appwidget_play);
			break;
		case R.id.button_play_mode:
			Editor editor = getSharedPreferences("config", 0).edit();
			editor.putInt("play_mode", mMusicService.isLooping()?
					PLAY_MODE_REPEAT_ALL:PLAY_MODE_REPEAT_ONE);
			editor.commit();
			playModeButton.setBackgroundResource(
					mMusicService.isLooping()?
							R.drawable.img_appwidget_playmode_repeat
							:R.drawable.img_appwidget_playmode_repeatone);
			mMusicService.setLooping(!mMusicService.isLooping());
			break;

		default:
			break;
		}
	}
	
	class MusicSeekBarListener implements OnSeekBarChangeListener{
		@Override
		public void onProgressChanged(SeekBar arg0, int progress, boolean fromUser) {
			// TODO Auto-generated method stub
			if (fromUser==true) {
				mMusicService.seekTo(progress);
            }
		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@SuppressLint("DefaultLocale")
	public String secondsToMinutes(int millis){
		int minutes = millis/1000/60;
		int seconds = millis/1000%60;
		return String.format("%02d:%02d", minutes, seconds);
	}
	
	private Bitmap getEmbeddedPicture(String filePath) {
	    Bitmap bitmap = null;
	    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
	    retriever.setDataSource(filePath);
	    byte[] art = retriever.getEmbeddedPicture();

	    if( art != null ){
	    	bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
	    }
	    return bitmap;
	}
	
	private void displayMusicPicture(){
		if (mMusicService == null || mMusicService.getMusicFilePath() == null) {
			return;
		}
		Bitmap bitmap = getEmbeddedPicture(mMusicService.getMusicFilePath());
		if (bitmap == null) {
			musicImageView.setVisibility(View.GONE);
		}
		else {
			musicImageView.setVisibility(View.VISIBLE);
			musicImageView.setImageBitmap(bitmap);
		}
	}
	
	public class MusicChangedReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(ACTION_MUSIC_CHANGED)) {
				displayMusicPicture();
			}
		}
		
	}

}

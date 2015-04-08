package com.example.amplayer;

import java.util.List;

import com.example.amplayer.MusicProber.MusicInfo;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service implements
	MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

	private static final String TAG = "MusicService";
	private static final String ACTION_PLAY = "com.example.amplayer.action.PLAY";
	private static final String ACTION_MUSIC_CHANGED = "com.amplayer.music_changed";
	private MediaPlayer mMediaPlayer = null;
	private List<MusicInfo> mMusicInfos;
	private int playIndex;
	private MusicBinder mMusicBinder;
	private int duration;
	private static final int PLAY_MODE_REPEAT_ALL = 0;
	private static final int PLAY_MODE_REPEAT_ONE = 1;
	private boolean userSetLoop = false;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mMediaPlayer = new MediaPlayer();
		mMusicBinder = new MusicBinder();
		userSetLoop = (getSharedPreferences("config", 0)
				.getInt("play_mode", PLAY_MODE_REPEAT_ALL)
				== PLAY_MODE_REPEAT_ONE)?true:false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.d("", "****************onBind*******************");
		return mMusicBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction().equals(ACTION_PLAY)) {
			playMusic(intent.getStringExtra("Url"));
			playIndex = intent.getIntExtra("position", 0);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mMediaPlayer.stop();
		mMediaPlayer.release();
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		// TODO Auto-generated method stub
		mMediaPlayer.start();
		duration = mMediaPlayer.getDuration();
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void playMusic(String url){
		mMediaPlayer.reset();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(url));
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.prepareAsync();
			mMediaPlayer.setLooping(userSetLoop);
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer player) {
					switchMusic(true);
				}
			});
			Intent intent = new Intent();
			intent.setAction(ACTION_MUSIC_CHANGED);
			sendBroadcast(intent);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public int getCurPosition(){
		return mMediaPlayer.getCurrentPosition();
	}
	
	public int getDuration(){
		return duration;
	}
	
	public void setMusicList(List<MusicInfo> list){
		mMusicInfos = list;
	}
	
	public void playOrPause(){
		if (mMediaPlayer.isPlaying()) {
			mMediaPlayer.pause();
		}
		else {
			mMediaPlayer.start();
		}
	}
	
	public void switchMusic(boolean next){
		if (next) {
			playIndex = (playIndex == mMusicInfos.size()-1)?0:(playIndex+1);
		}
		else {
			playIndex = (playIndex == 0)?(mMusicInfos.size()-1):(playIndex-1);
		}
		playMusic(mMusicInfos.get(playIndex).getUrl());
	}
	
	public boolean isPlaying(){
		return mMediaPlayer.isPlaying();
	}
	
	public void setLooping(Boolean loop){
		userSetLoop = loop;
		mMediaPlayer.setLooping(loop);
	}
	
	public void seekTo(int position){
		mMediaPlayer.seekTo(position);
	}
	
	public boolean isLooping(){
		return mMediaPlayer.isLooping();
	}
	
	public String getMusicTitle(){
		try {
			return mMusicInfos.get(playIndex).getTitle();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
		
	}
	
	public String getMusicArtist(){
		try {
			return mMusicInfos.get(playIndex).getArtist();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
		
	}
	
	public String getMusicAlbum(){
		try {
			return mMusicInfos.get(playIndex).getAlbum();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}
	
	public String getMusicFilePath(){
		try {
			return mMusicInfos.get(playIndex).getUrl();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}
	
	public class MusicBinder extends Binder{
		public MusicService getService(){
			return MusicService.this;
		}
	}


}

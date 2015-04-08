package com.example.amplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.example.amplayer.MusicProber.MusicInfo;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.widget.Toast;

public class UsbStateReceiver extends BroadcastReceiver {

	private static final String DATABASE_NAME = "music.db";
	private static final String ACTION_USB_SCAN_COMPLETED = "com.example.amplayer.usb_scan_completed";
	private static final int DATABASE_VERSION = 1;
	private MusicSQLiteHelper  helper;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		helper = new MusicSQLiteHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)
				|| intent.getAction().equals(Intent.ACTION_MEDIA_CHECKING)) {
			Toast.makeText(context, "USB device detected, start to scan mp3 files",
					Toast.LENGTH_SHORT).show();
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					List<MusicInfo> list = scanMusicFiles("/mnt/usb/sda1");
					saveUsbMusicInfo2DB(helper, list);
					Intent intent = new Intent();
					intent.putExtra("number", list.size());
					intent.setAction(ACTION_USB_SCAN_COMPLETED);
					mContext.sendBroadcast(intent);
				}
			}).start();
		}
		else if (intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)
				|| intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
			Toast.makeText(context, "USB device removed",
					Toast.LENGTH_SHORT).show();
			deleteUsbMusicInfoFromDB(helper);
			Intent intent1 = new Intent();
			intent.setAction(ACTION_USB_SCAN_COMPLETED);
			mContext.sendBroadcast(intent1);
		}
	}
	
	private List<MusicInfo> scanMusicFiles(String dir){
		List<MusicInfo> musicInfos = new ArrayList<MusicInfo>();
		try {
			File root = new File(dir);
			for (File file : root.listFiles()) {
				if (file.isFile() && file.getName().matches("^.*\\.mp3$")) {
					musicInfos.add(getMusicInfo(file.getAbsolutePath()));
				}
			}Toast.makeText(mContext, musicInfos.size() + " musics found", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return musicInfos;
	}
	
	public MusicInfo getMusicInfo(String filepath){
		MusicInfo info = new MusicInfo();
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		try {
			mmr.setDataSource(filepath);
			info.setAlbum(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
			info.setArtist(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
			info.setTitle(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
			info.setUrl(filepath);
			info.setDuration(Integer.parseInt(mmr.extractMetadata(
					MediaMetadataRetriever.METADATA_KEY_DURATION)));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return info;
	}
	
	private Boolean saveUsbMusicInfo2DB(MusicSQLiteHelper helper, List<MusicInfo> musicInfos)
	{
		try {
			SQLiteDatabase db = helper.getWritableDatabase();
			for (MusicInfo info : musicInfos) {
				ContentValues contentValues = new ContentValues();
				contentValues.put("mid", info.getId());
				contentValues.put("title", info.getTitle());
				contentValues.put("album", info.getAlbum());
				contentValues.put("duration", info.getDuration());
				contentValues.put("size", info.getSize());
				contentValues.put("artist", info.getArtist());
				contentValues.put("url", info.getUrl());
				db.insert("usb", null, contentValues);
				contentValues.clear();
			}
			db.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
	}
	
	private Boolean deleteUsbMusicInfoFromDB(MusicSQLiteHelper helper)
	{
		try {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.execSQL("delete from usb");
			db.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
	}

}

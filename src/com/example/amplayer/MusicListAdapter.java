package com.example.amplayer;

import java.util.List;

import com.example.amplayer.MusicProber.MusicInfo;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MusicListAdapter extends BaseAdapter {

	private Context mContext;
	private ViewHolder holder = null;
	private List<MusicInfo> musicList;
	private MusicSQLiteHelper musicSQLiteHelper;
	private static final String DATABASE_NAME = "music.db";
	private static final int DATABASE_VERSION = 1;
	private static final String ACTION_SET_FAV = "com.amplayer.music_set_fav";
	
	public MusicListAdapter(Context context, List<MusicInfo> list) {
		// TODO Auto-generated constructor stub
		mContext = context;
		musicList = list;
		musicSQLiteHelper = new MusicSQLiteHelper(mContext,
				DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return musicList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return musicList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(mContext, R.layout.music_list_item1, null);
			holder.musicIDTextView = (TextView)convertView.findViewById(R.id.textView_music_id);
			holder.musicNameTextView = (TextView) convertView.findViewById(R.id.textView_music_title);
			holder.artistTextView = (TextView) convertView.findViewById(R.id.textView_music_artist);
			holder.favoriteButton = (Button) convertView.findViewById(R.id.button_favorite);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.position = position;
		holder.musicIDTextView.setText(String.valueOf(position + 1));
		holder.musicNameTextView.setText(musicList.get(position).getTitle());
		holder.artistTextView.setText(musicList.get(position).getArtist());
		holder.favoriteButton.setOnClickListener(new FavButtonListener());
		holder.favoriteButton.setBackgroundResource(
				IsFavoriteMusic(musicSQLiteHelper, musicList.get(position))?
						R.drawable.img_musiccircle_favorite:
						R.drawable.img_musiccircle_message_unfavorite);
		Log.d("chen", "position = " + position + " Height = " + convertView.getHeight());
		return convertView;
	}
	
	public ViewHolder getViewHolder(View v){
		if (v.getTag() == null){
		    return getViewHolder((View) v.getParent());
		  }
		  return (ViewHolder ) v.getTag();
	}
	
	public class FavButtonListener implements OnClickListener{
		@Override
		public void onClick(View view) {
			// TODO Auto-generated method stub
			int vid = view.getId();
			if (vid == holder.favoriteButton.getId()){
				ViewHolder holder = getViewHolder(view);
				if (IsFavoriteMusic(musicSQLiteHelper, musicList.get(holder.position))){
					RemoveMusicFromDBFavorite(musicSQLiteHelper,musicList.get(holder.position));
					view.setBackgroundResource(R.drawable.img_musiccircle_message_unfavorite);
				}
				else {
					AddMusicToDBFavorite(musicSQLiteHelper,musicList.get(holder.position));
					view.setBackgroundResource(R.drawable.img_musiccircle_favorite);
				}
				Intent intent = new Intent();
				intent.setAction(ACTION_SET_FAV);
				mContext.sendBroadcast(intent);
			}
		}
	}

	private Boolean AddMusicToDBFavorite(MusicSQLiteHelper helper, MusicInfo musicInfo)
	{
		try {
			SQLiteDatabase db = helper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put("mid", musicInfo.getId());
			contentValues.put("title", musicInfo.getTitle());
			contentValues.put("album", musicInfo.getAlbum());
			contentValues.put("duration", musicInfo.getDuration());
			contentValues.put("size", musicInfo.getSize());
			contentValues.put("artist", musicInfo.getArtist());
			contentValues.put("url", musicInfo.getUrl());
			db.insert("favorites", null, contentValues);
			contentValues.clear();
			db.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
	}
	
	private void RemoveMusicFromDBFavorite(MusicSQLiteHelper helper, MusicInfo musicInfo)
	{
		try {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete("favorites", "url=?", new String[]{musicInfo.getUrl()});
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private Boolean IsFavoriteMusic(MusicSQLiteHelper helper, MusicInfo musicInfo)
	{
		try {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.query("favorites", null, "url=?",
					new String[]{musicInfo.getUrl()}, null, null, null);
			return (cursor.getCount() > 0);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
	}
	
	class ViewHolder {
		public TextView musicIDTextView;
		public TextView musicNameTextView;
		public TextView artistTextView;
		public Button favoriteButton;
		public int position;
	}
}

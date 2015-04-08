package com.example.amplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.amplayer.MusicProber.MusicInfo;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MusicListActivity extends FragmentActivity implements
		ActionBar.TabListener {

	private static final String DATABASE_NAME = "music.db";
	private static final int DATABASE_VERSION = 1;
	private static View[] mFragmentView = new View[3];
	private static MusicSQLiteHelper musicSQLiteHelper;
	private static final String ACTION_PLAY = "com.example.amplayer.action.PLAY";
	private static List<MusicInfo> listCur, listAll, listFav, listOnline;
	private static MusicOnItemClickListener musicOnItemClickListener;
	private static FavoriteMusicLongClickListener favoriteMusicLongClickListener;
	private static int curPosion;
	private static MusicService mMusicService = null;
	private static String[] onlineMusic = new String[]{
		"http://download.cvte.cn/downfile.php?file_id=38059&file_key=pAbtZTtB",
		"http://download.cvte.cn/downfile.php?file_id=38060&file_key=9LTCkjia"
	};
	private static final String ACTION_USB_SCAN_COMPLETED = "com.example.amplayer.usb_scan_completed";
	private static final String ACTION_MUSIC_DOWNLOADED = "amplayer.music_downloaded";
	private static final String ACTION_MUSIC_CHANGED = "com.amplayer.music_changed";
	private static final String ACTION_SET_FAV = "com.amplayer.music_set_fav";
	private UsbMusicFoundReceiver receiver;
	private static boolean specificArtistMusic = false;
	private static boolean downloadMusic = false;
	private static String curArtist;
	private static MusicDownloadTask mDownloadTask;
	private Button playButton;
	private TextView titleTextView, artistTextView;
	private ImageView musicImageView;
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	private MusicChangedReceiver musicChangedReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_list);
		
		musicOnItemClickListener = new MusicOnItemClickListener();
		favoriteMusicLongClickListener = new FavoriteMusicLongClickListener();
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		musicImageView = (ImageView)findViewById(R.id.imageView_music_picture2);
		
		playButton = (Button)findViewById(R.id.button_play_panel_play);
		titleTextView = (TextView)findViewById(R.id.textView_play_panel_title);
		artistTextView = (TextView)findViewById(R.id.textView_play_panel_artist);

		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
						mSectionsPagerAdapter.getItem(position);
						initListView(MusicListActivity.this, position);
					}
				});

		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		Intent intent = new Intent();
		intent.setClass(MusicListActivity.this, MusicService.class);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
		
		IntentFilter intentFilter= new IntentFilter();
		intentFilter.addAction(ACTION_USB_SCAN_COMPLETED);
		intentFilter.addAction(ACTION_MUSIC_DOWNLOADED);
		intentFilter.addAction(ACTION_SET_FAV);
		receiver = new UsbMusicFoundReceiver();
		registerReceiver(receiver, intentFilter);

		IntentFilter intentFilter2 = new IntentFilter();
		intentFilter2.addAction(ACTION_MUSIC_CHANGED);
		musicChangedReceiver = new MusicChangedReceiver();
		registerReceiver(musicChangedReceiver, intentFilter2);
		
		mDownloadTask = new MusicDownloadTask(getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.music_list, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		curPosion = tab.getPosition();
		mViewPager.setCurrentItem(tab.getPosition());
	}
	
	@Override
	protected void onResume() {
		if (mMusicService != null
				&& mMusicService.getMusicTitle() != null) {
			playButton.setBackgroundResource(mMusicService.isPlaying()?
					R.drawable.img_appwidget_pause
					:R.drawable.img_appwidget_play);
			titleTextView.setText(mMusicService.getMusicTitle());
			artistTextView.setText(mMusicService.getMusicArtist());
		}
		displayMusicPicture();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(mDownloadTask != null){
			mDownloadTask.cancel(true);
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mServiceConnection);
		unregisterReceiver(receiver);
		unregisterReceiver(musicChangedReceiver);
		super.onDestroy();
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
	
	public void onClick(View view){
		if (mMusicService == null || mMusicService.getMusicTitle() == null) {
			Toast.makeText(getApplicationContext(),
					"Please select a music to play",
					Toast.LENGTH_SHORT).show();
			return;
		}
		switch (view.getId()) {
		case R.id.button_play_panel_play:
			mMusicService.playOrPause();
			playButton.setBackgroundResource(mMusicService.isPlaying()?
					R.drawable.img_appwidget_pause
					:R.drawable.img_appwidget_play);
			break;
		case R.id.textView_play_panel_title:
		case R.id.textView_play_panel_artist:
		case R.id.imageView_music_picture2:
			Intent mIntent = new Intent();
			mIntent.setClass(MusicListActivity.this, MusicPlayActivity.class);
			startActivity(mIntent);
			break;

		default:
			break;
		}
	}
	
	public static ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mMusicService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mMusicService = ((MusicService.MusicBinder)service).getService();
		}
	};
	
	public static void initListView(Context context, int position)
	{
		ListView listView = (ListView) mFragmentView[position].findViewById(R.id.listView_music);
		listView.setOnItemClickListener(musicOnItemClickListener);
		musicSQLiteHelper = new MusicSQLiteHelper(context,
				DATABASE_NAME, null, DATABASE_VERSION);
		switch (position) {
		case 0:
			if (listAll == null) {
				listAll = new ArrayList<MusicInfo>(MusicProber.instance(context
						.getContentResolver()).getMusicList());
				listAll.addAll(getMusicListFromDB(musicSQLiteHelper, "usb"));
			}
			listCur = listAll;
			
			break;
		case 1:
			Toast.makeText(context, "Long click item to switch display mode",
					Toast.LENGTH_SHORT).show();
			if (listFav == null) {
				listFav = new ArrayList<MusicInfo>(
						getMusicListFromDB(musicSQLiteHelper, "favorites"));
			}
			listCur = listFav;
			listView.setOnItemLongClickListener(favoriteMusicLongClickListener);
			break;
		case 2:
			if (listOnline == null) {
				listOnline = new ArrayList<MusicInfo>();
			}
			listCur = listOnline;
			if (downloadMusic) {
				downloadMusic = false;
				break;
			}
			if (mDownloadTask != null && mDownloadTask.getStatus() == Status.RUNNING) {
				break;
			}

			Toast.makeText(context, "Downloading music...Please wait",
					Toast.LENGTH_SHORT).show();
			
			try {
				mDownloadTask.execute(onlineMusic);
				downloadMusic = false;
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			break;

		default:
			break;
		}
		MusicListAdapter adapter = new MusicListAdapter(context, listCur);
		listView.setAdapter(adapter);
	}
	
	private static List<MusicInfo> getMusicListFromDB(MusicSQLiteHelper helper, String table)
	{
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		try {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.query(table, null, null, null, null, null, null);
			while(cursor.moveToNext()){
				MusicInfo info = new MusicInfo();
				info.setId(cursor.getLong(cursor.getColumnIndex("mid")));
				info.setTitle(cursor.getString(cursor.getColumnIndex("title")));
				info.setAlbum(cursor.getString(cursor.getColumnIndex("album")));
				info.setArtist(cursor.getString(cursor.getColumnIndex("artist")));
				info.setDuration(cursor.getInt(cursor.getColumnIndex("duration")));
				info.setSize(cursor.getLong(cursor.getColumnIndex("size")));
				info.setUrl(cursor.getString(cursor.getColumnIndex("url")));
				list.add(info);
			}
			db.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return list;
	}
	
	public class MusicOnItemClickListener implements AdapterView.OnItemClickListener
	{

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			String urlString = null;
			// TODO Auto-generated method stub
			try {
				urlString = listCur.get(arg2).getUrl();
				File file = new File(urlString);
				if (!file.exists()) {
					Toast.makeText(getApplicationContext(),
							urlString + " does not exists", Toast.LENGTH_SHORT).show();
					return;
				}

				Intent mIntent = new Intent();
				mIntent.setClass(MusicListActivity.this, MusicPlayActivity.class);
				startActivity(mIntent);
				
				Intent intent = new Intent();
				intent.setClass(MusicListActivity.this, MusicService.class);
				intent.setAction(ACTION_PLAY);
				intent.putExtra("Url", listCur.get(arg2).getUrl());
				intent.putExtra("position", arg2);
				startService(intent);
				
				mMusicService.setMusicList(listCur);
				
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		
	}
	
	public class FavoriteMusicLongClickListener implements AdapterView.OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long arg3) {
			curArtist = listCur.get(position).getArtist();
			new AlertDialog.Builder(MusicListActivity.this).setTitle("Tips")
			.setMessage("Only display musics from " + curArtist + " ?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					specificArtistMusic = true;
					listFav = new ArrayList<MusicInfo>(
							getMusicListFromDB(musicSQLiteHelper, "favorites"));
					List<MusicInfo> tempList = new ArrayList<MusicInfo>(listFav);
					for (MusicInfo music : tempList) {
						if (!music.getArtist().equals(curArtist)) {
							listFav.remove(music);
						}
					}
					initListView(MusicListActivity.this, 1);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					specificArtistMusic = false;
					listFav = new ArrayList<MusicInfo>(
							getMusicListFromDB(musicSQLiteHelper, "favorites"));
					initListView(MusicListActivity.this, 1);
				}
			})
			.show();
			
			return true;
		}
		
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// TODO Auto-generated method stub
			return super.instantiateItem(container, position);
		}
	}

	public static class DummySectionFragment extends Fragment {

		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = mFragmentView[getArguments().getInt(ARG_SECTION_NUMBER)]
					= inflater.inflate(
					R.layout.fragment_music_list_dummy, container, false);
			if (curPosion == 0) {
				initListView(getActivity(), curPosion);
			}
			
			return rootView;
		}

	}
	
	public class UsbMusicFoundReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(ACTION_USB_SCAN_COMPLETED)) {
				if (curPosion == 0) {
					listAll = new ArrayList<MusicInfo>(MusicProber.instance(context
							.getContentResolver()).getMusicList());
					listAll.addAll(getMusicListFromDB(musicSQLiteHelper, "usb"));
					initListView(context, curPosion);
				}
				Toast.makeText(context,
						"Found " + intent.getIntExtra("number", 0) + " music files in usb",
						Toast.LENGTH_SHORT).show();
			}
			else if (intent.getAction().equals(ACTION_MUSIC_DOWNLOADED)) {
				if(curPosion == 2){
					listOnline = mDownloadTask.getDownloadList();
					downloadMusic = true;
					initListView(context, curPosion);
				}
			}
			else if (intent.getAction().equals(ACTION_SET_FAV)) {
				listFav = new ArrayList<MusicInfo>(
						getMusicListFromDB(musicSQLiteHelper, "favorites"));
				if (specificArtistMusic) {
					List<MusicInfo> tempList = new ArrayList<MusicInfo>(listFav);
					for (MusicInfo music : tempList) {
						if (!music.getArtist().equals(curArtist)) {
							listFav.remove(music);
						}
					}
				}
				if (curPosion == 1) {
					initListView(context, curPosion);
				}
			}
		}
		
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
			musicImageView.setVisibility(View.GONE);
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
				titleTextView.setText(mMusicService.getMusicTitle());
				playButton.setBackgroundResource(R.drawable.img_appwidget_pause);
				artistTextView.setText(mMusicService.getMusicArtist());
			}
		}
		
	}

}

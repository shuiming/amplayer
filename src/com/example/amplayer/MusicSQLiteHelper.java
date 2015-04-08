package com.example.amplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MusicSQLiteHelper extends SQLiteOpenHelper {

	public MusicSQLiteHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("create table if not exists favorites("
				+ "id integer primary key,"
				+ "mid integer,"
				+ "title varchar(255),"
				+ "album varchar(255),"
				+ "duration integer,"
				+ "size integer,"
				+ "artist varchar(255),"
				+ "url varchar(255),"
				+ "unique(url))");
		db.execSQL("create table if not exists usb("
				+ "id integer primary key,"
				+ "mid integer,"
				+ "title varchar(255),"
				+ "album varchar(255),"
				+ "duration integer,"
				+ "size integer,"
				+ "artist varchar(255),"
				+ "url varchar(255),"
				+ "unique(url))");
//		db.execSQL("create table if not exists usb like favorites");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

}

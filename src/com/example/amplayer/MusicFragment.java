package com.example.amplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MusicFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("", "**************88onCreateView**************");
		View rootView = inflater.inflate(
				R.layout.fragment_music_list_dummy, container, false);
		return rootView;
	}

}

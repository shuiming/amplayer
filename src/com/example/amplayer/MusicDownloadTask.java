package com.example.amplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.amplayer.MusicProber.MusicInfo;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Environment;

public class MusicDownloadTask extends AsyncTask<String, Void, List<MusicInfo>> {

	private static final String ACTION_MUSIC_DOWNLOADED = "amplayer.music_downloaded";
	private Context mContext;
	private List<MusicInfo> musicInfos = new ArrayList<MusicInfo>();
	
	public MusicDownloadTask(Context context){
		mContext = context;
	}
	
	public List<MusicInfo> getDownloadList(){
		return musicInfos;
	}
	
	@Override
	protected List<MusicInfo> doInBackground(String... urls) {
		// TODO Auto-generated method stub
		for (String url : urls) {
			downloadMusic(url);
		}
		return musicInfos;
	}
	
	@Override
	protected void onPostExecute(List<MusicInfo> result) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setAction(ACTION_MUSIC_DOWNLOADED);
		mContext.sendBroadcast(intent);
		super.onPostExecute(result);
	}

	private MusicInfo downloadMusic(String musicUrl) {
		MusicInfo info = new MusicInfo();
		String filename  = getDownloadFileName(musicUrl);
		File download = new File(Environment.
				getExternalStorageDirectory().
				getAbsolutePath() + "/Download");
		if(!download.exists()){
			download.mkdir();
		}
		String musicFilePath = download.getPath() + "/" + filename;
		File targetMusicFile = new File(musicFilePath);
		if (targetMusicFile.exists()) {
			return getMusicInfo(musicFilePath);
		}
		HttpURLConnection con = null;
		try {
			URL url = new URL(musicUrl);
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5 * 1000);
			con.setReadTimeout(10 * 1000);
			con.setRequestMethod("GET");
			//con.setDoInput(true);
			//con.setDoOutput(true);
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream isInputStream = con.getInputStream();
				saveToFile(musicFilePath, isInputStream);
				return getMusicInfo(musicFilePath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return info;
	}
	
	public void saveToFile(String fileName, InputStream in) throws IOException { 
	      FileOutputStream fos = null;    
	      BufferedInputStream bis = null;       
	      int BUFFER_SIZE = 1024; 
	      byte[] buf = new byte[BUFFER_SIZE];    
	      int size = 0;    
	      bis = new BufferedInputStream(in);    
	      fos = new FileOutputStream(fileName);    
	      while ( (size = bis.read(buf)) != -1)     
	        fos.write(buf, 0, size);    
	      fos.close();    
	      bis.close();
	 }
	
	public String getDownloadFileName(String Url){
		String filename = "";
        boolean isok = false;
        // 从UrlConnection中获取文件名称
        try {
            URL myURL = new URL(Url);

            URLConnection conn = myURL.openConnection();
            if (conn == null) {
                return null;
            }
            Map<String, List<String>> hf = conn.getHeaderFields();
            if (hf == null) {
                return null;
            }
            Set<String> key = hf.keySet();
            if (key == null) {
                return null;
            }

            for (String skey : key) {
                List<String> values = hf.get(skey);
                for (String value : values) {
                    String result;
                    try {
                        result = new String(value.getBytes("ISO-8859-1"), "GBK");
                        int location = result.indexOf("filename");
                        if (location >= 0) {
                            result = result.substring(location
                                    + "filename".length());
                            filename = result
                                    .substring(result.indexOf("=") + 1);
                            isok = true;
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }// ISO-8859-1 UTF-8 gb2312
                }
                if (isok) {
                    break;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 从路径中获取
        if (filename == null || "".equals(filename)) {
            filename = Url.substring(Url.lastIndexOf("/") + 1);
        }
        return filename.replace("\"", "");
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
			musicInfos.add(info);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return info;
	}
}

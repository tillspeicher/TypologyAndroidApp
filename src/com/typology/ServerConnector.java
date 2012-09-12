package com.typology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class ServerConnector extends AsyncTask<String, Void, TreeMap<Integer, String>> {
	private static Gson json = new Gson();
	private static String URL = "http://complet.typology.de/complet.php";
	private static String DEVICE_ID;
	
	public static boolean lastResultsEmpty = false;
	
	public static void setDeviceId(String id) {
		DEVICE_ID = id;
	}
	
	public static void getSuggestions(String[] words, String letters, String lastSelection, int elementsCnt) {
		if (connectionNecessary(words, letters, elementsCnt)) {
			String json = toJson(words, letters, DEVICE_ID, lastSelection);
	        new ServerConnector().execute(json);
		}
	}

	@Override
	protected TreeMap<Integer, String> doInBackground(String... jsondata) {
		String result = "";
		if (jsondata[0] != null && jsondata[0].length() > 0) {
			try {
				HttpParams params = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(params, 1000);
				HttpConnectionParams.setSoTimeout(params, 700);
				DefaultHttpClient httpClient = new DefaultHttpClient(params);
				HttpPost httpPost = new HttpPost(URL);
				
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
				nameValuePairs.add(new BasicNameValuePair("json", jsondata[0]));
				httpPost.setEntity(new UrlEncodedFormEntity((nameValuePairs), HTTP.UTF_8));
				try {
					HttpResponse response = httpClient.execute(httpPost);
					BufferedReader rd = new BufferedReader(new InputStreamReader(
							response.getEntity().getContent()), 8192);
					String line = "";
					while ((line = rd.readLine()) != null) {
						if (line.length() > 0) {
							result = line;
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
				}		   
			} catch (Exception e) {
				e.printStackTrace();
			}	
			 
		}
		TreeMap<Integer, String> suggestions = fromJson(result);
		return suggestions;
	}
	
	@Override
	protected void onPostExecute(TreeMap<Integer, String> result) {	
//		Log.i("updateCandidates", "recieved json: " + result);
		
		if (result != null && result.isEmpty() == false) {
			List<String> list = new ArrayList<String>();
	        for(Integer key : result.keySet()){
	        	list.add(result.get(key));
	        }
	        SoftKeyboard.mSuggestions.setSuggestions(list);
		}
		if (result.size() < 10) {
			lastResultsEmpty = true;
		} else {
			lastResultsEmpty = false;
		}
	}
	
	public static String toJson(String[] words, String letters, String ID, String lastSelection) {
		int actualLength = 0;
		boolean force_primitive = false;
		for (int i = 0; i < words.length; i++) {
			if (!words[i].equals("")) {
				actualLength++;
			}
		}
		String[] strippedWords = new String[actualLength];
		System.arraycopy(words, 0, strippedWords, 0, actualLength);
		if (strippedWords.length == 0) {
			force_primitive = true;
		}
		CompletData cdat = new CompletData(strippedWords, letters, ID, force_primitive, lastSelection);
		String jsondata = json.toJson(cdat, CompletData.class);
		
//		Log.i("updateCandidates", "send jsondata: " + jsondata);
		
		return jsondata;
	}
	
	public static TreeMap<Integer, String> fromJson(String raw) {
		TreeMap<Integer, String> suggestions = new  TreeMap<Integer, String>();
		if (raw != null && raw.length() > 0) {
			try {
				CompletData res = json.fromJson(raw, CompletData.class);
		        for(Integer key : res.result.keySet()){
		        	suggestions.put(key, res.result.get(key));
		        }
			}catch (JsonSyntaxException e) {
				e.printStackTrace();
			}
	        
		}
		return suggestions;
	}
	
	private static boolean connectionNecessary(String[] words, String letters, int elementsCnt) {
		boolean result = true;
		if (letters.length() == 0) {
			result = false;
		}
		if (elementsCnt >= 5) {
			result = false;
		}
		if (lastResultsEmpty) {
			result = false;
		}
		return result;
	}
}

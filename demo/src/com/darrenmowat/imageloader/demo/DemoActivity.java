package com.darrenmowat.imageloader.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.darrenmowat.imageloadersample.R;

public class DemoActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        new PugListAsyncTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_demo, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private class PugListAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {

		static final String PUG_ME_URL = "http://pugme.herokuapp.com/bomb?count=" + 30;

		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(PUG_ME_URL).openConnection();
				conn.setRequestProperty("Accept", "application/json");
				InputStream is = conn.getInputStream();

				StringBuilder sb = new StringBuilder();
				BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					sb.append(line);
				}
				try {
					is.close();
				} catch (IOException e) {
				}

				String response = sb.toString();
				JSONObject document = new JSONObject(response);

				JSONArray pugsJsonArray = document.getJSONArray("pugs");
				ArrayList<String> pugUrls = new ArrayList<String>(pugsJsonArray.length());

				for (int i = 0, z = pugsJsonArray.length(); i < z; i++) {
					pugUrls.add(pugsJsonArray.getString(i));
				}

				pugUrls.addAll(pugUrls);
				pugUrls.addAll(pugUrls);
				pugUrls.addAll(pugUrls);
				
				Collections.shuffle(pugUrls);
				
				return pugUrls;

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(result);

			ImageAdapter adapter = new ImageAdapter(result, DemoActivity.this);
			setListAdapter(adapter);
		}

	}

}

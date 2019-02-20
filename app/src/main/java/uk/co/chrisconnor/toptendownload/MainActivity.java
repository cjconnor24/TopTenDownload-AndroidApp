package uk.co.chrisconnor.toptendownload;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // CONSTS FOR LOGGING AND PASSING DATA TO BUNDLE
    private static final String TAG = "MainActivity";
    public static final String STATE_URL = "feedUrl";
    public static final String STATE_LIMIT = "feedLimit";

    private ListView listApps;
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private String feedCacheUrl = "INVALIDATED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // GETS THE LIST VIEW FROM THE LAYOUT
        listApps = (ListView) findViewById(R.id.xmlListView);

        // CHECK TO SEE IF DATA ALREADY SET, IF SO PULL IT FROM THE BUNDLE
        if(savedInstanceState !=null){
            feedUrl = savedInstanceState.getString(STATE_URL);
            feedLimit = savedInstanceState.getInt(STATE_LIMIT);
        }

        downloadUrl(String.format(feedUrl, feedLimit));

    }

    // CREATE TOP MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if (feedLimit == 10) {
            menu.findItem(R.id.mnu10).setChecked(true);
        } else {
            menu.findItem(R.id.mnu25).setChecked(true);
        }
        return true;
    }

    // ACTIONS FOR WHEN MENU ITEM SELECTED
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mnuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " setting feedLimit to " + feedLimit);
                } else {
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " feedLimit unchanged");
                }
                break;
            case R.id.mnuRefresh:
                feedCacheUrl = "INVALIDATED";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        downloadUrl(String.format(feedUrl, feedLimit));
        return true;

    }

    // SAVE THE DATA TO BUNDLE SO NOT LOST ON ORIENTATION CHANGE
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_URL, feedUrl);
        outState.putInt(STATE_LIMIT, feedLimit);
        super.onSaveInstanceState(outState);
    }

    // DOWNLOAD THE XML DATA FROM THE PASSED URL
    private void downloadUrl(String feedUrl) {

        // CHECK TO SEE IF URL IS ALREADY CACHED
        if (!feedUrl.equalsIgnoreCase(feedCacheUrl)) {

            Log.d(TAG, "downloadUrl: starting Async Task");
            DownloadData downloadData = new DownloadData();
            downloadData.execute(feedUrl);
            feedCacheUrl = feedUrl;
            Log.d(TAG, "downloadUrl: done");

        } else {
            Log.d(TAG, "downloadUrl: URL NOT CHANGED");
        }

    }

    // ASYNC CLASS TASK TO DOWNLOAD DATA ON SEPARATE THREAD
    private class DownloadData extends AsyncTask<String, Void, String> {

        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: paramater is " + s);

            // GET THE XML DATA PARSED TO OBJECTS
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<>(MainActivity.this,R.layout.list_view,parseApplications.getApplications());
//            listApps.setAdapter(arrayAdapter);

            // CREATE AN INSTANCE OF THE NEW CUSTOM FEED ADAPTER AND SET THE SOURCE
            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);

        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            String rssFeed = downloadXML(strings[0]);
            if (rssFeed == null) {
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        /**
         * Open up the connection
         * @param urlPath
         * @return
         */
        private String downloadXML(String urlPath) {
            StringBuilder xmlResult = new StringBuilder();

            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: The response code was " + response);


                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                int charsRead;
                char[] inputBuffer = new char[500]; // CREATE A BUFFER

                // START TO LOOP
                while (true) {

                    // ADD TO BUFFER
                    charsRead = reader.read(inputBuffer);
                    if (charsRead < 0) {
                        break; // BREAK IF COMPLETE
                    }
                    if (charsRead > 0) {
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();

                return xmlResult.toString();

            } catch (MalformedURLException e) {
                Log.e(TAG, "downloadXML: Invalid URL" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "downloadXML: IO Exception reading data:" + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "downloadXML: cannot access internet: Needs permission? " + e.getMessage());
            }


            return null;

        }

    }

}

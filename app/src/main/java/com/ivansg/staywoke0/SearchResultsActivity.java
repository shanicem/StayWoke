package com.ivansg.staywoke0;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class SearchResultsActivity extends MainActivity {
    DatabaseTable db = new DatabaseTable(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // Find matches in database for query
            Cursor c = db.getStopMatches(query, null);
            // Find ListView to populate to populate with search results
            final ListView lvItems = (ListView) findViewById(R.id.list);
            // Setup cursor adapter
            final TodoCursorAdapter todoAdapter = new TodoCursorAdapter(this, c);
            // Attach cursor adapter to the ListView
            lvItems.setAdapter(todoAdapter);

            final Context that = this;
            // ListView Item Click Listener
            lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // ListView Clicked item value
                    Cursor o = (Cursor) lvItems.getItemAtPosition(i);

                    Intent intent = new Intent(view.getContext(), DistanceToDestinationActivity.class);
                    intent.putExtra("_id", o.getString(0));
                    intent.putExtra("STOP_LAT", o.getString(1));
                    intent.putExtra("STOP_LON", o.getString(2));
                    startActivity(intent);
                }
            });
        }
    }
}

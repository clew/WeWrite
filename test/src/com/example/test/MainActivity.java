package com.example.test;

import java.util.ArrayList;
import java.util.Random;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class MainActivity extends ListActivity  {

private String userName;	
	
static final String[] MOBILE_OS = 
      new String[] { "First Option", "Second", "abcdefghijklmnopqrstuvwxyz", "Just another test"};

private static final String TAG = "MainActivity";
  
	ArrayList<String> writes = new ArrayList<String>();
  
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
            
        setContentView(R.layout.activity_main);                    
        setListAdapter(new listadapter(this, writes));
        
        Random rand = new Random();
        userName = "lewc" + rand.nextInt(Integer.MAX_VALUE);
        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new OnItemLongClickListener() 
        {
        	@Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                int arg2, long arg3)
            {
        		writes.remove(arg2);
        		setListAdapter(new listadapter(MainActivity.this, writes));
        		return true;
            }
        });
       
    }
    
    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
   
      //get selected items
      String selectedValue = (String) getListAdapter().getItem(position);
      Intent i = new Intent(getBaseContext(), wewrite.class);
      i.putExtra("str", selectedValue);
      i.putExtra("loc", position);
      i.putExtra("mode", "rejoin");
      i.putExtra("userName", userName);
      startActivityForResult(i,1);
   
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent i = new Intent(getBaseContext(), wewrite.class); 
        i.putExtra("str", "");
        i.putExtra("loc", writes.size()-1);
        i.putExtra("userName", userName);
        
        switch (item.getItemId()) {
            case R.id.add:
                i.putExtra("mode", "create");
                startActivityForResult(i,2);
                return true;
            case R.id.join_session:
            	i.putExtra("mode", "join");
            	startActivityForResult(i,3);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Bundle extras = intent.getExtras();
    
        switch(requestCode) {
            case 1:
                writes.set(extras.getInt("loc"), extras.getString("str"));
                setListAdapter(new listadapter(this, writes));
                
                break;
            case 2:
                writes.add(extras.getString("str"));
                setListAdapter(new listadapter(this, writes));
              break;
              
        
        }
    } 
}

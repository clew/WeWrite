package com.example.test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Random;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.test.Event.WeWriteEvent;

import edu.umich.imlc.collabrify.client.CollabrifyClient;
import edu.umich.imlc.collabrify.client.CollabrifyListener;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;

public class wewrite extends Activity 
{

	public class participant extends Object {
		public long id;
		public int cursor_pos;
	}
	
	
    public class MyEvent {
        int type;
        String text;
        int start;
        int replacedTextLength;

        long who;
        int location;

        MyEvent(int type, String text, int start, int replacedTextLength){
            this.type = type;
            this.text = text;
            this.start = start;
            this.replacedTextLength = replacedTextLength;
        }

        MyEvent(int type, long who, int location) {
            this.type = type;
            this.who = who;
            this.location = location;
        }
    }

	public ArrayList<participant> participantList = new ArrayList<participant>();
	
	/* WeWrite Variables */
	private ArrayList<String> undolist = new ArrayList<String>();
	private ArrayList<String> redolist = new ArrayList<String>();

	EditTextSelectionWatch txt;
	int location;
	private String temp;
	private	String temp2;
	private Button undobutton;
	private Button redobutton;
	boolean no;	
	public boolean isTextChange;
	
	/* Collabrify Variables */
	static String TAG = "WeWrite";	
	public CollabrifyClient myClient;


	ArrayList<String> tags = new ArrayList<String>();
	ArrayList<Integer> myRegIDs = new ArrayList<Integer>();
	long sessionId;
	String sessionName;
	String userName;
	ByteArrayInputStream baseFileBuffer;
	ByteArrayOutputStream baseFileReceiveBuffer;
	boolean getLatestEvent = false;
	long globalOrder = 0;
	boolean is_user_input = true;
	long userID;
	boolean unconfirmed_event = true;
	int latest_unconfirmed_event = 0;
	ArrayList<MyEvent> reapplyQueue = new ArrayList<MyEvent>();
	int earliestStartSoFar = Integer.MAX_VALUE;
	String last_correct_state = "";
	Menu menu;
	boolean in_reapply = false;
	
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);
		setContentView(R.layout.text);
		
		
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		/* WeWrite Edit Text Operations */
		txt = (EditTextSelectionWatch) findViewById(R.id.editText1);
		txt.setWeWrite(this);
		
		undobutton = (Button) findViewById(R.id.undo);
		redobutton = (Button) findViewById(R.id.redo);

		Bundle extras = getIntent().getExtras();
		temp = extras.getString("str");
		location = extras.getInt("loc");
		
		no = true;
		is_user_input = true;

		txt.addTextChangedListener(new TextWatcher() 
		{
			@Override
			public void afterTextChanged(Editable s) {
				isTextChange = false;
				if(no)
				{
					
					if(temp.equals(txt.getText().toString())){}
					
					else
					{
						undolist.add(temp);
					
						temp = new String(txt.getText().toString());					
						redolist.clear();
																	
						undobutton.setEnabled(true);
						redobutton.setEnabled(false);
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				isTextChange = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
				
				if(is_user_input){
					WeWriteEvent packet = WeWriteEvent.newBuilder()
						.setType(WeWriteEvent.EventType.CHANGETEXT)
						.setNewText(s.subSequence(start, start+count).toString())
						.setTextStart(start)
						.setReplacedTextLength(before)
						.build();
					
					try {
						int regID = myClient.broadcast(packet.toByteArray(), "myTextChange");
						unconfirmed_event = true;
		    			myRegIDs.add(regID);
						Log.i(TAG, "from onBroadcast regID: " + regID);
					}
					catch (CollabrifyException e) {
						Log.e(TAG, "Error Broadcasting Message");
						e.printStackTrace();
					}
				}
			}
		});

		undobutton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View arg0) 
			{	
				String temp2 = new String(undolist.get(undolist.size()-1));
			
				undolist.remove(undolist.size()-1);							
				redolist.add(temp);
				
				no = false;					
				txt.setText(temp2);
				temp = temp2;
				txt.setSelection(txt.getText().length());
				no = true;
				
				redobutton.setEnabled(true);
									
				if(undolist.size() == 0)
				{
					undobutton.setEnabled(false);
				}							
			}
		});

		redobutton.setOnClickListener(new OnClickListener() 
		{ 
			@Override
			public void onClick(View arg0) {
				
				String temp2 = new String(redolist.get(redolist.size()-1));
			
				redolist.remove(redolist.size()-1);		
				undolist.add(temp);
				
				no = false;
				txt.setText(temp2);		
				temp = temp2;
				txt.setSelection(txt.getText().length());
				no = true;
				
				undobutton.setEnabled(true);
				
				txt.setSelection(txt.getText().length());
				
				if(redolist.size() == 0){
					redobutton.setEnabled(false);
				}		
			}
		});

		/* Collabrify Setup */
		tags.add("rawr");
		tags.add("moo");
		
		CollabrifyListener collabrifyListener = new wewriteCollabrifyAdapter(this);
		try
		{
			Random rand = new Random();
			String mode = extras.getString("mode");	
			userName = extras.getString("userName");
			myClient = new CollabrifyClient(this, "blah@poop.com", userName,
					"441fall2013@umich.edu", "XY3721425NoScOpE", getLatestEvent,
					collabrifyListener);
			
			if (mode.equals("create"))
			{
				/* Create a Session */
				sessionName = "lewc_vakayash_" + rand.nextInt(Integer.MAX_VALUE);
				baseFileBuffer = new ByteArrayInputStream(txt.toString().getBytes());
				
				myClient.createSessionWithBase(sessionName, tags, null, 0);
				Log.i(TAG, "Session name is " + sessionName);
			
			} 
			else if (mode.equals("join"))
			{
				myClient.requestSessionList(tags);
				Log.v(TAG, "Joined Session: " + sessionName + " under " + userName);
			}
			else if (mode.equals("rejoin"))
			{
				Log.v(TAG, "PUUUUSH THE BUTTON");
			}
			else 
			{
				Log.v(TAG, "HOW DID WE GET HERE WITHOUT JOIN OR CREATE!!!");
			}

			participant new_participant = new participant();
			new_participant.cursor_pos = 0;
			new_participant.id = myClient.currentSessionParticipantId();
			participantList.add(new_participant);
			
		}
		catch (CollabrifyException e)
		{
			Log.e(TAG, "error", e);
			e.printStackTrace();
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		onBackPressed();
		return true;
	}

	@Override
	public void onBackPressed() {
				
		Intent returnIntent = new Intent();
		String title_str = "Session: " + sessionName + "\nUser Display Name: " + userName;
		returnIntent.putExtra("str", title_str);
		returnIntent.putExtra("loc", location);
		
		setResult(RESULT_OK, returnIntent);
		finish();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		try
		{
			myClient.leaveSession(true);
		} 
		catch (CollabrifyException e) 
		{
			Log.e(TAG, "error cant leave session in onBackPressed", e);
			e.printStackTrace();			
		}	
	}
	
	public int searchParticipantList(long id){
		for (int i = 0; i < participantList.size(); i++){
			participant p = participantList.get(i);
			if (id == p.id){
				return i;
			}
		}
		Log.e(TAG, "Error participant_id " + id + " is not in the array list");
		return -1;
	}
	
	public void participantListCursorChange(long id, int pos)
	{
		int index = searchParticipantList(id);
		if (index < 0){
			return;
		}
		participant p = participantList.get(index);
		p.cursor_pos = pos;
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.wewrite_actionbar, menu);
		MenuItem session_name = menu.findItem(R.id.session_name);
		session_name.setTitle(sessionName);
	    return super.onCreateOptionsMenu(menu);
	}
}
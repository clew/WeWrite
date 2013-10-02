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
	private ArrayList<MyEvent> undolist = new ArrayList<MyEvent>();
	private ArrayList<MyEvent> redolist = new ArrayList<MyEvent>();

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
	String current_state = "";
	boolean is_undo_change = false;
	
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
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
				
				isTextChange = true;
				if (!is_undo_change && is_user_input){
					String tmp_string = s.toString().substring(start, start+count);
					MyEvent undo_event = new MyEvent(1, tmp_string, start, after);
					undolist.add(undo_event);
					redolist.clear();
					undobutton.setEnabled(true);
					redobutton.setEnabled(false);
				}
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
						myClient.broadcast(packet.toByteArray(), "myTextChange");
						unconfirmed_event = true;
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
				MyEvent event = undolist.get(undolist.size()-1);
				undolist.remove(undolist.size()-1);
				
				String s = txt.getText().toString();
				int end = Math.min(event.start + event.replacedTextLength, s.length());
				String redo_text = s.substring(event.start, end);
				MyEvent redo_event = new MyEvent(1,redo_text, event.start, event.text.length());
				redolist.add(redo_event);
				
				is_undo_change = true;
				//Apply Event;
				do_undo_redo_event(event);
				//APPLY EVENT!!!
				int index = Math.min(redo_event.start + redo_event.text.length(), txt.getText().toString().length());
				txt.setSelection(index);
				is_undo_change = false;
				
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
			public void onClick(View view)
			{
				MyEvent event = redolist.get(redolist.size()-1);
				redolist.remove(redolist.size()-1);
				
				String s = txt.getText().toString();
				int end = Math.min(event.start + event.replacedTextLength, s.length());
				String undo_text = s.substring(event.start, end);
				MyEvent undo_event = new MyEvent(1,undo_text, event.start, event.text.length());
				undolist.add(undo_event);
				
				is_undo_change = true;
				//Apply Event;
				do_undo_redo_event(event);
				//APPLY EVENT!!!
				int index = Math.min(undo_event.start + undo_event.text.length(), txt.getText().toString().length());
				txt.setSelection(index);
				
				is_undo_change = false;
				
				undobutton.setEnabled(true);
				if(redolist.size() == 0)
				{
					redobutton.setEnabled(false);
				}
			}			
		});

		/* Collabrify Setup */
		tags.add("reddit");
		tags.add("hji");
		
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
			if (myClient.currentSessionOwner().getId() == userID) {
				myClient.leaveSession(true);				
			} else {
				myClient.leaveSession(false);
			}
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

	public void do_undo_redo_event(MyEvent event) {
		String s = txt.getText().toString();
		final int end = Math.min(event.start + event.replacedTextLength, s.length());
		final int start = Math.min(event.start,  s.length());

		txt.getText().replace(Math.min(start, end), Math.max(start, end), event.text);

		int offset = event.text.length() - event.replacedTextLength;
		for (int i = 0; i < participantList.size(); i++){
			int pos =  participantList.get(i).cursor_pos;
			if (pos > start){
				participantList.get(i).cursor_pos = Math.max(start, pos + offset);
			}
		}
	}
}
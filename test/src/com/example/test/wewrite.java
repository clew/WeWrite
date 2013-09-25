package com.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.test.TextChange.MyTextChange;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.umich.imlc.collabrify.client.CollabrifyAdapter;
import edu.umich.imlc.collabrify.client.CollabrifyClient;
import edu.umich.imlc.collabrify.client.CollabrifyListener;
import edu.umich.imlc.collabrify.client.CollabrifyParticipant;
import edu.umich.imlc.collabrify.client.CollabrifySession;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;

public class wewrite extends Activity 
{

	/* WeWrite Variables */
	private ArrayList<String> undolist = new ArrayList<String>();
	private ArrayList<String> redolist = new ArrayList<String>();

	private EditText txt;
	private Button undobutton;
	private Button redobutton;
	private int location;
	private String temp;
	private	String temp2;
	private boolean no;	

	/* Collabrify Variables */
	private static String TAG = "WeWrite";	
	private CollabrifyClient myClient;

	private CollabrifyListener collabrifyListener;
	private ArrayList<String> tags = new ArrayList<String>();
	private ArrayList<Integer> myRegIDs = new ArrayList<Integer>();
	private long sessionId;
	private String sessionName;
	private String userName;
	private ByteArrayInputStream baseFileBuffer;
	private ByteArrayOutputStream baseFileReceiveBuffer;
	private boolean getLatestEvent = false;
		
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);
		setContentView(R.layout.text);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		/* WeWrite Edit Text Operations */
		txt = (EditText) findViewById(R.id.editText1);
		undobutton = (Button) findViewById(R.id.undo);
		redobutton = (Button) findViewById(R.id.redo);

		Bundle extras = getIntent().getExtras();
		temp = extras.getString("str");
		location = extras.getInt("loc");

		txt.setText(temp);
		txt.setSelection(txt.getText().length());

		no = true;

		txt.addTextChangedListener(new TextWatcher() 
		{
			@Override
			public void afterTextChanged(Editable s) {
				
				if(no){
					
					if(temp.equals(txt.getText().toString())){}
					
					else{
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
				// TODO Auto-generated method stub	
			}

			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
				
				MyTextChange packet = MyTextChange.newBuilder()
					.setNewText(s.subSequence(start, start+count).toString())
					.setStart(start)
					.setReplacedTextLength(before)
					.setNewTextLength(count)
					.build();
				try {
					int regID = myClient.broadcast(packet.toByteArray(), "myTextChange");
	    			myRegIDs.add(regID);
					
				}
				catch (CollabrifyException e) {
					Log.e(TAG, "Error Broadcasting Message");
					e.printStackTrace();
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
									
				if(undolist.size() == 0){
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
	    collabrifyListener = new CollabrifyAdapter()
	    {
	    	
	    	@Override
	    	public void onDisconnect()
	    	{
	    	  Log.i(TAG, "disconnected from " + sessionName + " with " + userName);
	    	}
	    	
	    	@Override
	    	public void onParticipantLeft(CollabrifyParticipant p)
	    	{
	    		Log.v(TAG, "Participant Left " + p.toString());
	    	}
	    	
	    	@Override
	    	public void onParticipantJoined(CollabrifyParticipant p) 
	    	{
	    		Log.v(TAG, "Participant Joined " + p.toString());
	    	}
	    	
	    	@Override
	    	public void onReceiveEvent(final long orderId, int subId,
	    	  String eventType, final byte[] data)
	    	{
	    		Log.i(TAG, "orderID: " + orderId + " regID: " + subId + " " + eventType);
	    		if (myRegIDs.contains(subId)){
	    			Integer i = subId;
	    			myRegIDs.remove(i);
	    		} 
	    		else {
		    		try {
		    			MyTextChange packet = MyTextChange.parseFrom(data);
		    			final String newText = packet.getNewText();
		    			final int start = packet.getStart();
		    			final int replacedTextLength = packet.getReplacedTextLength();
		    			final int newTextLength = packet.getNewTextLength();
		    			location += newTextLength;
		    			no = false;
		    	        runOnUiThread(new Runnable()
		    	        {

		    	          @Override
		    	          public void run()
		    	          {

				    			txt.getText().replace(start, replacedTextLength, newText);    	        	  
		    	          }
		    	        });		    			
		    				
		    	        no = true;
		    		} catch(InvalidProtocolBufferException e) {
		    			Log.e(TAG, "error receiving data");
		    		}
	    		}	
	    	}	    	
	    	
			@Override
			public void onSessionCreated(long id)
			{
				Log.i(TAG, "Session created, id: " + id);
				sessionId = id;
			}

			@Override
			public void onSessionJoined(long maxOrderId, long baseFileSize)
			{
				//May Redo this to use google protocol buffer!
				Log.i(TAG, "Session Joined");
				if( baseFileSize > 0 )
				{
					  //initialize buffer to receive base file
					  baseFileReceiveBuffer = new ByteArrayOutputStream((int) baseFileSize);
				}
		
			}
			
			@Override
			public void onSessionEnd(long id)
			{
				//filler
				Log.i(TAG, sessionName + ", " + sessionId + " has ended");				
			}
			
			@Override
			public void onError(CollabrifyException e)
			{
				//Filler
				Log.e(TAG, "error", e);
			}

			public void onReceiveSessionList(final List<CollabrifySession> sessionList)
			{
				if( sessionList.isEmpty() )
				{
					Log.i(TAG, "No session available");
					return;
				}
				List<String> sessionNames = new ArrayList<String>();
				for( CollabrifySession s : sessionList )
				{
					sessionNames.add(s.name());
				}
				final AlertDialog.Builder builder = new AlertDialog.Builder(
					wewrite.this);
				builder.setTitle("Choose Session").setItems(
				sessionNames.toArray(new String[sessionList.size()]),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						try
						{
							sessionId = sessionList.get(which).id();
							sessionName = sessionList.get(which).name();
							myClient.joinSession(sessionId, null);
					
							//txt.setText(baseFileReceiveBuffer.toString());
						}
						catch( CollabrifyException e )
						{
							Log.e(TAG, "error", e);
						}
					}
				});
				
				runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						builder.show();
					}
				});
			}
			
			@Override
			public byte[] onBaseFileChunkRequested(long currentBaseFileSize)
			{
				// read up to max chunk size at a time
				byte[] temp = new byte[CollabrifyClient.MAX_BASE_FILE_CHUNK_SIZE];
				int read = 0;
				try
				{
					read = baseFileBuffer.read(temp);
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}
				if( read == -1 )
				{
					return null;
				}
				if( read < CollabrifyClient.MAX_BASE_FILE_CHUNK_SIZE )
				{
					// Trim garbage data
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					bos.write(temp, 0, read);
					temp = bos.toByteArray();
				}
				return temp;
			}

			@Override
			public void onBaseFileChunkReceived(byte[] baseFileChunk)
			{
				try
				{
					if( baseFileChunk != null )
					{
						baseFileReceiveBuffer.write(baseFileChunk);
					}
					else
					{
						Log.v(TAG, baseFileReceiveBuffer.toString());
						baseFileReceiveBuffer.close();
					}
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}
			}

			@Override
			public void onBaseFileUploadComplete(long baseFileSize)
			{
				Log.v(TAG, sessionName);
				try
				{
					baseFileBuffer.close();
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}
			}
		};

		tags.add("lewc");
		tags.add("vakayash");
		
		try
		{
			Random rand = new Random();
			String mode = extras.getString("mode");
			userName = "lewc" + rand.nextInt(Integer.MAX_VALUE);
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
		
		try
		{
			myClient.leaveSession(true);
		} 
		catch (CollabrifyException e) 
		{
			Log.e(TAG, "error", e);
			e.printStackTrace();			
		}
		
		Intent returnIntent = new Intent();
		String title_str = "Session: " + sessionName + "\nUser Display Name: " + userName;
		returnIntent.putExtra("str", title_str);
		returnIntent.putExtra("loc", location);
		
		setResult(RESULT_OK, returnIntent);
		finish();
	}


}
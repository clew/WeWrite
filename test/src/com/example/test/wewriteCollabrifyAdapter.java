package com.example.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.example.test.Event.WeWriteEvent;
import com.example.test.Event.WeWriteEvent.EventType;
import com.example.test.wewrite.MyEvent;
import com.example.test.wewrite.participant;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.umich.imlc.collabrify.client.CollabrifyAdapter;
import edu.umich.imlc.collabrify.client.CollabrifyClient;
import edu.umich.imlc.collabrify.client.CollabrifyParticipant;
import edu.umich.imlc.collabrify.client.CollabrifySession;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;


public class wewriteCollabrifyAdapter extends CollabrifyAdapter 
{
	private wewrite w;
	Integer UI_lock = 0;
	
	public wewriteCollabrifyAdapter(wewrite w){ 
		this.w = w;
	}

	@Override
	public void onDisconnect()
	{
	  Log.i(wewrite.TAG, "disconnected from " + w.sessionName + " with " + w.userName);
	}
	
	@Override
	public void onParticipantJoined(CollabrifyParticipant p) 
	{
		participant new_participant = w.new participant();
		new_participant.id = p.getId();
		new_participant.cursor_pos = 0;
		w.participantList.add(new_participant);
		Log.v(wewrite.TAG, "Participant Joined " + new_participant.id);
	}
	
	@Override
	public void onParticipantLeft(CollabrifyParticipant p)
	{
		int index = w.searchParticipantList(p.getId());
		w.participantList.remove(index);
		Log.v(wewrite.TAG, "Participant Left " + p.getId());
	}
	
	@Override
	public synchronized void onReceiveEvent(final long orderId, int subId,
	  String eventType, final byte[] data)
	{
		
		Log.d(wewrite.TAG, "received!! " + orderId + " " + subId);
	    MyEvent recv_event;
		try {
		    WeWriteEvent packet;
			packet = WeWriteEvent.parseFrom(data);
		    if (packet.getType() == WeWriteEvent.EventType.MOVECURSOR){
		        recv_event = w.new MyEvent(0, packet.getParticipantID(), packet.getCursorStart());
		    } else {
		        String text = new String(packet.getNewText());
		        recv_event = w.new MyEvent(1, text, packet.getTextStart(), packet.getReplacedTextLength());
		    }
		    
			if (subId < 0) {
				applyEvent(recv_event);
				w.globalOrder = orderId;
			} else {
				if (w.unconfirmed_event) {
					//check if invalidated
					if (recv_event.type == 0){
						if (recv_event.location < w.earliestStartSoFar){
							Log.i(wewrite.TAG, "moo added to reapplyQueue");
							w.reapplyQueue.add(recv_event);
							return;
						} else {
							reapplyEvents(recv_event);
						}
					} else {
						if (recv_event.start + Math.max(recv_event.replacedTextLength, recv_event.text.length()) < w.earliestStartSoFar) {
							Log.i(wewrite.TAG, "moo added to reapplyQueue");
							w.reapplyQueue.add(recv_event);
							return;
						} else {
							reapplyEvents(recv_event);
						}
					}
				} else {
					applyEvent(recv_event);
				}
			}
		} catch (InvalidProtocolBufferException e) {
			Log.e(wewrite.TAG, "Error receiveing data");
			e.printStackTrace();
		}
		

	}	    	

	public void reapplyEvents(MyEvent event) {
		Log.i(wewrite.TAG, "in reapply");
		w.in_reapply = true;
		Log.d(wewrite.TAG, "Before: " + w.txt.getText().toString() + "last correct state: " + w.last_correct_state);
		synchronized(UI_lock) {
			Runnable myRunnable = new Runnable() {
				@Override
				public void run()
				{	
					synchronized(UI_lock){
						w.is_user_input = false;
						w.txt.setText(w.last_correct_state);
						Log.d(wewrite.TAG, "CHANGED UI TO THE THE CORRECT STATE!");
			    		w.is_user_input = true;
			    		UI_lock.notifyAll();
					}
				}			
			};
			w.runOnUiThread(myRunnable);
			try {
				UI_lock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}



		Log.d(wewrite.TAG, "After: " + w.txt.getText().toString() + "last correct state: " + w.last_correct_state);	

		while (!w.reapplyQueue.isEmpty()){
			Log.i(wewrite.TAG, "Infinite Loop?");
			applyEvent(w.reapplyQueue.get(0));
			w.reapplyQueue.remove(0);
		}
		applyEvent(event);
		w.earliestStartSoFar = Integer.MAX_VALUE;
		w.unconfirmed_event = false;
		w.last_correct_state = w.txt.getText().toString();
		Log.d(wewrite.TAG, "NEW CORRECTED STATE" + w.last_correct_state);
		w.in_reapply = false;
		return;
	}
	
	public void applyEvent(final MyEvent event){
		if (event.type == 0) {
			int index = w.searchParticipantList(event.who);
			event.location = Math.min(event.location, w.txt.getText().toString().length());
			event.location = Math.max(event.location, 0);
			w.participantList.get(index).cursor_pos = event.location;	
		} else {
			String s = w.txt.getText().toString();
			Log.i(wewrite.TAG, "STRING S is " + s);
			final int end = Math.min(event.start + event.replacedTextLength, s.length());
			final int start = Math.min(event.start,  s.length());

			synchronized(UI_lock) {
				Runnable myRunnable = new Runnable(){
					@Override
					public void run()
					{
						synchronized(UI_lock){
							w.is_user_input = false;
							Log.i(wewrite.TAG, start + " " + end + " " + w.txt.getText().toString() + " " + event.text);
				    		w.txt.getText().replace(Math.min(start, end), Math.max(start, end), event.text);
				    		w.is_user_input = true;
				    		UI_lock.notifyAll();
						}
					}
				};
				w.runOnUiThread(myRunnable);
				try {
					UI_lock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
						
    		int offset = event.text.length() - event.replacedTextLength;
    		for (int i = 0; i < w.participantList.size(); i++){
    			int pos =  w.participantList.get(i).cursor_pos;
    			if (pos > start){
    				w.participantList.get(i).cursor_pos = Math.max(start, pos + offset);
    			}
    		}
    		
    		Log.e(wewrite.TAG, w.unconfirmed_event + " " + w.in_reapply);
			if (w.unconfirmed_event && !w.in_reapply){
				Log.d(wewrite.TAG, "Added the event to reapplyQueue");
				w.reapplyQueue.add(event);
				w.earliestStartSoFar = (start < w.earliestStartSoFar) ? start : w.earliestStartSoFar;
			}
		}
	}
	
	@Override
	public void onSessionCreated(long id)
	{
		Log.i(wewrite.TAG, "Session created, id: " + id);
		w.sessionId = id;
	}

	@Override
	public void onSessionJoined(long maxOrderId, long baseFileSize)
	{

		Log.i(wewrite.TAG, "Session Joined");
		if( baseFileSize > 0 )
		{
			  //initialize buffer to receive base file
			  w.baseFileReceiveBuffer = new ByteArrayOutputStream((int) baseFileSize);
		}

	}
	
	@Override
	public void onSessionEnd(long id)
	{
		//filler
		Log.i(wewrite.TAG, w.sessionName + ", " + w.sessionId + " has ended");				
	}
	
	@Override
	public void onError(CollabrifyException e)
	{
		//Filler
		Log.e(wewrite.TAG, "onERROR WAS CALLED WTFFF", e);
	}

	public void onReceiveSessionList(final List<CollabrifySession> sessionList)
	{
		if( sessionList.isEmpty() )
		{
			Log.i(wewrite.TAG, "No session available");
			return;
		}
		List<String> sessionNames = new ArrayList<String>();
		for( CollabrifySession s : sessionList )
		{
			sessionNames.add(s.name());
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(
			w);
		builder.setTitle("Choose Session").setItems(
		sessionNames.toArray(new String[sessionList.size()]),
		new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				try
				{
					w.sessionId = sessionList.get(which).id();
					w.sessionName = sessionList.get(which).name();
					w.myClient.joinSession(w.sessionId, null);
			
					//txt.setText(baseFileReceiveBuffer.toString());
				}
				catch( CollabrifyException e )
				{
					Log.e(wewrite.TAG, "error", e);
				}
			}
		});
		
		w.runOnUiThread(new Runnable()
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
			read = w.baseFileBuffer.read(temp);
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
				w.baseFileReceiveBuffer.write(baseFileChunk);
			}
			else
			{
				Log.v(wewrite.TAG, w.baseFileReceiveBuffer.toString());
				w.baseFileReceiveBuffer.close();
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
		Log.v(wewrite.TAG, w.sessionName);
		try
		{
			w.baseFileBuffer.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}
}

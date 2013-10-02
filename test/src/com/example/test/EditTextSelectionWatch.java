package com.example.test;

import com.example.test.Event.WeWriteEvent;

import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

public final class EditTextSelectionWatch extends EditText 
{
	wewrite w;
	boolean WeWriteSet = false;
	
	public EditTextSelectionWatch(Context context) 
	{
		super(context);
	}
	
	public EditTextSelectionWatch(Context context, AttributeSet attrs)
	{
		super(context, attrs);	
	}
	
	public EditTextSelectionWatch(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);	
	}
		
	public void setWeWrite(wewrite w){
		this.w = w;
		WeWriteSet = true;
	}
	
	@Override
	protected void onSelectionChanged(int selStart, int selEnd) 
	{
		if (!WeWriteSet)
		{
			return;
		}
		
		if (!w.isTextChange)
		{
			return;
		} 
		else 
		{
			WeWriteEvent packet = WeWriteEvent.newBuilder()
				.setType(WeWriteEvent.EventType.MOVECURSOR)
				.setCursorStart(selStart)
				.setParticipantID(w.userID)
				.build();
			
			try 
			{
				Log.i(wewrite.TAG, "CURSOR CHANGE HOOLIGANS!");
				w.myClient.broadcast(packet.toByteArray(), "MoveCursor");
    			w.participantListCursorChange(w.userID, selStart);
			}
			catch (CollabrifyException e)
			{
				Log.e(wewrite.TAG, "Error Broadcasting MoveCursor");
				e.printStackTrace();
			}
		}
	}
}
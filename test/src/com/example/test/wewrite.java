package com.example.test;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class wewrite extends Activity {
	
	
		ArrayList<String> undolist = new ArrayList<String>();
		ArrayList<String> redolist = new ArrayList<String>();
		
		
		EditText txt;
		
		Button undobutton;
		Button redobutton;
		
		String temp;
		String temp2;
				
		boolean no;
	
	 	@Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.text);
	        
	        //getActionBar().setDisplayHomeAsUpEnabled(true);
	        
	        txt  = (EditText) findViewById(R.id.editText1);
	        undobutton = (Button) findViewById(R.id.undo);
	        redobutton = (Button) findViewById(R.id.redo);
	        
	        temp = new String("");
	        	        
	        no = true;
	        	   
	        txt.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable arg0) {
					
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
				public void onTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {
					// TODO Auto-generated method stub
					
				}
	        	
	        });
	        
	        
	        undobutton.setOnClickListener(new OnClickListener() {
				 
				@Override
				public void onClick(View arg0) {
					
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
	        
	        redobutton.setOnClickListener(new OnClickListener() {
				 
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
	        
	        
	    }

	 	 @Override
	     public boolean onOptionsItemSelected(MenuItem item) {
	 		onBackPressed();
	 	    return true;
	     }

}

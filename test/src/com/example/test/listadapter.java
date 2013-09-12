package com.example.test;
  
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class listadapter extends ArrayAdapter<String> {
  private final Context context;
  private final ArrayList<String> values;
 
  public listadapter(Context context, ArrayList<String> values) {
    super(context, R.layout.listelement, values);
    this.context = context;
    this.values = values;
  }
 
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
    View rowView = inflater.inflate(R.layout.listelement, parent, false);
    TextView textView = (TextView) rowView.findViewById(R.id.label);
    ImageView imageView = (ImageView) rowView.findViewById(R.id.logo);
    textView.setText(values.get(position));
 
    // Change icon based on name
    String s = values.get(position);
 
    System.out.println(s);
 
    return rowView;
  }
}
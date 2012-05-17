/**
 * Copyright 2012 Inomera Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Includes jQuery.js
 * http://jquery.com
 * Copyright 2011, John Resig
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://jquery.org/license
 */

package com.inomera.mb;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.ContentContext;
import com.netmera.mobile.ContentService;
import com.netmera.mobile.NetmeraClient;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraMedia;

public class ViewContent extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
	private int progressBarStatus = 0;
	private Handler progressBarHandler = new Handler();	
	private String path = "";
	private List<Bitmap> images = new ArrayList<Bitmap>();	
	private ContentContext context;
	
	private static final int RETURN_CODE_EDIT = 2222;
	private static final int DELETED_RESULT = 48;
	
	//setting UI elements
	private Button okButton;
	private Button editButton;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.view);
	    
	    //initializing UI elements
	    okButton = (Button) findViewById(R.id.btnOK);
	    okButton.setOnClickListener(this);
	    
	    editButton = (Button) findViewById(R.id.btnEdit);
	    editButton.setOnClickListener(this);
	    
		path = (String) getIntent().getExtras().get(GeneralConstants.KEY_PATH);
		NetmeraClient.init(getApplicationContext(), GeneralConstants.SECURITY_KEY);
		//get the content service with mobiblogger
		ContentService service = new ContentService(GeneralConstants.DATA_TABLE_NAME);
		service.setPath(path);		
		ContentContext ctx;
		try {
			//get content context from the service
			ctx = service.get();
			context = ctx;
			TextView titleText = (TextView) findViewById(R.id.titleText);
			TextView contentText = (TextView) findViewById(R.id.contentText);
			//set values for UI elements
			titleText.setText(ctx.getString(GeneralConstants.KEY_TITLE));
			contentText.setText(ctx.getString(GeneralConstants.KEY_DESCRIPTION));
		} catch (NetmeraException e) {
			e.printStackTrace();
		}

		//reset progress bar status
		progressBarStatus = 0;
		//start progress dialog
		final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", false);
		
		new Thread(new Runnable() {
			public void run() {
				while (progressBarStatus < 100) {			
					// 	process fetching content
					progressBarStatus = displayContent(context);
					// 		Update the progress bar
					progressBarHandler.post(new Runnable() {
						public void run() {
						//	progressBar.setProgress(progressBarStatus);
						}
					});
				}				
				//when the operation is finished, send message with handler
				handler.sendEmptyMessage(0);
			}
			
		    private Handler handler = new Handler() {
		    	//get message from handler, exit the new thread and retrn to display view
		    	@Override
		    	public void handleMessage(Message msg) {
                   super.handleMessage(msg);	                   
           		ImageAdapter myAdapter = new ImageAdapter(ViewContent.this);
        		myAdapter.setImages(images);
        		GridView gridview = (GridView) findViewById(R.id.gridPhotos);
        	    gridview.setAdapter(myAdapter);
        	    dialog.dismiss();	        	    
               }
           };
		}).start();
	}

	private int displayContent(ContentContext ctx) {
		try {			
			int mediaCount = 0;
			//display images
			while(ctx.getContent().getData().get("file"+mediaCount) != null) {
				if (ctx.getContent().getData().get("file"+mediaCount).length() != 7) {
					NetmeraMedia media = ctx.getNetmeraMedia("file" + mediaCount);
					String url = media.getUrl(NetmeraMedia.PhotoSize.SMALL);
					byte[] imageBytes = media.getData();
					Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
					images.add(bmp);
				}	
				mediaCount++;
			}
		} catch (NetmeraException e) {
			e.printStackTrace();
		}	
		return 100;
	}
	
	@Override
	public void onClick(View v) {
		if(v == okButton) {
			//back button is clicked, go back to main menu
			setResult(RESULT_OK);
			finish();
		} else if(v == editButton) {
			//edit button is clicked, send item path to edit window.
			Intent intent = new Intent(ViewContent.this, EditContent.class);
			intent.putExtra(GeneralConstants.KEY_PATH, path);
			startActivityForResult(intent , RETURN_CODE_EDIT);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//get result from edit.
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RETURN_CODE_EDIT && resultCode == RESULT_OK) {
			//the content is edited, update views
			updateBlog();			
		} else if(requestCode == RETURN_CODE_EDIT && resultCode == DELETED_RESULT ) {
			//the content is deleted, return back to main menu
			setResult(RESULT_OK);
			finish();
		}
	}

	public void updateBlog() {	
		//fetch updated data from the server
		super.onResume();		
		images = new ArrayList<Bitmap>();
		path = (String) getIntent().getExtras().get(GeneralConstants.KEY_PATH);
		NetmeraClient.init(getApplicationContext(), GeneralConstants.SECURITY_KEY);
		
		ContentService service = new ContentService(GeneralConstants.DATA_TABLE_NAME);
		service.setPath(path);		
		ContentContext ctx;
		try {
			ctx = service.get();
			context = ctx;
			TextView titleText = (TextView) findViewById(R.id.titleText);
			TextView contentText = (TextView) findViewById(R.id.contentText);
			
			titleText.setText(ctx.getString(GeneralConstants.KEY_TITLE));
			contentText.setText(ctx.getString(GeneralConstants.KEY_DESCRIPTION));
		} catch (NetmeraException e) {
			e.printStackTrace();
		}

		//reset progress bar status
		progressBarStatus = 0;
		//start progress dialog
		final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", false);
		
		new Thread(new Runnable() {
			public void run() {
				while (progressBarStatus < 100) {			
					// 	process fetching content
					progressBarStatus = displayContent(context);
					// 		Update the progress bar
					progressBarHandler.post(new Runnable() {
						public void run() {
						//	progressBar.setProgress(progressBarStatus);
						}
					});
				}
				//when fetching finishes, end the new thread and send message with handler
				handler.sendEmptyMessage(0);
			}
			
		    private Handler handler = new Handler() {
		    		//process message from handler, and display updated content
	               	@Override
	               	public void handleMessage(Message msg) {
            	   	super.handleMessage(msg);
                   
           			ImageAdapter myAdapter = new ImageAdapter(ViewContent.this);
	        		myAdapter.setImages(images);
	        		myAdapter.notifyDataSetChanged();
	        		GridView gridview = (GridView) findViewById(R.id.gridPhotos);
	        	    gridview.setAdapter(myAdapter);
	        	    dialog.dismiss();	        	    
	               }
	           };
		}).start();
	}
	
	public class ImageAdapter extends BaseAdapter {
		//define a custom image adapter for gridview
	    private Context mContext;
	    private List<Bitmap> pics;
	    
	    public ImageAdapter(Context c) {
	        mContext = c;
	    }

	    public int getCount() {
	        return pics.size();
	    }

	    public Object getItem(int position) {
	        return null;
	    }

	    public long getItemId(int position) {
	        return 0;
	    }

	    // create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	final Bitmap zoomBitmap = pics.get(position);
	        ImageView imageView;
	        if (convertView == null) {  // if it's not recycled, initialize some attributes
	            imageView = new ImageView(mContext);
	            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	            imageView.setPadding(8, 8, 8, 8);
	        } else {
	            imageView = (ImageView) convertView;
	        }

	        imageView.setImageBitmap(pics.get(position));
	        //imageView.setImageResource(mThumbIds[position]);
	        imageView.setOnClickListener(new OnClickListener() {					
				@Override
				public void onClick(View v) {						
					final AlertDialog.Builder imageDialog = new AlertDialog.Builder(ViewContent.this);
					ImageView cloneImage = new ImageView(ViewContent.this);
					cloneImage.setImageBitmap(zoomBitmap);
					
			        imageDialog.setView(cloneImage);
			        imageDialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){

			            public void onClick(DialogInterface dialog, int which) {
			            	dialog.dismiss();
			            }
			        });
			        imageDialog.create();
			        imageDialog.show();
				}
	        });
	        return imageView;
	    }

	    public void setImages(List<Bitmap> imgs) {
	    	pics = imgs;
	    }
	}
}

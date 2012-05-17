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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.ContentContext;
import com.netmera.mobile.ContentService;
import com.netmera.mobile.NetmeraClient;
import com.netmera.mobile.NetmeraException;

public class MobiBloggerActivity extends Activity implements OnItemClickListener,OnClickListener{
    /** Called when the activity is first created. */

	private static final int RETURN_CODE_ADD = 1111; 
	private static final int RETURN_CODE_EDIT = 2222;
	private static final int RETURN_CODE_VIEW= 3333;
	
	private Button searchButton;
	private EditText editText;
	private ListView listView;
	private Button btnAdd;
	
	private List<ContentContext> ccList = null;	
	private ArrayList<String> listArray = new ArrayList<String>();	
	private boolean isSearchedSth = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        listView = (ListView) findViewById(R.id.listView1);

        initAPI();
        //Setting UI attributes 
		listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listArray));
		listView.setOnItemClickListener(this);
		
		editText = (EditText) findViewById(R.id.editText1);
		
		searchButton = (Button) findViewById(R.id.button1);
		searchButton.setOnClickListener(this);
		
		btnAdd = (Button) findViewById(R.id.btnAdd);
		btnAdd.setOnClickListener(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listArray));
    	listView.setOnItemClickListener(this);
    	listView.setOnItemLongClickListener(new OnItemLongClickListener() {
	        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	        	Intent intent = new Intent(MobiBloggerActivity.this, EditContent.class);
				intent.putExtra(GeneralConstants.KEY_PATH, ccList.get(position).getPath());
				startActivityForResult(intent , RETURN_CODE_EDIT);		
				return true;
	        }
	    });
    }
    
    private List<ContentContext> initAPI(){
    	
    	NetmeraClient.init(getApplicationContext(), GeneralConstants.SECURITY_KEY);
    	ContentService cs = new ContentService(GeneralConstants.DATA_TABLE_NAME);
    	
    	try {
			ccList = cs.search();
			for (ContentContext cc : ccList) {
				listArray.add(cc.getString(GeneralConstants.KEY_TITLE));
			}
		} catch (NetmeraException e) {
			e.printStackTrace();
		}    	
    	return ccList;
    }

	@Override
	public void onClick(View arg0) {		
		if (arg0 == searchButton) {
			String searchText = editText.getText().toString();
			isSearchedSth = true;
			
	    	ContentService cs = new ContentService(GeneralConstants.DATA_TABLE_NAME);
	    	cs.addSearchText(searchText);
	    	
	    	try {	    		
				ccList = cs.search();
				listArray.clear();
				
				for (ContentContext cc : ccList) {
					
					listArray.add(cc.getString(GeneralConstants.KEY_TITLE));
				}
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	    		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
				
			} catch (NetmeraException e) {
				e.printStackTrace();
			}
	    	
		} else if(arg0 == btnAdd) {
			Intent intent = new Intent(MobiBloggerActivity.this, AddContent.class);
			startActivityForResult(intent, RETURN_CODE_ADD);
		}		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Intent intent = new Intent(MobiBloggerActivity.this, ViewContent.class);
			intent.putExtra(GeneralConstants.KEY_PATH, ccList.get(arg2).getPath());
			startActivityForResult(intent , RETURN_CODE_VIEW);		
	}
	
	
	
	
	
	private void updateList(){
    	
		ContentService cs = new ContentService(GeneralConstants.DATA_TABLE_NAME);
    	
    	try {
			ccList = cs.search();
			listArray.clear();			
			for (ContentContext cc : ccList) {
				listArray.add(cc.getString(GeneralConstants.KEY_TITLE));
			}
		} catch (NetmeraException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == RETURN_CODE_ADD && resultCode == RESULT_OK) {
			updateList();
		}else 	if (requestCode == RETURN_CODE_EDIT && resultCode == RESULT_OK) {
			
			updateList();
		} else if (requestCode == RETURN_CODE_VIEW && resultCode == RESULT_OK) {
			updateList();
		} else if(requestCode == RETURN_CODE_VIEW && resultCode == -1) {
			updateList();
		}
	}

	@Override
	public void onBackPressed() {		
		if (isSearchedSth == true) {
			isSearchedSth = false;
			updateList();
			
		}else {
			
			super.onBackPressed();
		}
	}
}
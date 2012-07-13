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
import android.app.Dialog;
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
import android.widget.TextView;
import android.widget.Toast;

import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.NetmeraCallback;
import com.netmera.mobile.NetmeraClient;
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraService;
import com.netmera.mobile.NetmeraUser;
import com.netmera.mobile.util.StringUtils;

public class MobiBloggerActivity extends Activity implements OnItemClickListener, OnClickListener {
	
	private static final int RETURN_CODE_ADD = 1111;
	private static final int RETURN_CODE_EDIT = 2222;
	private static final int RETURN_CODE_VIEW = 3333;
	private static final int RETURN_CODE_LOCATION = 4444;

	private Button searchButton;
	private EditText searchTextField;
	private ListView listView;
	private Button addBlogButton;
	private Button locationSearchButton;
	private Button registerButton;
	private Button loginButton;
	private Dialog registerDialog;
	private Dialog loginDialog;
	
	private ArrayAdapter<String> adapter;
	
	private List<NetmeraContent> globalNetmeraContentList = new ArrayList<NetmeraContent>();
	private List<String> listArray = new ArrayList<String>();
	private NetmeraUser currentUser;

	private boolean isSearchedSth = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		currentUser = NetmeraUser.getCurrentUser();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		listView = (ListView) findViewById(R.id.listView1);

		NetmeraClient.init(getApplicationContext(), GeneralConstants.SECURITY_KEY);
		adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, listArray);
		
		// Setting UI attributes
		searchTextField = (EditText) findViewById(R.id.searchText);
		
		loginButton = (Button) findViewById(R.id.login);
		loginButton.setOnClickListener(this);
		registerButton = (Button) findViewById(R.id.register);
		registerButton.setOnClickListener(this);
		
		searchButton = (Button) findViewById(R.id.button1);
		searchButton.setOnClickListener(this);

		addBlogButton = (Button) findViewById(R.id.btnAdd);
		addBlogButton.setOnClickListener(this);

		locationSearchButton = (Button) findViewById(R.id.btnLocation);
		locationSearchButton.setOnClickListener(this);
		
		updateList();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(MobiBloggerActivity.this, ViewContent.class);
		try {
			intent.putExtra(GeneralConstants.KEY_PATH, globalNetmeraContentList.get(position).getPath());
		} catch (NetmeraException e) {
			e.printStackTrace();
		}
		startActivityForResult(intent, RETURN_CODE_VIEW);
	}

	@Override
	public void onClick(View button) {
		if (button == searchButton) {
			isSearchedSth = true;

			updateList();
			
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(searchTextField.getWindowToken(), 0);
		} else if (button == addBlogButton) {
			Intent intent = new Intent(MobiBloggerActivity.this, AddContent.class);
			startActivityForResult(intent, RETURN_CODE_ADD);
		} else if (button == locationSearchButton) {
			Intent intent = new Intent(MobiBloggerActivity.this, LocationSearch.class);
			startActivityForResult(intent, RETURN_CODE_LOCATION);
		} else if (button == registerButton) {
			registerDialog = new Dialog(this);
			registerDialog.setContentView(R.layout.register_dialog);
			registerDialog.setTitle("Register");
			registerDialog.setCancelable(true);
			
			Button cancelButton = (Button)registerDialog.findViewById(R.id.registerCancel);
			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					registerDialog.cancel();
				}
			});
			
			Button regBttn = (Button)registerDialog.findViewById(R.id.registerComplete);
			regBttn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					TextView email = (TextView)registerDialog.findViewById(R.id.registerEmail);
					TextView nickname = (TextView)registerDialog.findViewById(R.id.registerNickname);
					TextView password = (TextView)registerDialog.findViewById(R.id.registerPassword);
					
					String emailString = email.getText().toString();
					String nicknameString = nickname.getText().toString();
					String passwordString = password.getText().toString();
					
					if (StringUtils.isNotBlank(emailString) && StringUtils.isNotBlank(nicknameString) && StringUtils.isNotBlank(passwordString)) {
						currentUser = new NetmeraUser();
						currentUser.setEmail(emailString);
						currentUser.setNickname(nicknameString);
						currentUser.setPassword(passwordString);
						
						currentUser.registerInBackground(new NetmeraCallback<NetmeraUser> () {
							@Override
							public void callback(NetmeraUser user, NetmeraException arg1) {
								//TODO : login?
								if (user != null) {
									registerDialog.cancel();
								} else {
									//TODO correct reason
									Toast toast = Toast.makeText(getBaseContext(), "Login Failed", Toast.LENGTH_SHORT);
									toast.show();
								}
							}
						});
					} else {
						Toast toast = Toast.makeText(getBaseContext(), "Fields cannot be empty", Toast.LENGTH_SHORT);
						toast.show();
					}
				}
			});
			
			registerDialog.show();
		} else if (button == loginButton) {
			NetmeraUser user= NetmeraUser.getCurrentUser();
			
			if (user == null) {
				loginDialog = new Dialog(this);
				loginDialog.setContentView(R.layout.login_dialog);
				loginDialog.setTitle("Login");
				loginDialog.setCancelable(true);
				
				///////////// put as default
				TextView email = (TextView)loginDialog.findViewById(R.id.loginEmail);
				TextView password = (TextView)loginDialog.findViewById(R.id.loginPassword);
				email.setText("mustafa.genc@inomera.com");
				password.setText("1234");
				/////////////////////////////
				
				Button cancelButton = (Button)loginDialog.findViewById(R.id.loginCancel);
				cancelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						loginDialog.cancel();
					}
				});
				
				Button lgnBtn = (Button)loginDialog.findViewById(R.id.loginComplete);
				lgnBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						TextView email = (TextView)loginDialog.findViewById(R.id.loginEmail);
						TextView password = (TextView)loginDialog.findViewById(R.id.loginPassword);
						
						String emailString = email.getText().toString();
						String passwordString = password.getText().toString();
						
						if (StringUtils.isNotBlank(emailString) && StringUtils.isNotBlank(passwordString)) {
							NetmeraUser.loginInBackground(emailString, passwordString, new NetmeraCallback<NetmeraUser> () {
								@Override
								public void callback(NetmeraUser user, NetmeraException exception) {
									if (user != null) {
										currentUser = NetmeraUser.getCurrentUser();
										addBlogButton.setEnabled(true);
										loginButton.setText(R.string.logout);
										
										TextView emailText = (TextView)findViewById(R.id.emailText);
										emailText.setText(user.getEmail());
										
										updateList();
										loginDialog.cancel();
									} else {
										//TODO correct reason
										Toast toast = Toast.makeText(getBaseContext(), "Login Failed", Toast.LENGTH_SHORT);
										toast.show();
									}
								}
							});
							
						} else {
							Toast toast = Toast.makeText(getBaseContext(), "Fields cannot be empty", Toast.LENGTH_SHORT);
							toast.show();
						}
					}
				});
				
				loginDialog.show();
			} else {
				NetmeraUser.logout();
				currentUser = null;
				
				addBlogButton.setEnabled(false);
				loginButton.setText(R.string.login);
				
				TextView emailText = (TextView)findViewById(R.id.emailText);
				emailText.setText("");
				
				updateList();
			}
		}
	}

	private void updateList() {
		globalNetmeraContentList.clear();
		adapter.clear();	
		
		String searchText = searchTextField.getText().toString();
		
		if (currentUser != null) {
			NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
			
			netmeraService.addSearchText(searchText);
			netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PRIVATE);
			netmeraService.whereEqual(GeneralConstants.KEY_OWNER, currentUser.getEmail());
			
			try {
				List<NetmeraContent> resultList = netmeraService.search();
				
				for (NetmeraContent netmeraContent : resultList) {
					globalNetmeraContentList.add(netmeraContent);
					adapter.add(netmeraContent.getString(GeneralConstants.KEY_TITLE) + "-" + GeneralConstants.PRIVACY_PRIVATE);
				}
			} catch (NetmeraException e) {
				e.printStackTrace();
			}
		} 
		
		NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
		netmeraService.addSearchText(searchText);
		netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PUBLIC);
		
		try {
			List<NetmeraContent> resultList = netmeraService.search();
			
			for (NetmeraContent netmeraContent : resultList) {
				globalNetmeraContentList.add(netmeraContent);
				adapter.add(netmeraContent.getString(GeneralConstants.KEY_TITLE));
			}
		} catch (NetmeraException e) {
			e.printStackTrace();
		}
		
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(MobiBloggerActivity.this, EditContent.class);
				try {
					intent.putExtra(GeneralConstants.KEY_PATH, globalNetmeraContentList.get(position).getPath());
				} catch (NetmeraException e) {
					e.printStackTrace();
				}
				startActivityForResult(intent, RETURN_CODE_EDIT);
				return true;
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RETURN_CODE_ADD && resultCode == RESULT_OK) {
			updateList();
		} else if (requestCode == RETURN_CODE_EDIT && resultCode == RESULT_OK) {
			updateList();
		} else if (requestCode == RETURN_CODE_VIEW && resultCode == RESULT_OK) {
			updateList();
		} else if (requestCode == RETURN_CODE_VIEW && resultCode == -1) {
			updateList();
		}
	}

	@Override
	public void onBackPressed() {
		if (isSearchedSth == true) {
			isSearchedSth = false;
			updateList();
		} else {
			super.onBackPressed();
		}
	}
}
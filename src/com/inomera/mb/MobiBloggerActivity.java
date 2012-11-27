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
import android.util.Log;
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
import com.netmera.mobile.NetmeraPushService;
import com.netmera.mobile.NetmeraService;
import com.netmera.mobile.NetmeraTwitterUtils;
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
	private Button loginTwitterButton;
	private Dialog registerDialog;
	private Dialog loginDialog;

	private ArrayAdapter<String> adapter;

	private List<NetmeraContent> globalNetmeraContentList = new ArrayList<NetmeraContent>();
	private List<String> listArray = new ArrayList<String>();
	private NetmeraUser currentUser = null;

	private boolean isSearchedSth = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//Setting up netmera cloud application to my mobile application
		NetmeraClient.init(getApplicationContext(), GeneralConstants.NETMERA_API_KEY);
		try {
			//Setting up push notifaction 
			NetmeraPushService.register(getApplicationContext(), MobiBloggerActivity.class, GeneralConstants.GOOGLE_API_KEY);

			//Setting up twitterApi
			NetmeraTwitterUtils.initialize(GeneralConstants.TWITTER_CONSUMER_KEY, GeneralConstants.TWITTER_CONSUMER_SECRET);
		} catch (NetmeraException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			currentUser = NetmeraUser.getCurrentUser();
		} catch (NetmeraException e) {
			e.printStackTrace();
		}

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		listView = (ListView) findViewById(R.id.listView1);

		adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, listArray);

		// Setting UI attributes
		searchTextField = (EditText) findViewById(R.id.searchText);

		loginButton = (Button) findViewById(R.id.login);
		loginButton.setOnClickListener(this);

		loginTwitterButton = (Button) findViewById(R.id.loginTwitter);
		loginTwitterButton.setOnClickListener(this);

		registerButton = (Button) findViewById(R.id.register);
		registerButton.setOnClickListener(this);

		searchButton = (Button) findViewById(R.id.button1);
		searchButton.setOnClickListener(this);

		addBlogButton = (Button) findViewById(R.id.btnAdd);
		addBlogButton.setOnClickListener(this);

		locationSearchButton = (Button) findViewById(R.id.btnLocation);
		locationSearchButton.setOnClickListener(this);

		searchBlog();
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

			searchBlog();
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
			NetmeraUser user = null;
			try {
				user = NetmeraUser.getCurrentUser();
			} catch (NetmeraException e) {
				e.printStackTrace();
			}

			if (user == null) {
				loginDialog = new Dialog(this);
				loginDialog.setContentView(R.layout.login_dialog);
				loginDialog.setTitle("Login");
				loginDialog.setCancelable(true);

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
										try {
											currentUser = NetmeraUser.getCurrentUser();
										} catch (NetmeraException e) {
											e.printStackTrace();
										}
										addBlogButton.setEnabled(true);
										loginButton.setText(R.string.logout);

										TextView emailText = (TextView)findViewById(R.id.emailText);
										emailText.setText(user.getEmail());

										searchBlog();
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

				searchBlog();
			}
		}  else if (button == loginTwitterButton) {
			NetmeraTwitterUtils.login(this, new NetmeraCallback<NetmeraUser>() {
				@Override
				public void callback(NetmeraUser user, NetmeraException exception) {
					if (user == null) {
						//fail
						Toast toast = Toast.makeText(getBaseContext(), "Fail Twitter Login", Toast.LENGTH_SHORT);
						toast.show();
					} else if (user.isNewUser()) {
						//user is newly created via Twitter
						TextView emailText = (TextView)findViewById(R.id.emailText);
						emailText.setText(user.getNickname());
						addBlogButton.setEnabled(true);
					} else {
						//already registered user. its only logged in
						TextView emailText = (TextView)findViewById(R.id.emailText);
						emailText.setText(user.getNickname());
						addBlogButton.setEnabled(true);
					}
				}
			});
		}
	}

	private void searchBlog(){
		globalNetmeraContentList.clear();
		adapter.clear();
		
		String searchText = searchTextField.getText().toString();

		if (currentUser != null) {
			NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);

			netmeraService.addSearchText(searchText);
			netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PRIVATE);
			netmeraService.whereEqual(GeneralConstants.KEY_OWNER, currentUser.getEmail());

			netmeraService.searchInBackground(new NetmeraCallback< List< NetmeraContent>>() {
				@Override
				public void callback(List< NetmeraContent> contentList, NetmeraException exception) {
					if (contentList != null && exception == null) {
						// Success
						for (NetmeraContent content : contentList) {
							try {
								globalNetmeraContentList.add(content);
								adapter.add(content.getString(GeneralConstants.KEY_TITLE) + "-" + GeneralConstants.PRIVACY_PRIVATE);
							} catch (NetmeraException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						// Error occurred
					}
					searchBlogWithoutUser();
				}
			});
		}else {
			searchBlogWithoutUser();
		}

	}

	private void searchBlogWithoutUser(){
		String searchText = searchTextField.getText().toString();

		NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
		netmeraService.addSearchText(searchText);
		netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PUBLIC);

		netmeraService.searchInBackground(new NetmeraCallback< List< NetmeraContent>>() {
			@Override
			public void callback(List< NetmeraContent> contentList, NetmeraException exception) {
				if (contentList != null && exception == null) {
					// Success
					for (NetmeraContent content : contentList) {
						try {
							globalNetmeraContentList.add(content);
							adapter.add(content.getString(GeneralConstants.KEY_TITLE));
						} catch (NetmeraException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					// Error occurred
					Log.v("asd", "asd");
				}
				updateList();
			}
		});
	}

	private void updateList() {
		
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
			searchBlog();
		} else if (requestCode == RETURN_CODE_EDIT && resultCode == RESULT_OK) {
			searchBlog();
		} else if (requestCode == RETURN_CODE_VIEW && resultCode == RESULT_OK) {
			searchBlog();
		} else if (requestCode == RETURN_CODE_VIEW && resultCode == -1) {
			searchBlog();
		}
	}

	@Override
	public void onBackPressed() {
		if (isSearchedSth == true) {
			isSearchedSth = false;
			searchBlog();
		} else {
			super.onBackPressed();
		}
	}
}
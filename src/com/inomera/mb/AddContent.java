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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.NetmeraClient;
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraGeoLocation;
import com.netmera.mobile.NetmeraMedia;
import com.netmera.mobile.NetmeraUser;
import com.netmera.mobile.util.HttpUtils;

public class AddContent extends Activity implements OnClickListener {
	/** Called when the activity is first created. */
	private int imageCount = 0;
	private Uri mCapturedImageURI;
	private int progressBarStatus = 0;
	private Handler progressBarHandler = new Handler();
	private double latitude = 0;
	private double longitude = 0;
	LocationManager locationManager;
	LocationListener locationListener;

	private static final int PHOTO_REQUEST = 1111;
	private static final int CAMERA_PIC_REQUEST = 1337;

	// setting UI elements
	private CheckBox isPrivateCheckBox;
	private Button submitButton;
	private EditText title;
	private EditText description;
	private Button btnPhoto;
	private List<String> photoPaths = new ArrayList<String>();
	private Button camButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Setting UI attributes
		submitButton = (Button) findViewById(R.id.buttonSubmit);
		submitButton.setOnClickListener(this);
		title = (EditText) findViewById(R.id.editTextTitle);
		description = (EditText) findViewById(R.id.editTextDescription);

		btnPhoto = (Button) findViewById(R.id.btnAddPhoto);
		btnPhoto.setOnClickListener(this);

		camButton = (Button) findViewById(R.id.btnCamera);
		camButton.setOnClickListener(this);

		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				latitude = location.getLatitude();
				longitude = location.getLongitude();

				locationManager.removeUpdates(locationListener);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	@Override
	public void onClick(View clickedItem) {
		if (clickedItem == submitButton) {
			progressBarStatus = 0;
			// set progress dialog
			final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", false);
			new Thread(new Runnable() {
				public void run() {
					while (progressBarStatus < 100) {
						// wait until the operation is complete
						// add new blog
						progressBarStatus = addBlog(title.getText().toString(), description.getText().toString());
						progressBarHandler.post(new Runnable() {
							public void run() {
							}
						});
					}
					// when loading is finished, send message with handler
					handler.sendEmptyMessage(0);
				}

				private Handler handler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// process handler message and get out.
						super.handleMessage(msg);
						Intent intent = new Intent();
						intent.putExtra(GeneralConstants.KEY_TITLE, title.getText().toString());
						dialog.dismiss();
						if (getParent() == null) {
							setResult(RESULT_OK, intent);
						} else {

							setResult(RESULT_OK, intent);
						}
						finish();
					}
				};
			}).start();
		} else if (clickedItem == btnPhoto) {
			// open photo gallery
			Intent imageInt = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(imageInt, PHOTO_REQUEST);
		} else if (clickedItem == camButton) {
			// open phone camera
			openDeviceCam();
		}
	}

	public void openDeviceCam() {
		// for each new photo from camera, assign a new path, and start camera
		// activity.
		String fileName = "temp" + String.valueOf(imageCount) + ".jpg";
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
		mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
		startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
	}

	public int addBlog(String titleText, String descriptionText) {
		// adding new blog operation
		// create a content context object with 'mobiBlogger' table name
		String address = getAddress();
		NetmeraContent netmeraContent = new NetmeraContent(GeneralConstants.DATA_TABLE_NAME);
		// create a netmeraGeoLocationObject from latitude and longitude
		try {
			NetmeraGeoLocation ngl = new NetmeraGeoLocation(latitude, longitude);
			// add title and description to content context object
			isPrivateCheckBox = (CheckBox)findViewById(R.id.isPrivate);
			if (isPrivateCheckBox.isChecked()) {
				netmeraContent.add(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PRIVATE);
			} else {
				netmeraContent.add(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PUBLIC);
			}
			
			NetmeraUser currentUser = NetmeraUser.getCurrentUser();
			
			if (currentUser != null) {
				netmeraContent.add(GeneralConstants.KEY_OWNER, currentUser.getEmail());
			}
			
			netmeraContent.add(GeneralConstants.KEY_TITLE, title.getText().toString());
			netmeraContent.add(GeneralConstants.KEY_DESCRIPTION, description.getText().toString());
			netmeraContent.add("location", ngl);
			netmeraContent.add("address", address);

			// add photos, each with a key 'file[photoNumber]'
			if (photoPaths.size() > 0) {
				for (int i = 0; i < photoPaths.size(); i++) {
					String path = photoPaths.get(i);
					byte[] bytes = HttpUtils.toByteArray(new File(path));
					NetmeraMedia file = new NetmeraMedia(bytes);
					netmeraContent.add(GeneralConstants.KEY_PHOTOS + i, file);
				}
			}
			netmeraContent.create();
		} catch (NetmeraException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error while saving data", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 100;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PHOTO_REQUEST) {
			if (resultCode == RESULT_OK) {
				// if result is from gallery and valid photo is returned
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				final String filePath = cursor.getString(columnIndex);
				photoPaths.add(filePath);
				cursor.close();

				final Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

				final ImageView newImage = new ImageView(this);
				newImage.setImageBitmap(yourSelectedImage);
				newImage.setLayoutParams(new LayoutParams(100, 100));

				LinearLayout rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
				LinearLayout newLayout = new LinearLayout(this);
				newLayout.setOrientation(LinearLayout.HORIZONTAL);
				newLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

				final ImageButton deleteBtn = new ImageButton(this);
				deleteBtn.setImageResource(R.drawable.delete);
				deleteBtn.setLayoutParams(new LayoutParams(48, 48));

				deleteBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						((ViewGroup) ((LinearLayout) deleteBtn.getParent()).getParent()).removeView((LinearLayout) deleteBtn.getParent());
						photoPaths.remove(filePath);
					}
				});

				newImage.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						final AlertDialog.Builder imageDialog = new AlertDialog.Builder(AddContent.this);
						ImageView cloneImage = new ImageView(AddContent.this);
						cloneImage.setImageBitmap(yourSelectedImage);

						imageDialog.setView(cloneImage);
						imageDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
						imageDialog.create();
						imageDialog.show();
					}
				});
				newLayout.addView(newImage);
				newLayout.addView(deleteBtn);
				rootLayout.addView(newLayout);

			}
		} else if (requestCode == CAMERA_PIC_REQUEST && resultCode != 0) {
			// if the result is from camera activity and valid photo is returned
			imageCount++;
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(mCapturedImageURI, filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			final String filePath = cursor.getString(columnIndex);
			photoPaths.add(filePath);
			cursor.close();

			final Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

			final ImageView newImage = new ImageView(this);
			newImage.setImageBitmap(yourSelectedImage);
			newImage.setLayoutParams(new LayoutParams(100, 100));

			LinearLayout rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
			LinearLayout newLayout = new LinearLayout(this);
			newLayout.setOrientation(LinearLayout.HORIZONTAL);
			newLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

			final ImageButton deleteBtn = new ImageButton(this);
			deleteBtn.setImageResource(R.drawable.delete);
			deleteBtn.setLayoutParams(new LayoutParams(48, 48));

			deleteBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					((ViewGroup) ((LinearLayout) deleteBtn.getParent()).getParent()).removeView((LinearLayout) deleteBtn.getParent());
					photoPaths.remove(filePath);
				}
			});

			newImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final AlertDialog.Builder imageDialog = new AlertDialog.Builder(AddContent.this);
					ImageView cloneImage = new ImageView(AddContent.this);
					cloneImage.setImageBitmap(yourSelectedImage);

					imageDialog.setView(cloneImage);
					imageDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					imageDialog.create();
					imageDialog.show();
				}
			});
			newLayout.addView(newImage);
			newLayout.addView(deleteBtn);
			rootLayout.addView(newLayout);
		}
	}

	// get current location
	public class MyLocation {
		Timer timer1;
		LocationManager lm;
		LocationResult locationResult;
		boolean gps_enabled = false;
		boolean network_enabled = false;

		public boolean getLocation(Context context, LocationResult result) {
			// I use LocationResult callback class to pass location value from
			// MyLocation to user code.
			locationResult = result;
			if (lm == null)
				lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			// exceptions will be thrown if provider is not permitted.
			try {
				gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
			} catch (Exception ex) {
			}
			try {
				network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			} catch (Exception ex) {
			}

			// don't start listeners if no provider is enabled
			if (!gps_enabled && !network_enabled)
				return false;

			if (gps_enabled)
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
			if (network_enabled)
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
			timer1 = new Timer();
			timer1.schedule(new GetLastLocation(), 20000);
			return true;
		}

		LocationListener locationListenerGps = new LocationListener() {
			public void onLocationChanged(Location location) {
				timer1.cancel();
				locationResult.gotLocation(location);
				lm.removeUpdates(this);
				lm.removeUpdates(locationListenerNetwork);
			}

			public void onProviderDisabled(String provider) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
		};

		LocationListener locationListenerNetwork = new LocationListener() {
			public void onLocationChanged(Location location) {
				timer1.cancel();
				locationResult.gotLocation(location);
				lm.removeUpdates(this);
				lm.removeUpdates(locationListenerGps);
			}

			public void onProviderDisabled(String provider) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
		};

		class GetLastLocation extends TimerTask {
			@Override
			public void run() {
				lm.removeUpdates(locationListenerGps);
				lm.removeUpdates(locationListenerNetwork);

				Location net_loc = null, gps_loc = null;
				if (gps_enabled)
					gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (network_enabled)
					net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

				// if there are both values use the latest one
				if (gps_loc != null && net_loc != null) {
					if (gps_loc.getTime() > net_loc.getTime())
						locationResult.gotLocation(gps_loc);
					else
						locationResult.gotLocation(net_loc);
					return;
				}

				if (gps_loc != null) {
					locationResult.gotLocation(gps_loc);
					return;
				}
				if (net_loc != null) {
					locationResult.gotLocation(net_loc);
					return;
				}
				locationResult.gotLocation(null);
			}
		}

	}

	public abstract class LocationResult {
		public abstract void gotLocation(Location location);
	}

	public String getAddress() {
		String returnVal = "";
		try {
			Geocoder gcd = new Geocoder(this, Locale.getDefault());
			List<Address> addresses = gcd.getFromLocation(latitude, longitude, 100);
			if (addresses.size() > 0) {
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < addresses.size(); i++) {
					Address address = addresses.get(i);
					int maxIndex = address.getMaxAddressLineIndex();
					for (int x = 0; x <= maxIndex; x++) {
						result.append(address.getAddressLine(x));
						result.append(",");
					}
					result.append(address.getLocality());
					result.append(",");
					result.append(address.getPostalCode());
					result.append("\n\n");
				}
				returnVal = result.toString();
			}
		} catch (IOException ex) {

		}
		return returnVal;
	}
}

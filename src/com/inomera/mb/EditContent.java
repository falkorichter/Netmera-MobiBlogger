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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inomera.mb.ViewContent.ImageAdapter;
import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.NetmeraCallback;
import com.netmera.mobile.NetmeraClient;
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraMedia;
import com.netmera.mobile.NetmeraService;
import com.netmera.mobile.util.HttpUtils;

public class EditContent extends Activity implements OnClickListener {
	/** Called when the activity is first created. */

	private List<Integer> imagesToBeDeleted = new ArrayList<Integer>();
	private List<Integer> imageIndexes = new ArrayList<Integer>();
	private int numberOfPhotos;
	private int imageCount = 0;
	private Uri mCapturedImageURI;
	private String path = "";
	private List<Bitmap> images = new ArrayList<Bitmap>();
	private List<String> photoPaths = new ArrayList<String>();
	private NetmeraContent globalCc;
	private int progressBarStatus = 0;
	private Handler progressBarHandler = new Handler();
	private NetmeraContent context;

	private static final int PHOTO_REQUEST = 1111;
	private static final int DELETED_RESULT = 48;
	private static final int CAMERA_PIC_REQUEST = 1337;

	// setting UI elements
	private Button deleteButton;
	private Button saveButton;
	private EditText title;
	private EditText description;
	private Button btnPhoto;
	private Button camButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit);

		// Initializing UI attributes
		saveButton = (Button) findViewById(R.id.buttonSave);
		saveButton.setOnClickListener(this);
		deleteButton = (Button) findViewById(R.id.buttonDelete);
		deleteButton.setOnClickListener(this);
		title = (EditText) findViewById(R.id.editTextTitle1);
		description = (EditText) findViewById(R.id.editTextDescription1);

		btnPhoto = (Button) findViewById(R.id.btnAddPhoto);
		btnPhoto.setOnClickListener(this);

		camButton = (Button) findViewById(R.id.btnCameraEdit);
		camButton.setOnClickListener(this);

		// Get data via api
		path = (String) getIntent().getExtras().get(GeneralConstants.KEY_PATH);
		NetmeraService service = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
		service.setPath(path);
		service.getInBackground(new NetmeraCallback < NetmeraContent>() {
			@Override
			public void callback(NetmeraContent result, NetmeraException exception) {
				if (result != null && exception == null) {
					// Success
					try {
						context = result;
						title.setText(result.getString(GeneralConstants.KEY_TITLE));
						description.setText(result.getString(GeneralConstants.KEY_DESCRIPTION));
						displayContent(context);
					} catch (NetmeraException ex) {
						// Handle exception
					}
				} else {
					// Error occurred.
				}
			}
		});

	}
	
	private int displayContent(final NetmeraContent ctx) {


		new Thread(new Runnable() {
			public void run() {
				// display images
				try {
					int mediaCount = 0;
					while (ctx.get("file" + mediaCount) != null) {
						if (ctx.getString("file" + mediaCount).length() != 7) {
							NetmeraMedia media = ctx.getNetmeraMedia("file" + mediaCount);
							// String url = media.getUrl(NetmeraMedia.PhotoSize.SMALL);
							byte[] imageBytes = media.getData();
							Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
							images.add(bmp);
							imageIndexes.add(mediaCount);
						}
						mediaCount++;
						numberOfPhotos = mediaCount;
					}
				} catch (NetmeraException e) {
					e.printStackTrace();
				}
				// when the operation is finished, send message with handler
				handler.sendEmptyMessage(0);
			}

			private Handler handler = new Handler() {
				// get message from handler, exit the new thread and return to
				// display view
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					for (int i = 0; i < images.size(); i++) {
						ImageView iv = new ImageView(EditContent.this);
						iv.setImageBitmap(images.get(i));
						placeImageView(iv, images.get(i), imageIndexes.get(i));
					}
				}
			};
		}).start();

		return 100;
	}

	private void placeImageView(ImageView iv, final Bitmap bmp, final int mediaCount) {
		// place image that fetched from server
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
				imagesToBeDeleted.add(mediaCount);
			}
		});

		iv.setLayoutParams(new LayoutParams(100, 100));
		iv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog.Builder imageDialog = new AlertDialog.Builder(EditContent.this);
				ImageView cloneImage = new ImageView(EditContent.this);
				cloneImage.setImageBitmap(bmp);

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
		newLayout.addView(iv);
		newLayout.addView(deleteBtn);
		rootLayout.addView(newLayout);
	}

	@Override
	public void onClick(View v) {
		if (v == saveButton) {
			// if content is updated and saved
			progressBarStatus = 0;
			// start progress dialog
			final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", false);
			new Thread(new Runnable() {
				public void run() {
					while (progressBarStatus < 100) {
						// process fetching content
						progressBarStatus = updateCc(title.getText().toString(), description.getText().toString());
						// Update the progress bar
						progressBarHandler.post(new Runnable() {
							public void run() {
							}
						});
					}
					// when the operation is finished, send message with handler
					handler.sendEmptyMessage(0);
				}

				private Handler handler = new Handler() {
					// get message from handler, exit the new thread and retrn
					// to display view
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);
						setResult(RESULT_OK);
						dialog.dismiss();
						finish();
					}
				};
			}).start();
		} else if (v == deleteButton) {
			// if the content is deleted
			progressBarStatus = 0;
			// get message from handler, exit the new thread and retrn to
			// display view
			final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", false);
			new Thread(new Runnable() {
				public void run() {
					while (progressBarStatus < 100) {
						// process fetching content
						progressBarStatus = deleteCc(globalCc);
						progressBarHandler.post(new Runnable() {
							public void run() {
							}
						});
					}
					// when the operation is finished, send message with handler
					handler.sendEmptyMessage(0);
				}

				private Handler handler = new Handler() {
					// get message from handler, exit the new thread and retrn
					// to display view
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);
						setResult(DELETED_RESULT);
						dialog.dismiss();
						finish();
					}
				};
			}).start();
		} else if (v == btnPhoto) {
			// if photo button is clicked, open up gallery activity
			Intent imageInt = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(imageInt, PHOTO_REQUEST);
		} else if (v == camButton) {
			// open up camera activity
			openDeviceCam();
		}
	}

	public void openDeviceCam() {

		// Setting parameters to camera intent
		String fileName = "temp" + String.valueOf(imageCount) + ".jpg";

		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
		mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
		startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
	}

	private int updateCc(String titleText, String descriptionText) {
		// update blog with contentn context object with loaded path and
		// 'mobiBlogger' key
		NetmeraContent cc = new NetmeraContent(GeneralConstants.DATA_TABLE_NAME);
		cc.setPath(path);
		try {
			// set title, description and photo fields of the content context
			// object
			cc.add(GeneralConstants.KEY_TITLE, titleText);
			cc.add(GeneralConstants.KEY_DESCRIPTION, descriptionText);
			for (int i = 0; i < imagesToBeDeleted.size(); i++) {
				cc.add(GeneralConstants.KEY_PHOTOS + imagesToBeDeleted.get(i), "deleted");
			}
			for (int i = numberOfPhotos; i < (numberOfPhotos + photoPaths.size()); i++) {
				String path = photoPaths.get(i - numberOfPhotos);
				byte[] bytes = HttpUtils.toByteArray(new File(path));
				NetmeraMedia file = new NetmeraMedia(bytes);
				cc.add(GeneralConstants.KEY_PHOTOS + i, file);
			}
			cc.updateInBackground();
		} catch (NetmeraException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error while removing data", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 100;
	}

	private int deleteCc(NetmeraContent ccUpdate) {
		// delete cc object
		NetmeraContent cc = new NetmeraContent(GeneralConstants.DATA_TABLE_NAME);
		cc.setPath(path);
		try {
			globalCc = cc;
			cc.delete();
		} catch (NetmeraException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error while removing data", Toast.LENGTH_SHORT).show();
		}
		return 100;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PHOTO_REQUEST) {
			// if result is returned from gallery photo activity
			if (resultCode == RESULT_OK) {
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
				newImage.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						final AlertDialog.Builder imageDialog = new AlertDialog.Builder(EditContent.this);
						ImageView cloneImage = new ImageView(EditContent.this);
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

				newLayout.addView(newImage);
				newLayout.addView(deleteBtn);
				rootLayout.addView(newLayout);

			}
		} else if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {
			// Photo taken
			// if result is returned from camera photo activity
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
					final AlertDialog.Builder imageDialog = new AlertDialog.Builder(EditContent.this);
					ImageView cloneImage = new ImageView(EditContent.this);
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
}

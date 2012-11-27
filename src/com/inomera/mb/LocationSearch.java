package com.inomera.mb;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.NetmeraCallback;
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraGeoLocation;
import com.netmera.mobile.NetmeraService;
import com.netmera.mobile.NetmeraUser;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;

public class LocationSearch extends MapActivity implements OnClickListener {

	/** Called when the activity is first created. */
	private Button circularSearchBtn;
	private Spinner spinner;
	private Button openSquareBtn;
	private MapView mapView;

	private static final int SQUARESEARCH_REQUEST = 9090;

	double latitude = 0;
	double longitude = 0;
	int distance = 0;
	
	List<NetmeraContent> globalNetmeraContentList = new ArrayList<NetmeraContent>();
	GeoPoint globalGeoPoint;
	
	LocationResult locationResult;
	LocationManager locationManager;
	LocationListener locationListener;
	
	List<Overlay> mapOverlays;
	Drawable redMarker;
	Drawable greenMarker;
	SimpleItemizedOverlay myLocationOverlay;
	SimpleItemizedOverlay smeOverlay;
	
	List<String> pathList;
	List<String> titleList;

	Bundle savedState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.locationsearch);

		// initializing UI elements
		savedState = savedInstanceState;

		circularSearchBtn = (Button) findViewById(R.id.btnLocationSearch);
		circularSearchBtn.setOnClickListener(this);

		openSquareBtn = (Button) findViewById(R.id.btnOpenSquare);
		openSquareBtn.setOnClickListener(this);

		spinner = (Spinner) findViewById(R.id.distances);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.distances_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				
				globalGeoPoint = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
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
		// circular search
		if (clickedItem == circularSearchBtn) {
			globalNetmeraContentList.clear();
			
			// if there is a overlay from previous search, clean it.
			if (!mapView.getOverlays().isEmpty()) {
				mapView.getOverlays().clear();
				mapView.invalidate();
			}
			
			// select circular search radius
			if (spinner.getSelectedItem().toString().compareTo("1km") == 0) {
				distance = 1;
			} else if (spinner.getSelectedItem().toString().compareTo("3km") == 0) {
				distance = 3;
			} else if (spinner.getSelectedItem().toString().compareTo("5km") == 0) {
				distance = 5;
			} else if (spinner.getSelectedItem().toString().compareTo("10km") == 0) {
				distance = 10;
			}
			
			NetmeraGeoLocation ngl = null;
			NetmeraUser nu = null;
			try {
				ngl = new NetmeraGeoLocation(latitude, longitude);
				nu = NetmeraUser.getCurrentUser();
			} catch (NetmeraException e1) {
				e1.printStackTrace();
			}
			
			if (nu != null) {
				NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
				
				netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PUBLIC);
				netmeraService.whereEqual(GeneralConstants.KEY_OWNER, nu.getEmail());
				
				netmeraService.circleSearchInBackground(ngl, distance, "location", new NetmeraCallback< List< NetmeraContent>>() {
				    @Override
				    public void callback(List< NetmeraContent> contentList, NetmeraException exception) {
				        if (contentList != null && exception == null) {
				            // Success
				            for (NetmeraContent content : contentList) {
				                globalNetmeraContentList.add(content);
				            }
				            
				        } else {
				            // Error occurred.
				        }             
				        syncMap();
				    }
				});
			} 
			
		} else if (clickedItem == openSquareBtn) {
			Intent intent = new Intent(LocationSearch.this, SquareSearch.class);
			startActivityForResult(intent, SQUARESEARCH_REQUEST);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// result of square search
		if (requestCode == SQUARESEARCH_REQUEST) {
			if (resultCode == RESULT_OK) {
				Bundle bundle = data.getExtras();

				int zoomLevel = bundle.getInt("zoomLevel");
				int mapCenterLatitude = bundle.getInt("mapCenterLatitude");
				int mapCenterLongitude = bundle.getInt("mapCenterLongitude");
				GeoPoint geoPoint = new GeoPoint(mapCenterLatitude, mapCenterLongitude);

				MapController mapControll = mapView.getController();
				mapView.setBuiltInZoomControls(true);
				mapControll.setZoom(zoomLevel);
				mapControll.animateTo(geoPoint);
				// clear map overlays from previous searches
				if (!mapView.getOverlays().isEmpty()) {
					mapView.getOverlays().clear();
					mapView.invalidate();
				}
				
				// print search results on map
				if (bundle.getInt("resultSize") > 0) {
					titleList = new ArrayList<String>();
					pathList = new ArrayList<String>();

					int totalResults = bundle.getInt("resultSize");

					mapOverlays = mapView.getOverlays();

					redMarker = getResources().getDrawable(R.drawable.marker);
					myLocationOverlay = new SimpleItemizedOverlay(redMarker, mapView);

					GeoPoint point = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
					OverlayItem overlayItem = new OverlayItem(point, "You Are Here", "");
					myLocationOverlay.addOverlay(overlayItem);
					mapOverlays.add(myLocationOverlay);
					
					// second overlay
					greenMarker = getResources().getDrawable(R.drawable.marker2);
					smeOverlay = new SimpleItemizedOverlay(greenMarker, mapView);
					
					try {
						for (int i = 0; i < totalResults; i++) {
							String description = (bundle.getString("resultDescription" + i));
							if (description.length() > 10) {
								description = description.substring(0, 10) + "...";
							}
							GeoPoint resultPoint = new GeoPoint((int) (bundle.getDouble("resultLatitude" + i) * 1E6), (int) (bundle.getDouble("resultLongitude" + i) * 1E6));
							
							OverlayItem resultOverlayItem = new OverlayItem(resultPoint, bundle.getString("resultTitle" + i), description);
							smeOverlay.addOverlay(resultOverlayItem);
							pathList.add(bundle.getString("resultPath" + i));
							titleList.add(bundle.getString("resultTitle" + i));
						}
					} catch (Exception e) {
						System.out.println("Box search data is missing!");
					}
					
					try {
						mapOverlays.add(smeOverlay);
					} catch (Exception e) {
						System.out.println("Overlay add exception!");
					}
				}
			}
		}
	}

	public abstract class LocationResult {
		public abstract void gotLocation(Location location);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public class MarkerOverlay extends Overlay {

		Geocoder geoCoder = null;

		public MarkerOverlay() {
			super();
		}

		@Override
		public boolean onTap(GeoPoint geoPoint, MapView mapView) {
			return super.onTap(geoPoint, mapView);
		}

		@Override
		public void draw(Canvas canvas, MapView mapV, boolean shadow) {

			if (shadow && globalGeoPoint.getLongitudeE6() > 0 && globalGeoPoint.getLatitudeE6() > 0) {
				Projection projection = mapV.getProjection();
				Point pt = new Point();
				projection.toPixels(globalGeoPoint, pt);

				int distanceCoefficient = 1;
				if (distance == 1) {
					distanceCoefficient = 1;
				} else if (distance == 3) {
					distanceCoefficient = 3;
				} else if (distance == 5) {
					distanceCoefficient = 5;
				} else if (distance == 10) {
					distanceCoefficient = 10;
				}

				GeoPoint newGeos = new GeoPoint((int) (globalGeoPoint.getLatitudeE6() + distanceCoefficient * (1E6 / 111)), (int) (globalGeoPoint.getLongitudeE6())); // adjust  your  radius accordingly
				Point pt2 = new Point();
				projection.toPixels(newGeos, pt2);
				float circleRadius = Math.abs(pt2.y - pt.y);

				Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

				circlePaint.setColor(0x30000000);
				circlePaint.setStyle(Style.FILL_AND_STROKE);
				circlePaint.setAlpha(25);
				canvas.drawCircle((float) pt.x, (float) pt.y, circleRadius, circlePaint);

				circlePaint.setColor(0x30000000);
				circlePaint.setStyle(Style.STROKE);
				canvas.drawCircle((float) pt.x, (float) pt.y, circleRadius, circlePaint);

				super.draw(canvas, mapV, shadow);
			}
		}
	}

	public class SimpleItemizedOverlay extends BalloonItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();

		public SimpleItemizedOverlay(Drawable defaultMarker, MapView mapView) {
			super(boundCenter(defaultMarker), mapView);
		}

		public void addOverlay(OverlayItem overlay) {
			m_overlays.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return m_overlays.get(i);
		}

		@Override
		public int size() {
			return m_overlays.size();
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			return super.onTouchEvent(event, mapView);
		}
		
		@Override
		protected boolean onBalloonTap(int index, OverlayItem item) {
			String path = "";
			for (int i = 0; i < titleList.size(); i++) {
				if (titleList.get(i) == item.getTitle()) {
					path = pathList.get(i);
				}
			}

			if (path != "") {
				Intent intent = new Intent(LocationSearch.this, ViewContent.class);
				intent.putExtra(GeneralConstants.KEY_PATH, path);
				startActivityForResult(intent, 9191);
			}
			return true;
		}

	}
	
	private void syncMap(){
		
		titleList = new ArrayList<String>();
		pathList = new ArrayList<String>();
		// create and place an overlay for current position of the user
		mapOverlays = mapView.getOverlays();

		redMarker = getResources().getDrawable(R.drawable.marker);
		myLocationOverlay = new SimpleItemizedOverlay(redMarker, mapView);
		
		//my current location overlay
		try {
			GeoPoint point = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
			OverlayItem overlayItem = new OverlayItem(point, "You Are Here", "");
			myLocationOverlay.addOverlay(overlayItem);
			mapOverlays.add(myLocationOverlay);
		} catch (Exception e) {
			System.out.println("latitude/longitude is null");
		}
		
		//sme overlay
		greenMarker = getResources().getDrawable(R.drawable.marker2);
		smeOverlay = new SimpleItemizedOverlay(greenMarker, mapView);
		
		if (globalNetmeraContentList.size() != 0) {
			try {
				for (int i = 0; i < globalNetmeraContentList.size(); i++) {
					String description = globalNetmeraContentList.get(i).getString("description");
					if (description.length() > 10) {
						description = description.substring(0, 10) + "...";
					}
					
					NetmeraGeoLocation geoLoc = globalNetmeraContentList.get(i).getNetmeraGeoLocation("location");
					
					GeoPoint resultPoint = new GeoPoint((int) (geoLoc.getLatitude() * 1E6), (int) (geoLoc.getLongitude() * 1E6));

					OverlayItem resultOverlayItem = new OverlayItem(resultPoint, globalNetmeraContentList.get(i).getString("title"), description);
					smeOverlay.addOverlay(resultOverlayItem);
					pathList.add(globalNetmeraContentList.get(i).getPath());
					titleList.add(globalNetmeraContentList.get(i).getString("title"));
				} 
			} catch (Exception e) {
				System.out.println("Circular search data is missing!");
			}
			try {
				mapOverlays.add(smeOverlay);
			} catch (Exception e){
				System.out.println("Overlay add exception!");
			}
		}
		
		GeoPoint geoPoint = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
		// set the zoom level according to search radius
		MapController mapControll = mapView.getController();
		mapView.setBuiltInZoomControls(true);

		if (distance == 1) {
			mapControll.setZoom(15);
		} else if (distance == 3) {
			mapControll.setZoom(13);
		} else if (distance == 5) {
			mapControll.setZoom(12);
		} else if (distance == 10) {
			mapControll.setZoom(11);
		}
		mapControll.animateTo(geoPoint);

		// drawing circle
		if (globalGeoPoint != null) {
			mapView.getOverlays().add(new MarkerOverlay());
		}
	}

}

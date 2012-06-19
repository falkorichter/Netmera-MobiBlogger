package com.inomera.mb;

import java.util.ArrayList;
import java.util.List;

import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.ContentContext;
import com.netmera.mobile.ContentService;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraGeoLocation;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Spinner;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;


public class LocationSearch extends MapActivity implements OnItemClickListener,OnClickListener{

	/** Called when the activity is first created. */
	private Button searchBtn;
	private Spinner spinner;
	private ListView listView;
	private Button openSquareBtn;
	private MapView mapView;
	
	private static final int SQUARESEARCH_REQUEST = 9090;
	
	double latitude = 0;
	double longitude = 0;
	private ArrayList<String> listArray = new ArrayList<String>();	
	List<ContentContext> ccList = null;		
	Object selectedItem = 0;
	GeoPoint globalGeoPoint;
	LocationResult locationResult;
	int distance = 0;
	LocationManager locationManager;
	LocationListener locationListener;
	List<Overlay> mapOverlays;
	Drawable drawable;
	Drawable drawable2;
	SimpleItemizedOverlay itemizedOverlay;
	SimpleItemizedOverlay itemizedOverlay2;
	List<String> pathList;
	List<String> titleList;
	
	Bundle savedState;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.locationsearch);
	    
	    //initializing UI elements
	    savedState = savedInstanceState;
	    
	    searchBtn = (Button) findViewById(R.id.btnLocationSearch);
	    searchBtn.setOnClickListener(this);
	    
	    openSquareBtn = (Button) findViewById(R.id.btnOpenSquare);
	    openSquareBtn.setOnClickListener(this);
	    
	    spinner = (Spinner) findViewById(R.id.distances);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.distances_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    
	    listView = (ListView) findViewById(R.id.lvLocationSearch);
        //Setting UI attributes 
		listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listArray));
		listView.setOnItemClickListener(this);
	    
		
		mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);		
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		      // Called when a new location is found by the network location provider.
		    	latitude = location.getLatitude();
		    	longitude = location.getLongitude();
		    	
		    	globalGeoPoint = new GeoPoint((int)(latitude* 1E6), (int)(longitude* 1E6));
		    	locationManager.removeUpdates(locationListener);
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}
	
	@Override
	public void onClick(View arg0) {		
		//circular search
		if(arg0 == searchBtn) {			
			//if there is a overlay from previous search, clean it.
			if(!mapView.getOverlays().isEmpty()){
				mapView.getOverlays().clear();
				mapView.invalidate();
			}
			ContentService cs = new ContentService(GeneralConstants.DATA_TABLE_NAME);
			Log.v("latitude", "" + latitude);
			Log.v("longitude", "" + longitude);
			NetmeraGeoLocation ngl = new NetmeraGeoLocation(latitude,longitude); 
			
			//select circular search radius
			try {				
				if(spinner.getSelectedItem().toString().compareTo("1km") == 0) {
					distance = 1;
				} else if(spinner.getSelectedItem().toString().compareTo("3km") == 0) {
					distance = 3;
				} else if(spinner.getSelectedItem().toString().compareTo("5km") == 0) {
					distance = 5;
				} else if(spinner.getSelectedItem().toString().compareTo("10km") == 0) {
					distance = 10;
				}
				//make circular search with only current location and radius
				ccList = cs.circleSearch(ngl, distance, "location");
				
				listArray.clear();
				for(ContentContext cc : ccList) {
					listArray.add(cc.getString(GeneralConstants.KEY_TITLE));
				}	
				
				titleList = new ArrayList<String>();
				pathList = new ArrayList<String>();
				//create and place an overlay for current position of the user
				mapOverlays = mapView.getOverlays();
				
				drawable = getResources().getDrawable(R.drawable.marker);
				itemizedOverlay = new SimpleItemizedOverlay(drawable, mapView);
				
				GeoPoint point = new GeoPoint((int)(latitude*1E6),(int)(longitude*1E6));
				OverlayItem overlayItem = new OverlayItem(point, "You Are Here", "");
				itemizedOverlay.addOverlay(overlayItem);				
				mapOverlays.add(itemizedOverlay);
				
				// second overlay				
				drawable2 = getResources().getDrawable(R.drawable.marker2);
				itemizedOverlay2 = new SimpleItemizedOverlay(drawable2, mapView);
				
				for(int i=0; i<ccList.size(); i++) {
					String descript = ccList.get(i).getContent().getData().get("description");
					if(descript.length() > 10){
						descript = descript.substring(0,10) + "...";
					}
					GeoPoint resultPoint = new GeoPoint((int)(Double.parseDouble(ccList.get(i).getContent().getData().get("location_netmera_mobile_latitude")) * 1E6), (int)(Double.parseDouble(ccList.get(i).getContent().getData().get("location_netmera_mobile_longitude")) * 1E6));
					
					OverlayItem resultOverlayItem = new OverlayItem(resultPoint, ccList.get(i).getContent().getData().get("title"), descript);
					itemizedOverlay2.addOverlay(resultOverlayItem);
					pathList.add(ccList.get(i).getContent().getPath());
					titleList.add(ccList.get(i).getContent().getData().get("title"));
				}
				
				mapOverlays.add(itemizedOverlay2);
				
				if (arg0 == null) {
					
					final MapController mc = mapView.getController();
					//mc.animateTo(point2);
					mc.animateTo(point);
					if(distance == 1) {
						mc.setZoom(16);
					} else if(distance == 3) {
						mc.setZoom(15);
					} else if(distance == 4) {
						mc.setZoom(14);
					} else if(distance == 10) {
						mc.setZoom(13);
					}
				} else {
					
				}
				
				GeoPoint geoPoint = new GeoPoint((int)(latitude * 1E6),(int)(longitude * 1E6));          
				//set the zoom level according to search radius
				MapController mapControll= mapView.getController();
	            mapView.setBuiltInZoomControls(true);
	            mapView.setStreetView(true);
	            if(distance == 1) {
	            	mapControll.setZoom(15);
	            } else if(distance == 3) {
	            	mapControll.setZoom(13);
	            } else if(distance == 5) {
	            	mapControll.setZoom(12);
	            } else if(distance == 10) {
	            	mapControll.setZoom(11);
	            }
	            mapControll.animateTo(geoPoint);
	            
				//drawing circle
				Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(b);
				if(globalGeoPoint!=null)
				{
					mapView.getOverlays().add(new MarkerOverlay());
				}
			} catch (NetmeraException e) {				
				e.printStackTrace();
			}
		} else if(arg0 == openSquareBtn) {
			Intent intent = new Intent(LocationSearch.this, SquareSearch.class);
			startActivityForResult(intent, SQUARESEARCH_REQUEST);
		}
	}


	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//result of square search
		if (requestCode == SQUARESEARCH_REQUEST) {
			if (resultCode == RESULT_OK) {
				//show on map
				Bundle b = data.getExtras();
				
				int zoomLevel = b.getInt("zoomLevel");
				int mapCenterLatitude = b.getInt("mapCenterLatitude");
				int mapCenterLongitude = b.getInt("mapCenterLongitude");
				GeoPoint geoPoint = new GeoPoint(mapCenterLatitude, mapCenterLongitude);          
				
				MapController mapControll= mapView.getController();
	            mapView.setBuiltInZoomControls(true);
	            mapView.setStreetView(true);
	            mapControll.setZoom(zoomLevel);
	            mapControll.animateTo(geoPoint);
				//clear map overlays from previous searches
				if(!mapView.getOverlays().isEmpty()){
					mapView.getOverlays().clear();
					mapView.invalidate();
				}
				//print search results on map
				if(b.getInt("resultSize") > 0) {
					titleList = new ArrayList<String>();
					pathList = new ArrayList<String>();
					
					int totalResults = b.getInt("resultSize");					
		            
		            mapOverlays = mapView.getOverlays();
					
					drawable = getResources().getDrawable(R.drawable.marker);
					itemizedOverlay = new SimpleItemizedOverlay(drawable, mapView);
					
					GeoPoint point = new GeoPoint((int)(latitude*1E6),(int)(longitude*1E6));
					OverlayItem overlayItem = new OverlayItem(point, "You Are Here", "");
					itemizedOverlay.addOverlay(overlayItem);				
					mapOverlays.add(itemizedOverlay);
					
					// second overlay
					
					drawable2 = getResources().getDrawable(R.drawable.marker2);
					itemizedOverlay2 = new SimpleItemizedOverlay(drawable2, mapView);
					
					for(int i = 0; i<totalResults; i++) {
						String descript = (b.getString("resultDescription" + i));
						if(descript.length() > 10){
							descript = descript.substring(0,10) + "...";
						}
						GeoPoint resultPoint = new GeoPoint((int)(Double.parseDouble(b.getString("resultLatitude"+i)) * 1E6), (int)(Double.parseDouble(b.getString("resultLongitude" + i)) * 1E6));
						OverlayItem resultOverlayItem = new OverlayItem(resultPoint, b.getString("resultTitle"+i), descript);
						itemizedOverlay2.addOverlay(resultOverlayItem);
						pathList.add(b.getString("resultPath"+i));
						titleList.add(b.getString("resultTitle" + i));
					}
					
					mapOverlays.add(itemizedOverlay2);
				}
			}
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
	}
	
	
	
	public abstract class LocationResult{
		public abstract void gotLocation(Location location);
	}
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	
	public class CircleOverlay extends Overlay{

	    Context context;
	    double mLat;
	    double mLon;

	     public CircleOverlay(Context _context, double _lat, double _lon ) {
	            context = _context;
	            mLat = _lat;
	            mLon = _lon;
	     }

	     public void draw(Canvas canvas, MapView mapView, boolean shadow) {
	         super.draw(canvas, mapView, shadow);
	         Projection projection = mapView.getProjection();
	         Point pt = new Point();
	         GeoPoint geo = new GeoPoint((int) (latitude *1e6), (int)(longitude * 1e6));
	         projection.toPixels(geo ,pt);
	         float circleRadius = 100;
	         Paint innerCirclePaint;
	         innerCirclePaint = new Paint();
	         innerCirclePaint.setARGB(255, 255, 255, 255);
	         innerCirclePaint.setAntiAlias(true);
	         innerCirclePaint.setStyle(Paint.Style.FILL);
	         canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, innerCirclePaint);
	        }
	}
	
	
	public class MarkerOverlay extends Overlay {

	    Geocoder geoCoder = null;

	    public MarkerOverlay() {
	        super();
	    }


	    @Override
	    public boolean onTap(GeoPoint geoPoint, MapView mapView){
	        return super.onTap(geoPoint,mapView);
	    }

	    @Override
	    public void draw(Canvas canvas, MapView mapV, boolean shadow){

	        if(shadow && globalGeoPoint.getLongitudeE6() >0 && globalGeoPoint.getLatitudeE6() > 0){
	            Projection projection = mapV.getProjection();
	            Point pt = new Point();
	            projection.toPixels(globalGeoPoint,pt);
	            
	            int distanceCoefficient = 1;
	            if(distance == 1) {
	            	distanceCoefficient = 1;
	            } else if(distance == 3) {
	            	distanceCoefficient = 3;
	            } else if(distance == 5) {
	            	distanceCoefficient = 5;
	            } else if(distance == 10) {
	            	distanceCoefficient = 10;
	            }  
	           
	            GeoPoint newGeos = new GeoPoint((int)(globalGeoPoint.getLatitudeE6() + distanceCoefficient*(1E6/111)), (int)(globalGeoPoint.getLongitudeE6())); // adjust your radius accordingly
	            Point pt2 = new Point();
	            projection.toPixels(newGeos,pt2);
	            float circleRadius = Math.abs(pt2.y-pt.y);

	            Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	            circlePaint.setColor(0x30000000);
	            circlePaint.setStyle(Style.FILL_AND_STROKE);
	            circlePaint.setAlpha(25);
	            canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, circlePaint);

	            circlePaint.setColor(0x30000000);
	            circlePaint.setStyle(Style.STROKE);
	            canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, circlePaint);

	            super.draw(canvas,mapV,shadow);
	        }
	    }
	}
	
	public class SimpleItemizedOverlay extends BalloonItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();
		private Context c;
		
		public SimpleItemizedOverlay(Drawable defaultMarker, MapView mapView) {
			super(boundCenter(defaultMarker), mapView);
			c = mapView.getContext();
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
		protected boolean onBalloonTap(int index, OverlayItem item) {
			String path = "";
			for(int i = 0; i<titleList.size(); i++) {
				if(titleList.get(i) == item.getTitle()) {
					path = pathList.get(i);
				}
			}
			
			if(path != "") {
				Intent intent = new Intent(LocationSearch.this, ViewContent.class);
				intent.putExtra(GeneralConstants.KEY_PATH, path);
				startActivityForResult(intent , 9191);
			}
			return true;
		}
		
	}


}

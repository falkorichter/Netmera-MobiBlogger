package com.inomera.mb;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.inomera.mb.constans.GeneralConstants;
import com.netmera.mobile.NetmeraCallback;
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraGeoLocation;
import com.netmera.mobile.NetmeraService;
import com.netmera.mobile.NetmeraUser;

public class SquareSearch extends MapActivity implements OnClickListener {

	private MapView mapView;
	MapController kontrol;
	float xs, ys, xe, ye;
	GeoPoint start, end;

	private Button btnSearch;

	List<Overlay> mapOverlays;
	Drawable drawable;
	Drawable drawable2;

	List<NetmeraContent> globalNetmeraContentList = new ArrayList<NetmeraContent>();

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.squaresearch);

		mapView = (MapView) findViewById(R.id.mapviewsquare);

		btnSearch = (Button) findViewById(R.id.btnSquareSearch);
		btnSearch.setOnClickListener(this);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onClick(View clickedItem) {
		if (clickedItem == btnSearch) {
			// get corner points from the map
			Projection proj = mapView.getProjection();
			GeoPoint topLeft = proj.fromPixels(0, 0);

			GeoPoint bottomRight = proj.fromPixels(mapView.getWidth() - 1, mapView.getHeight() - 1);

			double topLat = topLeft.getLatitudeE6() / 1E6;
			double topLon = topLeft.getLongitudeE6() / 1E6;
			double bottomLat = bottomRight.getLatitudeE6() / 1E6;
			double bottomLon = bottomRight.getLongitudeE6() / 1E6;

			NetmeraGeoLocation firstPoint = null;
			NetmeraGeoLocation secondPoint = null;
			NetmeraUser nu = null;
			try {
				firstPoint = new NetmeraGeoLocation(topLat, topLon);
				secondPoint = new NetmeraGeoLocation(bottomLat, bottomLon);
				nu = NetmeraUser.getCurrentUser();
			} catch (NetmeraException e1) {
				e1.printStackTrace();
			}
			final NetmeraGeoLocation firstPointMap = firstPoint;
			final NetmeraGeoLocation secondPointMap = secondPoint;

			//TODO : spinner ?
			if (nu != null && firstPoint != null && secondPoint != null) {
				NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);

				netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PRIVATE);
				netmeraService.whereEqual(GeneralConstants.KEY_OWNER, nu.getEmail());

				netmeraService.boxSearchInBackground(firstPoint, secondPoint, "location", new NetmeraCallback< List< NetmeraContent>>() {
					@Override
					public void callback(List< NetmeraContent> contentList, NetmeraException exception) {
						if (contentList != null && exception == null) {
							// Success
							for (NetmeraContent netmeraContent : contentList) {
								globalNetmeraContentList.add(netmeraContent);
							}

							searchBlogWithoutUser(firstPointMap, secondPointMap);
						} else {
							// Error occurred.
						}           
					}
				});
			} 

		}
	}

	private void searchBlogWithoutUser(NetmeraGeoLocation firstPoint, NetmeraGeoLocation secondPoint){
		NetmeraService netmeraService = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
		netmeraService.whereEqual(GeneralConstants.KEY_PRIVACY, GeneralConstants.PRIVACY_PUBLIC);

		netmeraService.boxSearchInBackground(firstPoint, secondPoint, "location", new NetmeraCallback< List< NetmeraContent>>() {
			@Override
			public void callback(List< NetmeraContent> contentList, NetmeraException exception) {
				if (contentList != null && exception == null) {
					// Success
					for (NetmeraContent netmeraContent : contentList) {
						globalNetmeraContentList.add(netmeraContent);
					}
					returnIntent();
				} else {
					// Error occurred.
				}           
			}
		});
	}
	
	private void returnIntent(){
		Intent intent = new Intent();

		intent.putExtra("zoomLevel", mapView.getZoomLevel());
		intent.putExtra("mapCenterLatitude", mapView.getMapCenter().getLatitudeE6());
		intent.putExtra("mapCenterLongitude", mapView.getMapCenter().getLongitudeE6());
		intent.putExtra("resultSize", globalNetmeraContentList.size());

		for (int i = 0; i < globalNetmeraContentList.size(); i++) {
			try {
				intent.putExtra("resultTitle" + i, globalNetmeraContentList.get(i).getString("title"));
				intent.putExtra("resultPath" + i, globalNetmeraContentList.get(i).getPath());
				intent.putExtra("resultDescription" + i, globalNetmeraContentList.get(i).getString("description"));
				NetmeraGeoLocation geoLoc = globalNetmeraContentList.get(i).getNetmeraGeoLocation("location");

				intent.putExtra("resultLatitude" + i, geoLoc.getLatitude());
				intent.putExtra("resultLongitude" + i, geoLoc.getLongitude());
			} catch (NetmeraException e) {
				e.printStackTrace();
			}
		}

		setResult(RESULT_OK, intent);

		finish();
	}
}

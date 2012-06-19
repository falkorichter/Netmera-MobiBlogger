package com.inomera.mb;

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
import com.netmera.mobile.NetmeraContent;
import com.netmera.mobile.NetmeraException;
import com.netmera.mobile.NetmeraGeoLocation;
import com.netmera.mobile.NetmeraService;

public class SquareSearch extends MapActivity implements OnClickListener {

	private MapView mapView;
	MapController kontrol;
	float xs, ys, xe, ye;
	GeoPoint start, end;

	private Button btnSearch;

	List<Overlay> mapOverlays;
	Drawable drawable;
	Drawable drawable2;

	List<NetmeraContent> ccList = null;

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
	public void onClick(View v) {
		if (v == btnSearch) {
			// get corner points from the map
			Projection proj = mapView.getProjection();
			GeoPoint topLeft = proj.fromPixels(0, 0);

			GeoPoint bottomRight = proj.fromPixels(mapView.getWidth() - 1, mapView.getHeight() - 1);

			double topLat = topLeft.getLatitudeE6() / 1E6;
			double topLon = topLeft.getLongitudeE6() / 1E6;
			double bottomLat = bottomRight.getLatitudeE6() / 1E6;
			double bottomLon = bottomRight.getLongitudeE6() / 1E6;

			NetmeraGeoLocation firstPoint = new NetmeraGeoLocation(topLat, topLon);
			NetmeraGeoLocation secondPoint = new NetmeraGeoLocation(bottomLat, bottomLon);

			NetmeraService cs = new NetmeraService(GeneralConstants.DATA_TABLE_NAME);
			try {
				// perform square search from those 2 points
				ccList = cs.boxSearch(firstPoint, secondPoint, "location");
			} catch (NetmeraException e) {
				e.printStackTrace();
			}
			Intent intent = new Intent();
			intent.putExtra("zoomLevel", mapView.getZoomLevel());
			intent.putExtra("mapCenterLatitude", mapView.getMapCenter().getLatitudeE6());
			intent.putExtra("mapCenterLongitude", mapView.getMapCenter().getLongitudeE6());

			intent.putExtra("resultSize", ccList.size());
			for (int i = 0; i < ccList.size(); i++) {
				try {
					intent.putExtra("resultTitle" + i, ccList.get(i).getString("title"));
					intent.putExtra("resultPath" + i, ccList.get(i).getPath());
					intent.putExtra("resultDescription" + i, ccList.get(i).getString("description"));
					intent.putExtra("resultLatitude" + i, ccList.get(i).getDouble("location_netmera_mobile_latitude"));
					intent.putExtra("resultLongitude" + i, ccList.get(i).getDouble("location_netmera_mobile_longitude"));
				} catch (NetmeraException e) {
					e.printStackTrace();
				}
			}

			setResult(RESULT_OK, intent);

			finish();
		}

	}

}

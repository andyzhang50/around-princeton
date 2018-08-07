package com.cos333.aroundprinceton;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapViewFragment extends Fragment {

    MapView mMapView;
    private GoogleMap mMap;
    private OnMapInteractionListener mListener;

    private int mMapViewHeight;
    private int mMapViewWidth;

    private LatLngBounds.Builder mBuilder;
    private LatLngBounds mFullBounds;
    private LatLngBounds mHalfBounds;

    private double scalingFactor = 1.3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_view, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }


        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                LatLng Princeton = new LatLng(40.346389, -74.657829);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Princeton, 14f));
//                mMap.setMinZoomPreference(14f);
//                mMap.setMaxZoomPreference(18f);


                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        mListener.onMapItemClick(marker);
                        return false;
                    }
                });

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        mListener.onMapItemClick(null);
                    }
                });



            }


        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ResultsListFragment.OnResultsListInteractionListener) {
            mListener = (OnMapInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapInteractionListener");
        }
    }

    public void setHeightWidth(int height, int width) {
        mMapViewHeight = height;
        mMapViewWidth = width;
    }

    public void expand() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mFullBounds, 120), 200, null);
    }

    public void shrink() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mHalfBounds, 120), 200, null);
    }

    public synchronized void update(boolean refocus) {
        int counter = 0;
        mMap.clear();
//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        mBuilder = new LatLngBounds.Builder();

        for (Facility facility : SearchResults.mResults) {
            double lat = facility.getLat();
            double lon = facility.getLon();
            LatLng latLng = new LatLng(facility.getLat(), facility.getLon());

            MarkerOptions markerOptions =
                    new MarkerOptions().position(latLng);

            markerOptions.title(facility.getBuilding());
//            markerOptions.snippet(facility.getDetails());

            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(facility);
//            builder.include(marker.getPosition());
//            builder.include(latLng);
            mBuilder.include(latLng);
            counter++;

        }


        if (refocus && SearchResults.mResults.size() > 0) {
            mFullBounds = mBuilder.build();
            LatLng northEast = mFullBounds.northeast;
            LatLng southWest = mFullBounds.southwest;
            double latDiff = northEast.latitude - southWest.latitude;
            double lonDiff = northEast.longitude - southWest.longitude;
            double ratio = latDiff * scalingFactor * mMapViewWidth / lonDiff / mMapViewHeight;
            double diff;
//            double diff = (mFullBounds.northeast.latitude - mFullBounds.southwest.latitude) * 50;
            if (ratio > 1) {
                diff = latDiff * 1.5;
            } else {
                diff = lonDiff / mMapViewWidth * mMapViewHeight / scalingFactor ;
            }
            mBuilder.include(new LatLng(mFullBounds.southwest.latitude - diff, mFullBounds.southwest.longitude));
            mHalfBounds = mBuilder.build();
//            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mHalfBounds, 120));
            shrink();

        }
    }


    public void update(Facility facility, boolean isHalf) {
//        mMap.clear();

            LatLng latLng = new LatLng(facility.getLat(), facility.getLon());

            MarkerOptions markerOptions =
                    new MarkerOptions().position(latLng);

            markerOptions.title(facility.getBuilding());
//            markerOptions.snippet(facility.getDetails());

            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(facility);

        marker.showInfoWindow();
        if (isHalf) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(latLng);
            builder.include(new LatLng(facility.getLat() + 0.001, facility.getLon()));
            builder.include(new LatLng(facility.getLat() - 0.003, facility.getLon()));
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), 200, null);
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 200, null);
        }


//        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public interface OnMapInteractionListener {
        public void onMapItemClick(Marker marker);


    }

//    public void getDirections(LatLng startLatLng, LatLng endLatLng){
//        String urlTopass = makeURL(startLatLng.latitude,
//                startLatLng.longitude, endLatLng.latitude,
//                endLatLng.longitude);
//        new connectAsyncTask(urlTopass).execute();
//    }
//
//    public String makeURL(double sourcelat, double sourcelog, double destlat,
//                          double destlog) {
//        StringBuilder urlString = new StringBuilder();
//        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
//        urlString.append("?origin=");// from
//        urlString.append(Double.toString(sourcelat));
//        urlString.append(",");
//        urlString.append(Double.toString(sourcelog));
//        urlString.append("&destination=");// to
//        urlString.append(Double.toString(destlat));
//        urlString.append(",");
//        urlString.append(Double.toString(destlog));
//        urlString.append("&sensor=false&mode=driving&alternatives=true");
//        return urlString.toString();
//    }
//
//    public void drawPath(String result) {
//        if (line != null) {
//            mMap.clear();
//        }
//        mMap.addMarker(new MarkerOptions().position(endLatLng).icon(
//                BitmapDescriptorFactory.fromResource(R.drawable.redpin_marker)));
//        mMap.addMarker(new MarkerOptions().position(startLatLng).icon(
//                BitmapDescriptorFactory.fromResource(R.drawable.redpin_marker)));
//        try {
//            // Tranform the string into a json object
//            final JSONObject json = new JSONObject(result);
//            JSONArray routeArray = json.getJSONArray("routes");
//            JSONObject routes = routeArray.getJSONObject(0);
//            JSONObject overviewPolylines = routes
//                    .getJSONObject("overview_polyline");
//            String encodedString = overviewPolylines.getString("points");
//            List<LatLng> list = decodePoly(encodedString);
//
//            PolylineOptions options = new PolylineOptions().width(3).color(Color.BLUE).geodesic(true);
//            for (int z = 0; z < list.size(); z++) {
//                LatLng point = list.get(z);
//                options.add(point);
//            }
//            line = mMap.addPolyline(options);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private List<LatLng> decodePoly(String encoded) {
//
//        List<LatLng> poly = new ArrayList<LatLng>();
//        int index = 0, len = encoded.length();
//        int lat = 0, lng = 0;
//
//        while (index < len) {
//            int b, shift = 0, result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lat += dlat;
//
//            shift = 0;
//            result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lng += dlng;
//
//            LatLng p = new LatLng((((double) lat / 1E5)),
//                    (((double) lng / 1E5)));
//            poly.add(p);
//        }
//
//        return poly;
//    }

}

package com.cos333.aroundprinceton;

import com.cos333.aroundprinceton.Facility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchResults {

    public static List<Facility> mResults = new ArrayList<Facility>();
    public static List<Facility> mPrevResults;

    public static String mDetails;
    public static String mNoDetails = "No facilities found";
    public static void clear() {
        mResults = new ArrayList<>();
    }

    public static void addItem(Facility facility) {
        mResults.add(facility);
    }


    public static void saveResults() {
        mPrevResults = mResults;
    }

    public static void rewindResults() {
        mResults = mPrevResults;
    }

    public static void updateResults(String response, String queryString) {
        boolean isBuilding = true;
        if (queryString.equals("laundry") ||
                queryString.equals("printer") ||
                queryString.equals("kitchen") ||
                queryString.equals("library") ||
                queryString.equals("food")) isBuilding = false;

        try {
            JSONObject obj = new JSONObject(response);
            JSONArray arrayOfResults = obj.getJSONArray("facilities");
            mResults = new ArrayList<>();
            for (int i = 0; i < arrayOfResults.length(); i++) {
                JSONObject thisResult = arrayOfResults.getJSONObject(i);
                Facility facility = new Facility();
                facility.setBuilding(thisResult.getString("2").trim());
                facility.setDetails(thisResult.getString("3").trim());
                if (isBuilding) {
                    facility.setType("building");
                } else {
                    facility.setType(queryString);
                }
                if (thisResult.isNull("4")) continue;
                if (thisResult.isNull("5")) continue;
                facility.setLat(thisResult.getDouble("4"));
                facility.setLon(thisResult.getDouble("5"));
                addItem(facility);
            }
            SearchResults.saveResults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateResultsAuto(String response) {
        try {
            JSONObject obj = new JSONObject(response);
            JSONArray arrayOfResults = obj.getJSONArray("terms");
            mResults = new ArrayList<>();
            for (int i = 0; i < arrayOfResults.length(); i++) {
                JSONObject thisResult = arrayOfResults.getJSONObject(i);
                Facility facility = new Facility();
                facility.setBuilding(thisResult.getString("0").trim());
                facility.setLat(thisResult.getDouble("lat"));
                facility.setLon(thisResult.getDouble("lon"));
                facility.setDetails("");
                addItem(facility);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateResultsDetails(String response) {

        mDetails = "";

        try {
            JSONObject obj = new JSONObject(response);
            JSONArray arrayOfResults = obj.getJSONArray("facilities");
            if (arrayOfResults.length() > 0) {
                mDetails = "Facilities: \n";
            }
            mResults = new ArrayList<>();
            for (int i = 0; i < arrayOfResults.length(); i++) {
                JSONObject thisResult = arrayOfResults.getJSONObject(i);
                mDetails = mDetails + thisResult.getString("facility_type_name") + ": " + thisResult.getString("description") + "\n";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package com.cos333.aroundprinceton;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Stack;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
        implements ResultsListFragment.OnResultsListInteractionListener,
        MenuListFragment.OnMenuInteractionListener,
        MapViewFragment.OnMapInteractionListener {

    // The states that the application can be in at run time.
    private final int STATE_HOME = 0;           // the home state
    private final int STATE_SEARCH = 1;         // the state where the search bar has focus
    private final int STATE_LIST_EXPANDED = 2;  // the state where there is a visible list of
                                                // search results
    private final int STATE_LIST_HIDDEN = 3;    // the state where there are visible search results
                                                // on the map but the results list is hidden
    private final int STATE_ITEM_SIMPLE = 4;    // the state where a simple item among the results
                                                // is being focused
    private final int STATE_ITEM_DETAILED = 5;  // the state where an item with details (such as
                                                // libraries and buildings) is being focused

    // the search keywords corresponding to each item on the drop-down menu
    public final String[] MENU_ITEMS = {"kitchen", "laundry", "printer", "library", "food"};

    private int mState; // the current state of the application
    private Stack<Integer> mStateStack; // stack for keeping track of previous states

    private EditText mQueryText;                      // the search bar
    private Button mMenuButton;                       // drop-down menu button
    private Button mBackButton;                       // back button
    private Button mSearchButton;                     // search button
    private Button mXButton;                          // "X" (clear or delete) button
    private Button mShowListButton;                   // The show-list button

    private Fragment mMenuListFragment;               // fragment for the drop-down menu
    private MapViewFragment mMapViewFragment;         // fragment for the map
    private ResultsListFragment mResultsListFragment; // fragment for the results list
    private FrameLayout mListViewLayout;              // layout containing mResultsListFragment
    private FrameLayout mMapExpandLayout;             // dummy layout whose purpose is to catch a
                                                      // touch on screen

    // the view for showing info of an item with details
    private RelativeLayout mDetailedItemContainer;
    private TextView mItemNameDetailed;
    private TextView mItemDescriptionDetailed;

    // the view for showing info of a simple item
    private RelativeLayout mSimpleItemContainer;
    private TextView mItemNameSimple;
    private TextView mItemDescriptionSimple;

    private Facility mFacility;  // the currently selected facility item

    // Animations
    private ValueAnimator mListNoneToHalfAnimation;
    private ValueAnimator mListHalfToNoneAnimation;
    private ValueAnimator mListNoneToFullAnimation;
    private ValueAnimator mListFullToNoneAnimation;
    private ValueAnimator mListHalfToFullAnimation;
    private ValueAnimator mListFullToHalfAnimation;


    private boolean mIsBuilding;
    private String mPrevQueryString;


    // An AsyncTask that performs network queries to the server
    private class SearchTask extends AsyncTask<String, Void, String> {
        private String mQueryText;
        private String mResult;
        private boolean mIsBuilding;
        private boolean mIsDetails;

        public SearchTask(String queryText, boolean isBuilding, boolean isDetails) {
            mQueryText = queryText;
            mIsBuilding = isBuilding;
            mIsDetails = isDetails;
        }

        protected String doInBackground(String... unused) {
            try {
                URL url;
                if (mIsBuilding) {
                    url = new URL("https://around-princeton.herokuapp.com/auto?term=" + mQueryText);
                } else if (mIsDetails) {
                    url = new URL("https://around-princeton.herokuapp.com/building?name=" + mQueryText);
                } else {
                    url = new URL("https://around-princeton.herokuapp.com/?facil=" + mQueryText);
                }
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();
                int responseCode = con.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK)
                    throw new IOException("HTTP error code: " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String inputLine;
                StringBuilder sb = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine.trim());
                }
                in.close();
                mResult = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (mIsBuilding) SearchResults.updateResultsAuto(mResult);
            else if (mIsDetails) SearchResults.updateResultsDetails(mResult);
            else SearchResults.updateResults(mResult, mQueryText);
            if (!mIsDetails) {
                mResultsListFragment.update();
                mMapViewFragment.update(true);
            }
        }
    }

    private class SearchBarTextWatcher implements TextWatcher {
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
        public void afterTextChanged(Editable s) {
            String queryTextString = mQueryText.getText().toString().trim();
            if (queryTextString.length() == 0 && mXButton.getVisibility() == View.VISIBLE) {
                mXButton.setVisibility(View.INVISIBLE);
            } else if (queryTextString.length() > 0 && mXButton.getVisibility() == View.INVISIBLE) {
                mXButton.setVisibility(View.VISIBLE);
            }
            if (queryTextString.length() == 0 && mSearchButton.getVisibility() == View.VISIBLE) {
                mSearchButton.setVisibility(View.INVISIBLE);
            } else if (queryTextString.length() > 0 && mSearchButton.getVisibility() == View.INVISIBLE) {
                mSearchButton.setVisibility(View.VISIBLE);
            }
            SearchTask searchTask = new SearchTask(mQueryText.getText().toString().trim(), true, false);
            searchTask.execute();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mState = STATE_HOME;
        mStateStack = new Stack<>();
        mIsBuilding = true;

        // initialize the fragment contanining menu items list and then hide it
        mMenuListFragment = new MenuListFragment();
        android.app.FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.menu_fragment_container, mMenuListFragment);
        fragmentTransaction.hide(mMenuListFragment);
        fragmentTransaction.commit();

        // get the menu button and define its OnClickListener. the OnClickListener tells the
        // buttonw what to do when it's clicked. in this case we want to show/hide the menu items
        // list when the menu button is clicked
        mMenuButton = (Button) findViewById(R.id.menuButton);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
                if (mMenuListFragment.isHidden()) {
                    ft.show(mMenuListFragment).commit();
                } else {
                    ft.hide(mMenuListFragment).commit();
                }
            }
        });

        // initialize the back button and hide it
        mBackButton = (Button) findViewById(R.id.backButton);
        mBackButton.setVisibility(View.INVISIBLE);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = mStateStack.pop();
                if (mState == STATE_SEARCH && state != STATE_HOME) {
                    SearchResults.rewindResults();
                    mQueryText.setText(mPrevQueryString);
                    mResultsListFragment.update();
                    if (state == STATE_LIST_EXPANDED) {
                        mMapViewFragment.shrink();
                    } else if (state == STATE_LIST_HIDDEN) {
                        mMapViewFragment.expand();
                    }
                }

                switchStates(state);
            }
        });

        // get the search button. Define its OnClickListener such that when clicked, we take the
        // text in mQueryText and feed that string to a new SearchThread, which performs a query
        // and then updates the results to SearchResults
        mSearchButton = (Button) findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMapViewFragment.update(true);
                switchStates(STATE_LIST_EXPANDED);
            }
        });
        mSearchButton.setVisibility(View.INVISIBLE);

        // X button
        mXButton = (Button) findViewById(R.id.xButton);
        mXButton.setVisibility(View.INVISIBLE);
        mXButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mXButton.setVisibility(View.INVISIBLE);
                if (mState == STATE_SEARCH) {
                    mQueryText.getText().clear();
                } else {
                    switchStates(STATE_HOME);
                }
            }
        });

        // initialize the search bar editText
        mQueryText = (EditText) findViewById(R.id.queryText);
        mQueryText.addTextChangedListener(new SearchBarTextWatcher());

        mQueryText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mState != STATE_SEARCH) {
                    mStateStack.push(mState);

                    switchStates(STATE_SEARCH);
                    mIsBuilding = true;
                }
                return false;
            }
        });
        mQueryText.clearFocus();

        // Create a new Fragment to be placed in the activity layout
        mMapViewFragment = new MapViewFragment();
        mResultsListFragment = new ResultsListFragment();


        // load map and list fragment
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.map_fragment_container, mMapViewFragment);
        ft.add(R.id.list_fragment_container, mResultsListFragment);
        ft.commit();

        mListViewLayout = (FrameLayout) findViewById(R.id.list_fragment_container);

        mMapExpandLayout = (FrameLayout) findViewById(R.id.map_expand_layout);
        mMapExpandLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mMapViewFragment.expand();
                if (mState == STATE_ITEM_DETAILED) {
                    switchStates(STATE_ITEM_SIMPLE);
                } else {
                    switchStates(STATE_LIST_HIDDEN);
                }
                mMapExpandLayout.setVisibility(View.INVISIBLE);
                return true;
            }
        });
        mMapExpandLayout.setVisibility(View.INVISIBLE);

        mDetailedItemContainer = (RelativeLayout) findViewById(R.id.detailed_item_container);
        mDetailedItemContainer.setVisibility(View.INVISIBLE);
        mItemNameDetailed = (TextView) findViewById(R.id.item_name_detailed);
        mItemDescriptionDetailed = (TextView) findViewById(R.id.item_description_detailed);
        mItemDescriptionDetailed.setMovementMethod(new ScrollingMovementMethod());

        mSimpleItemContainer = (RelativeLayout) findViewById((R.id.simple_item_container));
        mSimpleItemContainer.setVisibility(View.INVISIBLE);
        mItemNameSimple = (TextView) findViewById(R.id.item_name_simple);
        mItemDescriptionSimple = (TextView) findViewById(R.id.item_description_simple);
        mSimpleItemContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (hasDetails()) {
                    mStateStack.push(mState);
                    switchStates(STATE_ITEM_DETAILED);
                    mMapViewFragment.update(mFacility, true);
                }
                return true;
            }
        });

        // get the toggleListMapButton and tell it what to do
        mShowListButton = (Button) findViewById(R.id.showListButton);
        mShowListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapViewFragment.shrink();
                switchStates(STATE_LIST_EXPANDED);
            }
        });
        mShowListButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && mListNoneToHalfAnimation == null) {
            initializeAnimations();
            FrameLayout mapFragmentContainer = (FrameLayout) findViewById(R.id.map_fragment_container);
            mMapViewFragment.setHeightWidth(mapFragmentContainer.getHeight(), mapFragmentContainer.getWidth());
        }


    }

    private void initializeAnimations() {
        int listHalfHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, getResources().getDisplayMetrics());
        int listFullHeight = findViewById(R.id.map_expand_layout).getHeight();
        mListNoneToHalfAnimation = ValueAnimator.ofInt(0, listHalfHeight);
        mListNoneToHalfAnimation.setDuration(200);
        mListNoneToHalfAnimation.setInterpolator(new LinearInterpolator());
        mListNoneToHalfAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });

        mListHalfToNoneAnimation = ValueAnimator.ofInt(listHalfHeight, 0);
        mListHalfToNoneAnimation.setDuration(200);
        mListHalfToNoneAnimation.setInterpolator(new LinearInterpolator());
        mListHalfToNoneAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });

        mListNoneToFullAnimation = ValueAnimator.ofInt(0, listFullHeight);
        mListNoneToFullAnimation.setDuration(200);
        mListNoneToFullAnimation.setInterpolator(new LinearInterpolator());
        mListNoneToFullAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });

        mListFullToNoneAnimation = ValueAnimator.ofInt(listFullHeight, 0);
        mListFullToNoneAnimation.setDuration(200);
        mListFullToNoneAnimation.setInterpolator(new LinearInterpolator());
        mListFullToNoneAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });

        mListHalfToFullAnimation = ValueAnimator.ofInt(listHalfHeight, listFullHeight);
        mListHalfToFullAnimation.setDuration(200);
        mListHalfToFullAnimation.setInterpolator(new LinearInterpolator());
        mListHalfToFullAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });

        mListFullToHalfAnimation = ValueAnimator.ofInt(listFullHeight, listHalfHeight);
        mListFullToHalfAnimation.setDuration(200);
        mListFullToHalfAnimation.setInterpolator(new LinearInterpolator());
        mListFullToHalfAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                mListViewLayout.getLayoutParams().height = (int) updatedAnimation.getAnimatedValue();
                mListViewLayout.requestLayout();
            }
        });
    }

    // Callback function for ResultsListFragment
    public void onResultsItemClick(Facility item) {
        mFacility = item;
        mMapViewFragment.update(item, true);
        mStateStack.clear();
        mStateStack.push(STATE_LIST_EXPANDED);
        mItemNameSimple.setText(item.getBuilding());
        mItemDescriptionSimple.setText(item.getDetailsShort());

        if (!hasDetails()) {
            switchStates(STATE_ITEM_SIMPLE);
        } else {
            mItemNameDetailed.setText(item.getBuilding());
            if (!mIsBuilding) {
                mItemDescriptionDetailed.setText(item.getDetails());
            } else {
                SearchTask searchTask = new SearchTask(item.getBuilding(), false, true);
                    searchTask.execute();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mItemDescriptionDetailed.setText(SearchResults.mDetails);
                    }
                }, 200);
            }
            switchStates(STATE_ITEM_DETAILED);
        }

    }

    // Callback function for MapViewFragment
    public void onMapItemClick(Marker marker) {
        if (marker == null) {
            if (mState == STATE_HOME) return;
            if (mState != STATE_LIST_HIDDEN) {
                switchStates(STATE_LIST_HIDDEN);
            }
            return;
        }
        Facility item = (Facility) marker.getTag();
        if (item == null) return;
        mFacility = item;
        mItemNameSimple.setText(item.getBuilding());
        String itemDetailsShort = item.getDetailsShort();
        mItemDescriptionSimple.setText(itemDetailsShort);
        String queryTextString = mQueryText.getText().toString();
        if (!queryTextString.equals("printer") && !queryTextString.equals("laundry")
                && !queryTextString.equals("kitchen")) {
            mItemNameDetailed.setText(item.getBuilding());
            mItemDescriptionDetailed.setText(item.getDetails());
        }
        mSimpleItemContainer.invalidate();
        if (mState != STATE_ITEM_SIMPLE) {
            mStateStack.push(mState);
            switchStates(STATE_ITEM_SIMPLE);
        }
    }

    // Callback Function for MenuListFragment
    public void onMenuItemClick(int position) {
        mMenuButton.performClick();
        String queryString = MENU_ITEMS[position];
        mQueryText.setText(queryString);
        mIsBuilding = false;

        SearchTask searchTask = new SearchTask(queryString, false, false);
        searchTask.execute();
        switchStates(STATE_LIST_EXPANDED);
    }

    // Takes care of transition between different displays, or "States", in the application
    public void switchStates(int endState) {
        if (endState != STATE_SEARCH) {
            mQueryText.clearFocus();
        }

        if (mState == STATE_SEARCH) {
            mSearchButton.setVisibility(View.INVISIBLE);
            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(mQueryText.getWindowToken(), 0);
        } else if (mState == STATE_LIST_HIDDEN) {
            mShowListButton.setVisibility(View.INVISIBLE);
        } else if (mState == STATE_ITEM_DETAILED) {
            mDetailedItemContainer.setVisibility(View.INVISIBLE);
        } else if (mState == STATE_ITEM_SIMPLE) {
            mSimpleItemContainer.setVisibility(View.INVISIBLE);
        }

        if (endState == STATE_HOME) {
            mStateStack.clear();
            mIsBuilding = true;
            mXButton.setVisibility(View.INVISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.INVISIBLE);
            mQueryText.getText().clear();
            mQueryText.clearFocus();
            if (mState == STATE_SEARCH) {
                mListFullToNoneAnimation.start();
            } else if (mState == STATE_LIST_EXPANDED || mState == STATE_ITEM_DETAILED) {
                mListHalfToNoneAnimation.start();
            }
            SearchResults.clear();
            mResultsListFragment.update();
            mMapViewFragment.update(false);
        } else if (endState == STATE_SEARCH) {
            mStateStack.push(mState);
            if (mQueryText.getText().toString().length() != 0) {
                mXButton.setVisibility(View.VISIBLE);
                mSearchButton.setVisibility(View.VISIBLE);
            } else {
                mXButton.setVisibility(View.INVISIBLE);
                mSearchButton.setVisibility(View.INVISIBLE);
            }
            mBackButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
            if (mState == STATE_HOME) {
                mListNoneToFullAnimation.start();
            } else {
                SearchResults.saveResults();
                mPrevQueryString = mQueryText.getText().toString().trim();
                if (!mIsBuilding) {
                    mQueryText.getText().clear();
                    SearchResults.clear();
                    mResultsListFragment.update();
                    mMapViewFragment.update(false);
                }
                if (mState == STATE_LIST_EXPANDED || mState == STATE_ITEM_DETAILED) {
                    mListHalfToFullAnimation.start();
                } else {
                    mListNoneToFullAnimation.start();
                }
            }
        } else if (endState == STATE_LIST_EXPANDED) {
            mStateStack.clear();
            mXButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.INVISIBLE);
            mMapExpandLayout.setVisibility(View.VISIBLE);
            if (mState == STATE_SEARCH) {
                mListFullToHalfAnimation.start();
            } else if (mState == STATE_LIST_HIDDEN) {
                mListNoneToHalfAnimation.start();
            } else if (mState == STATE_ITEM_DETAILED) {
            } else {
                mListNoneToHalfAnimation.start();
            }
        } else if (endState == STATE_LIST_HIDDEN) {
            mStateStack.clear();
            mXButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.INVISIBLE);
            mShowListButton.setVisibility(View.VISIBLE);
            if (mState == STATE_LIST_EXPANDED || mState == STATE_ITEM_DETAILED) {
                mListHalfToNoneAnimation.start();
            }
        } else if (endState == STATE_ITEM_DETAILED) {
            mXButton.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
            mDetailedItemContainer.setVisibility(View.VISIBLE);
            if (mState == STATE_SEARCH) {
                mListFullToHalfAnimation.start();
            }
        } else if (endState == STATE_ITEM_SIMPLE) {
            mMapExpandLayout.setVisibility(View.INVISIBLE);
            mXButton.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
            mSimpleItemContainer.setVisibility(View.VISIBLE);
            if (mState == STATE_LIST_EXPANDED || mState == STATE_ITEM_DETAILED) {
                mListHalfToNoneAnimation.start();
            }
        }

        mState = endState;
    }

    // printers, libraries and kitchens don't have detailed displays. The other items do.
    private boolean hasDetails() {
        String queryString = mQueryText.getText().toString().trim();
        return !queryString.equals("printer") && !queryString.equals("kitchen")
                && !queryString.equals("laundry");
    }
}

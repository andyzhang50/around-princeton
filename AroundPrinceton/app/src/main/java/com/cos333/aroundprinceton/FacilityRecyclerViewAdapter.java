package com.cos333.aroundprinceton;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//import com.cos333.aroundprinceton.SearchResults.Item;

import java.util.List;


// A RecyclerView.Adapter that can display a Facility and makes a call to the
// specified ResultsListFragment.OnResultsListInteractionListener.


public class FacilityRecyclerViewAdapter extends RecyclerView.Adapter<FacilityRecyclerViewAdapter.ViewHolder> {

    private List<Facility> mFacilities;
    private ResultsListFragment.OnResultsListInteractionListener mListener;

    public FacilityRecyclerViewAdapter(List<Facility> results, ResultsListFragment.OnResultsListInteractionListener listener) {
        mFacilities = results;
        mListener = listener;
    }

    public void update() {
        mFacilities = SearchResults.mResults;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mFacilities.get(position);
        holder.mIdView.setText(mFacilities.get(position).getBuilding());
        String details = mFacilities.get(position).getDetailsShort();
        holder.mContentView.setText(details);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onResultsItemClick(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFacilities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public Facility mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}

package com.whoeverlovely.mychatapp;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by liyan on 2/21/18.
 */

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendItemViewHolder> {

    private static final String TAG = "FriendListAdapter";

    @Override
    public FriendItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(FriendItemViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class FriendItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public FriendItemViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onClick(View view) {

        }
    }

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemUserId);
    }
}

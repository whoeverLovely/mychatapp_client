package com.whoeverlovely.mychatapp.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

/**
 * Created by liyan on 2/21/18.
 */

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactItemViewHolder> {

    private static final String TAG = "ContactListAdapter";
    private ContactItemClickHandler itemClickHandler;
    private Cursor cursor;

    ContactListAdapter(ContactItemClickHandler itemClickHandler) {
        this.itemClickHandler = itemClickHandler;
    }

    public void swapCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.contact_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactItemViewHolder holder, int position) {

        if(!cursor.moveToPosition(position))
            return;

        String name = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_NAME));
        Log.d(TAG, "The contact name is " + name);
        long userId = cursor.getLong(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_USER_ID));
        if (name != null)
            holder.ContactItemTextView.setText(name);
        else
            holder.ContactItemTextView.setText(String.valueOf(userId));
    }

    @Override
    public int getItemCount() {
        if(cursor == null)
            return 0;

        return cursor.getCount();
    }

    class ContactItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        TextView ContactItemTextView;

        ContactItemViewHolder(View itemView) {
            super(itemView);
            ContactItemTextView = itemView.findViewById(R.id.contact_item);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            cursor.moveToPosition(adapterPosition);
            int userId = cursor.getInt(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_USER_ID));
            itemClickHandler.onClick(userId);
        }
    }

    public interface ContactItemClickHandler {
        void onClick(long userId);
    }
}

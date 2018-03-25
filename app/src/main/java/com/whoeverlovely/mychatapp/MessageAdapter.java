package com.whoeverlovely.mychatapp;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import java.util.zip.Inflater;

/**
 * Created by yan on 3/19/18.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ContactViewHolder> {

    private final Context mContext;
    private Cursor cursor;
    private final static String TAG = "MessageAdapter";

    public MessageAdapter(Context context) {
        mContext = context;
    }

    void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @Override
    public MessageAdapter.ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.message_list_item, parent, false);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {

        if (!cursor.moveToPosition(position))
            return;

        //Retrieve sender userId, msgContent and timestamp from cursor passed in
        int senderId = cursor.getInt(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID));
        String msgContent = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_MESSAGE_CONTENT));
        String timestamp = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_TIMESTAMP));

        //Query userName through content provider with CONTACT_WITH_USERID URI
        Uri uri = ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, senderId);
        Cursor contactCursor = mContext.getContentResolver().query(uri,
                new String[] {ChatAppDBContract.ContactEntry.COLUMN_NAME},
                null,
                null,
                null);

        //If the query result is not null, the messages were sent from other user, set userName and display color accordingly
        String userName;

        if(contactCursor.moveToFirst()) {
            userName = contactCursor.getString(0);

            holder.msgSender.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark, null));
            holder.msgContent.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark, null));
            holder.msgTimestamp.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark, null));
        } else
            userName = "Me";

        holder.msgSender.setText(userName + ": ");
        holder.msgContent.setText(msgContent);
        holder.msgTimestamp.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        if (cursor == null)
            return 0;

        return cursor.getCount();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {

        TextView msgSender;
        TextView msgContent;
        TextView msgTimestamp;

        public ContactViewHolder(View itemView) {
            super(itemView);
            msgSender = itemView.findViewById(R.id.msg_sender);
            msgContent = itemView.findViewById(R.id.msg_content);
            msgTimestamp = itemView.findViewById(R.id.msg_timestamp);
        }
    }
}

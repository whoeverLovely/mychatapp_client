package com.whoeverlovely.mychatapp.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

/**
 * Created by yan on 3/19/18.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ContactViewHolder> {

    private final Context mContext;
    private Cursor cursor;
    private String userName;
    private final static String TAG = "MessageAdapter";

    MessageAdapter(Context context) {
        mContext = context;
    }

    void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
    }

    void updateName(String name) {
        userName = name;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageAdapter.ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.message_list_item, parent, false);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {

        if (!cursor.moveToPosition(position))
            return;

        //Retrieve sender userId, msgContent and timestamp from cursor passed in
        int senderId = cursor.getInt(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID));
        String msgContent = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_MESSAGE_CONTENT));
        String timestamp = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.MessageEntry.COLUMN_TIMESTAMP));

       String myUserId = PreferenceManager.getDefaultSharedPreferences(mContext)
               .getString(mContext.getString(R.string.pref_key_my_user_id), "");
       //The message is received
       if(senderId != Integer.parseInt(myUserId)) {
           holder.msgSender.setText(userName + ": ");
           String friendMsgColor = PreferenceManager.getDefaultSharedPreferences(mContext)
                   .getString(mContext.getString(R.string.color_friend_msg_key), "");

           if (friendMsgColor.equals(mContext.getString(R.string.color_friend_msg_option_value_default)))
               setMsgColor(holder, mContext.getColor(R.color.colorPrimary));
           else if (friendMsgColor.equals(mContext.getString(R.string.color_friend_msg_option_value_blue)))
               setMsgColor(holder, mContext.getColor(R.color.colorFriendMsgBlue));
           else if (friendMsgColor.equals(mContext.getString(R.string.color_friend_msg_option_value_bluegrey)))
               setMsgColor(holder, mContext.getColor(R.color.colorFriendMsgBlueGrey));
           else if (friendMsgColor.equals(mContext.getString(R.string.color_friend_msg_option_value_red)))
               setMsgColor(holder, mContext.getColor(R.color.colorFriendMsgRed));
           else if (friendMsgColor.equals(mContext.getString(R.string.color_friend_msg_option_value_green)))
               setMsgColor(holder, mContext.getColor(R.color.colorFriendMsgGreen));
           else
               throw new RuntimeException();
       } else {
           holder.msgSender.setText("Me: ");
           setMsgColor(holder, Color.GRAY);
       }

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

    private void setMsgColor(ContactViewHolder holder, int color) {
        holder.msgSender.setTextColor(color);
        holder.msgContent.setTextColor(color);
        holder.msgTimestamp.setTextColor(color);
    }
}

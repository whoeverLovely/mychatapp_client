package com.whoeverlovely.mychatapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by liyan on 2/21/18.
 */

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactItemViewHolder> {

    private static final String TAG = "ContactListAdapter";
    private List<Contact> contactList;
    private ContactItemClickHandler itemClickHandler;

    public ContactListAdapter(ContactItemClickHandler itemClickHandler) {
        this.itemClickHandler = itemClickHandler;
    }

    @Override
    public ContactItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.contact_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactItemViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        String name = contact.getName();
        String userId = contact.getUserId();
        if (name != null)
            holder.ContactItemTextView.setText(name);
        else
            holder.ContactItemTextView.setText(userId);

    }

    @Override
    public int getItemCount() {
        if (null == contactList) return 0;
        return contactList.size();
    }

    public void setContactData(List<Contact> contactList) {
        this.contactList = contactList;
        notifyDataSetChanged();
    }

    class ContactItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        public TextView ContactItemTextView;

        public ContactItemViewHolder(View itemView) {
            super(itemView);
            ContactItemTextView = itemView.findViewById(R.id.contact_item);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            Contact contact = contactList.get(adapterPosition);
            itemClickHandler.onClick(contact);
        }
    }

    public interface ContactItemClickHandler {
        void onClick(Contact contact);
    }
}

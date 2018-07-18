package com.whoeverlovely.mychatapp.ui;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

public class EditContactNameDialogFragment extends DialogFragment {

    private static final String TAG = EditContactNameDialogFragment.class.getSimpleName();

    public EditContactNameDialogFragment() {
    }

    public static EditContactNameDialogFragment newInstance(long userId) {
        EditContactNameDialogFragment frag = new EditContactNameDialogFragment();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_username_dialog, container);
        getDialog().setTitle("Edit Name");
        getDialog().setCanceledOnTouchOutside(false);
        return rootView;
    }

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        // Get field from view
//        nameEditText = view.findViewById(R.id.username_editText);
//
//    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final long userId = getArguments().getLong("userId");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage(getString(R.string.please_input_your_friend_s_name));

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        alertDialogBuilder.setView(inflater.inflate(R.layout.fragment_username_dialog, null));
        alertDialogBuilder.setCancelable(false);

        // Add action buttons
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TextView nameEditText = getDialog().findViewById(R.id.username_editText);
                String name = nameEditText.getText().toString();
                Log.d(TAG, "the name input is " + name);
                if (!Strings.isNullOrEmpty(name)) {
                    // Update user name
                    ContentValues cv = new ContentValues();
                    cv.put(ChatAppDBContract.ContactEntry.COLUMN_NAME, name);
                    getContext().getContentResolver()
                            .update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, userId),
                                    cv, null, null);
                } else {

                }


            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        Dialog dialog = alertDialogBuilder.create();

        return dialog;
    }

}
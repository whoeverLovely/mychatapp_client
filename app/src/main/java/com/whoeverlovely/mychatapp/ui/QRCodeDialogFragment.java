package com.whoeverlovely.mychatapp.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.whoeverlovely.mychatapp.R;

public class QRCodeDialogFragment extends DialogFragment {
    private final static String QRCODE_SRC = "src";
    private final static String USER_ID = "userId";

    public QRCodeDialogFragment() {
    }

    public static QRCodeDialogFragment newInstance(String src, String userId) {
        QRCodeDialogFragment frag = new QRCodeDialogFragment();
        Bundle args = new Bundle();
        args.putString(QRCODE_SRC, src);
        args.putString(USER_ID, userId);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qrcode_popup, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        ImageView qrcodeImage = view.findViewById(R.id.qrcode_imageView);
        TextView userIdView = view.findViewById(R.id.userId_textView);
        // Get qrcode source
        String src = getArguments().getString(QRCODE_SRC);
        String userId = getArguments().getString(USER_ID);
        userIdView.setText(userId);
        // Set qrcode image
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(src, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrcodeImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    //TODO remove boarder
}

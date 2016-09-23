package com.example.helio.arduino.core;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.helio.arduino.R;

import java.io.File;

public class ShareUtil {
    public static void saveToDrive(Context context, File file) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setPackage("com.google.android.apps.docs");
        try {
            context.startActivity(Intent.createChooser(intent, ""));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.google_drive_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendEmail(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_applications, Toast.LENGTH_SHORT).show();
        }
    }
}

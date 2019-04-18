package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.webkit.URLUtil;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.List;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;

/**
 * NativeModule that allows JS to open emails sending apps chooser.
 */
public class RNMailModule extends ReactContextBaseJavaModule {

  ReactApplicationContext reactContext;

  public RNMailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMail";
  }

  /**
    * Converts a ReadableArray to a String array
    *
    * @param r the ReadableArray instance to convert
    *
    * @return array of strings
  */
  private String[] readableArrayToStringArray(ReadableArray r) {
    int length = r.size();
    String[] strArray = new String[length];

    for (int keyIndex = 0; keyIndex < length; keyIndex++) {
      strArray[keyIndex] = r.getString(keyIndex);
    }

    return strArray;
  }

  @ReactMethod
  public void mail(ReadableMap options, Callback callback) throws IOException {
    Intent i;

    if (options.hasKey("attachment") && !options.isNull("attachment")) {
      i = new Intent(Intent.ACTION_SEND);
      i.setType("vnd.android.cursor.dir/email");
    } else {
      i = new Intent(Intent.ACTION_SENDTO);
      i.setData(Uri.parse("mailto:"));
    }

    if (options.hasKey("subject") && !options.isNull("subject")) {
      i.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (options.hasKey("body") && !options.isNull("body")) {
      String body = options.getString("body");
      if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body).toString());
      } else {
        i.putExtra(Intent.EXTRA_TEXT, body);
      }
    }

    if (options.hasKey("recipients") && !options.isNull("recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      i.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      i.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      i.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

    if (options.hasKey("attachment") && !options.isNull("attachment")) {
      ReadableMap attachment = options.getMap("attachment");
      if (attachment.hasKey("path") && !attachment.isNull("path")) {

        String path = attachment.getString("path");

        Uri uri;
        boolean isWebUrl = path.startsWith("http");
        if (isWebUrl) {
            byte[] response = this.downloadFile(path);

            long unixTime = System.currentTimeMillis() / 1000L;
            String outputPath = reactContext.getCacheDir().getAbsolutePath() + "/mail" + String.valueOf(unixTime) + ".jpg";

            this.writeDownloadedFile(response, outputPath);

            uri = Uri.parse(outputPath);
        } else {
            uri = Uri.parse(path);
        }
        
        Uri main = Uri.parse("content://com.mimi_stelladot.providers/img/"+uri.getLastPathSegment());
        i.putExtra(Intent.EXTRA_STREAM, main);
      }
    }

    PackageManager manager = reactContext.getPackageManager();
    List<ResolveInfo> list = manager.queryIntentActivities(i, 0);

    if (list == null || list.size() == 0) {
      callback.invoke("not_available");
      return;
    }

    if (list.size() == 1) {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        reactContext.startActivity(i);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    } else {
      Intent chooser = Intent.createChooser(i, "Send Mail");
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      try {
        reactContext.startActivity(chooser);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    }
  }

  private byte[] downloadFile(String webURL) throws IOException {
    URL url = new URL(webURL);
    InputStream in = new BufferedInputStream(url.openStream());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int n = 0;
    while (-1!=(n=in.read(buf)))
    {
        out.write(buf, 0, n);
    }
    out.close();
    in.close();
    byte[] response = out.toByteArray();
    return response;
  }

  private void writeDownloadedFile(byte[] data, String outputPath) throws IOException {
    FileOutputStream fos = new FileOutputStream(outputPath);
    fos.write(data);
    fos.flush();
    fos.close();
  }
}

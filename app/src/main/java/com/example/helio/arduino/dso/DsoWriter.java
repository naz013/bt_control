package com.example.helio.arduino.dso;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DsoWriter {

    private static final String DATA_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/" + "DSO_data.txt";
    private static final String DATA_KEY = "data_key";
    private static final String TAG = "DsoWriter";

    public static void writeToFile(List<Float> yValues) throws IOException {
        File file = new File(DATA_FILE_PATH);
        if (file.exists()) file.delete();
        FileWriter fw = new FileWriter(file);
        fw.write(getDsoDataAsJson(yValues));
        fw.close();
    }

    public static List<Float> readDsoAsArray() {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(DATA_FILE_PATH);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (stream != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8")
                );
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return getArrayFromJson(writer.toString());
        }
        return new ArrayList<>();
    }

    public static boolean hasDsoData() {
        File file = new File(DATA_FILE_PATH);
        boolean res = file.exists();
        Log.d(TAG, "hasDsoData: " + res);
        return res;
    }

    private static List<Float> getArrayFromJson(String json) {
        List<Float> list = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has(DATA_KEY)) {
                Type collectionType = new TypeToken<List<Float>>() {}.getType();
                try {
                    list = new Gson().fromJson(jsonObject.get(DATA_KEY).toString(), collectionType);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String getDsoDataAsJson(List<Float> data) {
        JSONObject jObjectData = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            for (float f : data) array.put(f);
            jObjectData.put(DATA_KEY, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jObjectData.toString();
    }
}

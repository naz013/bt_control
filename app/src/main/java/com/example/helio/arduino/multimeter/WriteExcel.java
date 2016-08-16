package com.example.helio.arduino.multimeter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.TinyDB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jxl.CellView;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.UnderlineStyle;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Copyright 2016 Nazar Suhovich
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class WriteExcel {

    private static final String TAG = "WriteExcel";
    private WritableCellFormat timesBoldUnderline;
    private WritableCellFormat times;
    private String type;
    private int pointer;
    private Context mContext;
    private WritableSheet mExcelSheet;
    private WritableWorkbook mWorkbook;

    public WriteExcel(Context context) {
        this.mContext = context;
    }

    public void setOutput(String type) {
        this.type = type;
        initCellStyles();
        readOrCreateFile();
    }

    private void initCellStyles() {
        WritableFont times10pt = new WritableFont(WritableFont.TIMES, 10);
        times = new WritableCellFormat(times10pt);
        try {
            times.setWrap(true);
        } catch (WriteException e) {
            e.printStackTrace();
        }
        WritableFont times10ptBoldUnderline = new WritableFont(WritableFont.TIMES, 10, WritableFont.BOLD, false,
                UnderlineStyle.SINGLE);
        timesBoldUnderline = new WritableCellFormat(times10ptBoldUnderline);
        try {
            timesBoldUnderline.setWrap(true);
        } catch (WriteException e) {
            e.printStackTrace();
        }
        CellView cv = new CellView();
        cv.setFormat(times);
        cv.setFormat(timesBoldUnderline);
        cv.setAutosize(true);
    }

    private void readOrCreateFile() {
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER + "/" + Constants.SHEETS_FOLDER);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                return;
            }
        }
        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(new Locale("en", "EN"));
        File file;
        if (type.matches(Constants.V)) {
            file = new File(folder, "Voltage.xls");
            pointer = TinyDB.getInstance(mContext).getInt(Constants.VOLTAGE_ROW, 0);
        } else if (type.matches(Constants.I)) {
            file = new File(folder, "Current.xls");
            pointer = TinyDB.getInstance(mContext).getInt(Constants.CURRENT_ROW, 0);
        } else {
            file = new File(folder, "Resistance.xls");
            pointer = TinyDB.getInstance(mContext).getInt(Constants.RESISTANCE_ROW, 0);
        }
        if (file.exists()) {
            try {
                File dest = new File(folder, "readable.xls");
                if (dest.exists()) dest.delete();
                copyFile(file, dest);
                file.delete();
                Workbook workbook = Workbook.getWorkbook(dest);
                mWorkbook = Workbook.createWorkbook(file, workbook);
                mExcelSheet = mWorkbook.getSheet(0);
                workbook.close();
                if (dest.exists()) dest.delete();
            } catch (IOException | BiffException e) {
                e.printStackTrace();
            }
        } else {
            initNewWorkbook(file, wbSettings);
        }
        if (mWorkbook == null || mExcelSheet == null) {
            initNewWorkbook(file, wbSettings);
        }
    }

    private void initNewWorkbook(File file, WorkbookSettings wbSettings) {
        pointer = -1;
        updatePointer();
        try {
            mWorkbook = Workbook.createWorkbook(file, wbSettings);
            mWorkbook.createSheet("Data", 0);
            mExcelSheet = mWorkbook.getSheet(0);
            try {
                createLabel();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public void write() throws IOException, WriteException {
        mWorkbook.write();
    }

    public void close() throws IOException, WriteException {
        mWorkbook.close();
    }

    private void createLabel() throws WriteException, IOException {
        if (type.matches(Constants.V)) {
            addCaption(0, 0, mContext.getString(R.string.voltage));
        } else if (type.matches(Constants.R)) {
            addCaption(0, 0, mContext.getString(R.string.resistance));
        } else {
            addCaption(0, 0, mContext.getString(R.string.current));
        }
        addCaption(1, 0, mContext.getString(R.string.value));
        addCaption(2, 0, mContext.getString(R.string.time));
        updatePointer();
    }

    private void updatePointer() {
        pointer++;
        if (type.matches(Constants.I)) {
            TinyDB.getInstance(mContext).putInt(Constants.CURRENT_ROW, pointer);
        } else if (type.matches(Constants.V)) {
            TinyDB.getInstance(mContext).putInt(Constants.VOLTAGE_ROW, pointer);
        } else {
            TinyDB.getInstance(mContext).putInt(Constants.RESISTANCE_ROW, pointer);
        }
    }

    private void addCaption(int column, int row, String s) throws WriteException {
        Label label = new Label(column, row, s, timesBoldUnderline);
        mExcelSheet.addCell(label);
    }

    private void addNumber(int column, int row, Integer integer) throws WriteException {
        Number number = new Number(column, row, integer, times);
        mExcelSheet.addCell(number);
    }

    public void addValue(String s) throws WriteException {
        Log.d(TAG, "addValue: " + pointer);
        if (mExcelSheet == null) return;
        addNumber(0, pointer, pointer);
        mExcelSheet.addCell(new Label(1, pointer, s, times));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ", Locale.getDefault());
        String timeStamp = sdf.format(new Date());
        mExcelSheet.addCell(new Label(2, pointer, timeStamp, times));
        updatePointer();
    }
}

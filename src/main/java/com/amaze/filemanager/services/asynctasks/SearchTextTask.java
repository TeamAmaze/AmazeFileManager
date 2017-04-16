package com.amaze.filemanager.services.asynctasks;

import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;
import android.widget.ImageButton;

import com.amaze.filemanager.activities.TextReader;
import com.amaze.filemanager.utils.MapEntry;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Vishal on 2/1/16.
 */
public class SearchTextTask extends AsyncTask<Editable, Void, ArrayList<MapEntry>> {

    private EditText searchEditText, mInput;
    private ArrayList<MapEntry> nodes;
    private int searchTextLength;
    private ImageButton upButton, downButton;
    private TextReader textReader;
    private Editable editText;
    private String searchSubString;
    private StringReader stringReader;
    private LineNumberReader lineNumberReader;

    public SearchTextTask(TextReader textReader) {
        this.textReader = textReader;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        this.searchEditText = textReader.searchEditText;
        this.nodes = textReader.nodes;
        this.upButton = textReader.upButton;
        this.downButton = textReader.downButton;
        this.mInput = textReader.mInput;
        searchTextLength = searchEditText.length();
        editText = mInput.getText();
        stringReader = new StringReader(editText.toString());
        lineNumberReader = new LineNumberReader(stringReader);
    }

    @Override
    protected ArrayList<MapEntry> doInBackground(Editable... params) {
        for (int i = 0; i < (editText.length() - params[0].length()); i++) {
            if (searchTextLength == 0 || isCancelled())
                break;

            searchSubString = editText.subSequence(i, i + params[0].length()).toString();

            // comparing and adding searched phrase to a list
            if (searchSubString.equalsIgnoreCase(params[0].toString())) {

                nodes.add(new MapEntry(new MapEntry.KeyMapEntry(i, i + params[0].length()),
                        lineNumberReader.getLineNumber()));
            }

            // comparing and incrementing line number
            // ended up using LineNumberReader api instead
            try {
                lineNumberReader.skip(params[0].length());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return nodes;
    }

    @Override
    protected void onPostExecute(final ArrayList<MapEntry> mapEntries) {
        super.onPostExecute(mapEntries);

        for (Map.Entry mapEntry : mapEntries) {

            Map.Entry keyMapEntry = (Map.Entry) mapEntry.getKey();
            mInput.getText().setSpan(textReader.getAppTheme().equals(AppTheme.LIGHT) ? new BackgroundColorSpan(Color.YELLOW) :
                            new BackgroundColorSpan(Color.LTGRAY),
                    (Integer) keyMapEntry.getKey(), (Integer) keyMapEntry.getValue(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (mapEntries.size()!=0) {
            upButton.setEnabled(true);
            downButton.setEnabled(true);

            // downButton
            textReader.onClick(downButton);
        } else {
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        }
    }

}

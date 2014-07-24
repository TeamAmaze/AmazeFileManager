package com.amaze.filemanager.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amaze.filemanager.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TextReader extends Activity {
    EditText ma;
    String path;
    ProgressBar p;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        ma = (EditText) findViewById(R.id.fname);
        p = (ProgressBar) findViewById(R.id.pbar);
        ma.setVisibility(View.GONE);
        path = this.getIntent().getStringExtra("path");
        if (path != null) {
            Toast.makeText(this, "" + path, Toast.LENGTH_SHORT).show();
            new LoadText().execute(path);
        } else {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        }
        getActionBar().setTitle(new File(path).getName());
        getActionBar().setSubtitle(path);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text, menu);
        return true;
    }

    boolean save;

    @Override
    public boolean onPrepareOptionsMenu(Menu m) {

        return super.onPrepareOptionsMenu(m);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.save:
                writeTextFile(path, ma.getText().toString());
                return true;
        }
        return super.onOptionsItemSelected(menu);
    }

    public String readTextFile(String fileName) {
        String returnValue = "";
        FileReader file = null;
        String line = "";
        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);

            while ((line = reader.readLine()) != null) {
                returnValue += line + "\n";

            }
            reader.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        } finally {
            if (file != null) {
                try {
                    file.close();

                } catch (IOException e) {
                    Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
                    finish();
                    e.printStackTrace();
                }
            }
        }

        return returnValue;
    }

    public void writeTextFile(String fileName, String s) {
        File f = new File(fileName);

        FileWriter output = null;
        try {
            output = new FileWriter(fileName);
            BufferedWriter writer = new BufferedWriter(output);
            writer.write(s);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    class LoadText extends AsyncTask<String, String, String> {
        public void onpreExecute() {
            ma.setVisibility(View.GONE);
            p.setVisibility(View.VISIBLE);
        }

        boolean editable = true;

        public String doInBackground(String... p) {
            String returnValue = "";
            if (new File(p[0]).canWrite()) editable = true;
            else editable = false;
            FileReader file = null;
            String line = "";
            try {
                file = new FileReader(p[0]);
                BufferedReader reader = new BufferedReader(file);

                while ((line = reader.readLine()) != null) {
                    returnValue += line + "\n";
                }
                reader.close();
            } catch (FileNotFoundException e) {
                //	Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
            } catch (IOException e) {
                //	Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
            } finally {
                if (file != null) {
                    try {
                        file.close();

                    } catch (IOException e) {
                        //Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
                        e.printStackTrace();
                    }
                }
            }
            return returnValue;
        }

        @Override
        public void onPostExecute(String s) {
            p.setVisibility(View.GONE);
            ma.setFocusable(editable);
            save = editable;
            TextReader.this.invalidateOptionsMenu();
            ma.setVisibility(View.VISIBLE);
            ma.setText(s);
        }
    }

}

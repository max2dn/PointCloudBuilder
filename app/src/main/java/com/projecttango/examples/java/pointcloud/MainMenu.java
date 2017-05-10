package com.projecttango.examples.java.pointcloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import java.net.HttpURLConnection;

/**
 * Created by maxenchung on 5/3/17.
 *
 * Illegal characters from http://stackoverflow.com/questions/893977/java-how-to-find-out-whether-a-file-name-is-valid
 */

public class MainMenu extends AppCompatActivity {
    private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
    ListView mainListview;
    String frontFileName = "";
    String backFileName = "";
    String exportFileName= "";
    private static final int FRONT = 0;
    private static final int BACK = 1;
    Toolbar mToolbar;

    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;

    static final String[] MENUITEMS = {"Front", "Back"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_layout);
        mainListview = (ListView) findViewById(R.id.main_menu_listview);
        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, MENUITEMS);
        mainListview.setAdapter(adapter);
        mainListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String item = (String) mainListview.getItemAtPosition(position);
                AlertDialog.Builder b = new AlertDialog.Builder(MainMenu.this);
                b.setTitle("Input Filename");
                final EditText input = new EditText(MainMenu.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                b.setView(input);

                // Set up the buttons
                b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = input.getText().toString();
                        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/pointclouds");
                        File file = new File(path,filename+".pcd");
                        if(!validFileName(filename)||!file.exists()) {
                            dialog.cancel();
                            Toast.makeText(MainMenu.this,"File Does Not Exist",Toast.LENGTH_LONG).show();
                            return;
                        }
                        if(item.equals("Front")) {
                            frontFileName = input.getText().toString();
                            ((TextView) mainListview.getChildAt(FRONT)).setTextColor(Color.GREEN);
                        } else if (item.equals("Back")){
                            backFileName = input.getText().toString();
                            ((TextView) mainListview.getChildAt(FRONT)).setTextColor(Color.GREEN);
                        } else {
                            Log.e("MainMenuListViewLClick", "Invalid item selected");
                            return;
                        }
                        dialog.dismiss();
                    }
                });
                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                b.show();
                return true;
            }
        });

        mainListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String item = (String) mainListview.getItemAtPosition(position);
                AlertDialog.Builder b = new AlertDialog.Builder(MainMenu.this);
                b.setTitle("Input Desired Filename");
                final EditText input = new EditText(MainMenu.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                b.setView(input);

                // Set up the buttons
                b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int resultcode;
                        if(!validFileName(input.getText().toString()))
                            return;
                        if(item=="Front") {
                            resultcode = FRONT;
                            frontFileName = input.getText().toString();
                        } else if (item=="Back"){
                            resultcode = BACK;
                            backFileName = input.getText().toString();
                        } else {
                            Log.e("MainMenuListViewClick", "Invalid item selected");
                            return;
                        }
                        Intent launchCapture = new Intent(getApplicationContext(), PointCloudActivity.class);
                        launchCapture.putExtra("filename",input.getText().toString());
                        startActivityForResult(launchCapture,resultcode);
                    }
                });
                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                b.show();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==Activity.RESULT_OK){
            if(requestCode == FRONT){
                ((TextView) mainListview.getChildAt(FRONT)).setTextColor(Color.GREEN);
            } else if(requestCode == BACK){
                ((TextView) mainListview.getChildAt(BACK)).setTextColor(Color.GREEN);
            }
        } else {
            Toast.makeText(this,"Failed to Export",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        for(int i = 0; i<mainListview.getChildCount(); i++){
            if(((TextView) mainListview.getChildAt(i)).getCurrentTextColor() != Color.GREEN){
                Toast.makeText(this,"Input All Clouds",Toast.LENGTH_LONG).show();
                return false;
            }
        }
        switch(item.getItemId()) {
            case R.id.export_menu_item:
                if(!isOnline()){
                    Toast.makeText(MainMenu.this,"No Internet Connection",Toast.LENGTH_LONG).show();
                    return false;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(MainMenu.this);
                b.setTitle("Input Desired Filename");
                final EditText i = new EditText(MainMenu.this);
                i.setInputType(InputType.TYPE_CLASS_TEXT);
                b.setView(i);
                b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!validFileName(i.getText().toString()))
                            return;
                        exportFileName = i.getText().toString();
                        new SendtoServerTask().execute();
                    }
                });
                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                b.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    boolean validFileName (String name){
        for(char i : ILLEGAL_CHARACTERS){
            if(name.contains(String.valueOf(i))){
                return false;
            }
        }
        return true;
    }

    private class SendtoServerTask extends AsyncTask<URL,Integer,Boolean> {
        ProgressDialog d;
        @Override
        protected void onPreExecute() {
            d = new ProgressDialog(MainMenu.this);
            d.setMessage("Sending Data");
            d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            d.setProgressNumberFormat(null);
            d.setIndeterminate(false);
            d.setProgress(0);
            d.setMax(100);
            d.show();
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            HttpURLConnection urlConnection = null;
            JSONObject jsonObject = new JSONObject();
            JSONObject data = getJSONObject();
            try{
                jsonObject.put("main_body",data);
            } catch(JSONException e){
                Log.e("makeRequest","JSON Exception");
                return false;
            }
            try{
                URL url = new URL("http","54.183.187.18",8080,"json");
                urlConnection = (HttpURLConnection) url.openConnection();
                Log.v("makeRequest", "Made connection to " + url.toString());
                publishProgress(10);
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                publishProgress(15);
                OutputStreamWriter os = new OutputStreamWriter(urlConnection.getOutputStream());
                os.write(jsonObject.toString());
                os.close();
                publishProgress(25);
                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection.getResponseCode();
                publishProgress(75);
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    String line = null;
                    int count = 0;
                    while ((line = br.readLine()) != null) {
                        count++;
                        if(count%1000==0){
                            publishProgress(75+25*count/60000);
                        }
                        sb.append(line);
                        sb.append("\n");
                    }
                    br.close();
                    File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/objects");
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                    final File file = new File(f,exportFileName+".obj");
                    OutputStream outputfilestream = new FileOutputStream(file);
                    outputfilestream.write(sb.toString().getBytes());
                    outputfilestream.close();
                } else {
                    System.out.println(urlConnection.getResponseMessage());
                }
            } catch(MalformedURLException e){
                Log.e("makeRequest","MalformedURLException");
            } catch(IOException e){
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Toast.makeText(MainMenu.this,"Saved",Toast.LENGTH_LONG).show();
            d.dismiss();
            AlertDialog.Builder a = new AlertDialog.Builder(MainMenu.this);
            a.setMessage("Open File?");
            a.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openFile();
                }
            });
            a.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            a.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            d.setProgress(values[0]);
            switch(values[0]){
                case 0:
                    d.setMessage("Making Connection");
                    break;
                case 15:
                    d.setMessage("Posting Data");
                    break;
                case 25:
                    d.setMessage("Waiting for Server to Parse Data");
                    break;
                case 75:
                    d.setMessage("Writing File");
                    break;
            }
            if(values[0]==75){
                d.setMessage("Receiving File");
            }
        }
    }


    private JSONObject getJSONObject(){
        String files[]={frontFileName+".pcd", backFileName+".pcd"};
        JSONObject jsonObject = new JSONObject();
        for(String f: files) {
            StringBuilder text = new StringBuilder();
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/pointclouds/" + f);
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                bufferedReader.close();
            } catch (IOException e) {
                Log.e("getJSONObject", "IO Exception");
            }
            try {
                if(f.equals(frontFileName + ".pcd"))
                    jsonObject.put("front", text.toString());
                else if (f.equals(backFileName + ".pcd"))
                    jsonObject.put("back", text.toString());
            } catch(JSONException e){
                Log.e("getJSONObject","JSON Exception");
            }
        }
        return jsonObject;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public void openFile() {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/objects");
        File file = new File(f, exportFileName+".obj");

        // Get URI and MIME type of file
        Uri uri = FileProvider.getUriForFile(this,"com.mydomain.fileprovider", file);

        // Open file with user selected app
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}

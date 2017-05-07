package com.projecttango.examples.java.pointcloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        switch(item.getItemId()) {
            case R.id.export_menu_item:
                new SendtoServerTask().execute();
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
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                Log.v("makeRequest", "Set Request Method POST");
                OutputStreamWriter os = new OutputStreamWriter(urlConnection.getOutputStream());
                Log.v("makeRequest", "Got Output Stream");
                os.write(jsonObject.toString());
                Log.v("makeRequest", "Wrote Output Stream");
                os.close();
                Log.v("makeRequest", "Finished Sending Data");
                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection.getResponseCode();
                Log.v("makeRequest", "Received Response Code " + HttpResult);
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    br.close();
                    Log.d("vtk","" + sb.toString());
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

}

package me.aemo.metallama;

import android.os.AsyncTask;
import android.util.Log;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import com.google.appinventor.components.runtime.util.YailDictionary;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MetaLlama extends AndroidNonvisibleComponent implements Component {

  private ComponentContainer container;
  private String token;
  public MetaLlama(ComponentContainer container) {
    super(container.$form());
    this.container = container;
  }

  @SimpleProperty(description = "Get API token.")
  public String Token(){
    return token;
  }
  @SimpleProperty(description = "Set API token.")
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
  public void Token(String token){
    this.token = token;
  }

  @SimpleFunction(description = "Parses a JSON array and retrieves the value corresponding to a specific key from the JSON objects within the array.")
  public Object FromJsonArray(String json, String key) throws JSONException {
    JSONArray jsonArray = new JSONArray(json);
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      if (jsonObject.has(key)){
        return jsonObject.get(key);
      } else {
        return null;
      }
    }
    return null;
  }

  @SimpleFunction(description = "Parses a JSON object and retrieves the value corresponding to a specific key.")
  public Object FromJsonObject(String json, String key) throws JSONException {
    JSONObject jsonObject = new JSONObject(json);
    if (jsonObject.has(key)){
      return jsonObject.get(key);
    } else {
      return null;
    }
  }


  @SimpleFunction(description = "Initiates the creation of a chat asynchronously.")
  public void CreateChat(YailDictionary messages){
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        if (token.isEmpty()){
          container.$form().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              OnCreateChatError("Please check your token.");
            }
          });
        }
        new ChatAsyncTask(token, messages, new AsyncTaskListener() {
          @Override
          public void onSuccess(String result) {
            OnCreateChat(result);
          }

          @Override
          public void onError(Exception e) {
            OnCreateChatError(e.getMessage());
          }
        }).execute();
      }
    });
  }

  @SimpleEvent(description = "Event raised when a chat is successfully created.")
  public void OnCreateChat(String result) {
    EventDispatcher.dispatchEvent(this,"OnCreateChat", result);
  }

  @SimpleEvent(description = "Event raised when an error occurs during chat creation.")
  public void OnCreateChatError(String message) {
    EventDispatcher.dispatchEvent(this,"OnCreateChatError", message);
  }

  public class ChatAsyncTask extends AsyncTask<Void, Void, String> {

    private final String token;
    //private final String content;
    private final YailDictionary dictionary;
    private final AsyncTaskListener listener;

    public ChatAsyncTask(String token, YailDictionary dictionary, AsyncTaskListener listener){
      this.token = token;
      this.dictionary = dictionary;
      this.listener = listener;
    }
    /*
    public ChatAsyncTask(String token, String content, AsyncTaskListener listener){
      this.token = token;
      this.content = content;
      this.listener = listener;
    }
     */
    @Override
    protected String doInBackground(Void... voids) {
      try {
        // API endpoint URL
        URL url = new URL("https://api.llama-api.com/chat/completions");

        // Create connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token); // Set the token
        conn.setDoOutput(true);

        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObjectMessage = new JSONObject();
        JSONArray jsonArrayMessage = new JSONArray();

        for (Object key : dictionary.keySet()) {
          Object value = dictionary.get(key);
          jsonObjectMessage.put((String) key, value);
        }
        //jsonObjectMessage.put("role", "user");
        //jsonObjectMessage.put("content", content);


        jsonArrayMessage.put(0, jsonObjectMessage);
        jsonObject.put("messages", jsonArrayMessage);

        // Write JSON data to request body
        OutputStream os = conn.getOutputStream();
        os.write(jsonObject.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        Log.d("MetaLlama", String.valueOf(responseCode));

        if (responseCode == HttpURLConnection.HTTP_OK) {
          BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String inputLine;
          StringBuilder response = new StringBuilder();
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          in.close();

          return response.toString();
        } else {
          BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
          String inputLine;
          StringBuilder response = new StringBuilder();
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          in.close();
          container.$form().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              listener.onError(new Exception(response.toString()));
            }
          });
          return null;
        }
      } catch (IOException | JSONException e) {
        container.$form().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            listener.onError(e);
          }
        });
        return null;
      }
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if (result != null && listener != null) {
        container.$form().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            listener.onSuccess(result);
          }
        });
      } else {
        container.$form().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            listener.onError(new Exception("result is null"));
          }
        });
      }
    }
  }


  public interface AsyncTaskListener{
    void onSuccess(String result);
    void onError(Exception e);
  }



}

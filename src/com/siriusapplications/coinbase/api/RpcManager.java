package com.siriusapplications.coinbase.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;


public class RpcManager {

  private static RpcManager INSTANCE = null;

  public static RpcManager getInstance() {

    if(INSTANCE == null) {
      INSTANCE = new RpcManager();
    }

    return INSTANCE;
  }

  private RpcManager() {

  }
  
  private static final String BASE_URL = "https://coinbase.com/api/v1/";

  public JSONObject callGet(Context context, String method) throws IOException, JSONException {

    return call(context, method, false, null, true);
  }
  
  public JSONObject callPost(Context context, String method, Collection<BasicNameValuePair> params) throws IOException, JSONException {
    
    return call(context, method, true, params, true);
  }

  private JSONObject call(Context context, String method, boolean isPost, Collection<BasicNameValuePair> params, boolean retry) throws IOException, JSONException {

    DefaultHttpClient client = new DefaultHttpClient();

    String url = BASE_URL + method;

    HttpUriRequest request = null;

    if(isPost) {
      request = new HttpPost(url);
      List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
      parametersBody.addAll(params);
      ((HttpPost) request).setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));
    } else {
      
      request = new HttpGet(url);
    }
    
    String accessToken = LoginManager.getInstance().getAccessToken(context);
    request.addHeader("Authorization", String.format("Bearer %s", accessToken));

    HttpResponse response = client.execute(request);
    int code = response.getStatusLine().getStatusCode();

    if(code == 401 && retry) {
      
      // Authentication error.
      // This may be caused by an outdated access token - attempt to fetch a new one
      // before failing.
      LoginManager.getInstance().refreshAccessToken(context);
      return call(context, method, isPost, params, false);
    } else if(code != 200) {

      throw new IOException("HTTP response " + code);
    }

    JSONObject content = new JSONObject(new JSONTokener(EntityUtils.toString(response.getEntity())));
    return content;
  }
}
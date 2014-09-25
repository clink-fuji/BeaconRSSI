package com.example.beaconrssi;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


	public class HttpPostTask extends AsyncTask<Void, Void, Void> {

		  // 設定事項
		  private String request_encoding = "UTF-8";
		  private String response_encoding = "UTF-8";

		  // 初期化事項
		  private Activity parent_activity = null;
		  private String post_url = null;
		  private Handler ui_handler = null;
		  private List<NameValuePair> post_params = null;

		  // 処理中に使うメンバ
		  private ResponseHandler<Void> response_handler = null;
		  private String http_err_msg = null;
		  private String http_ret_msg = null;
		  private ProgressDialog dialog = null;


		  // 生成時
		  public HttpPostTask( Activity parent_activity, String post_url, Handler ui_handler )
		  {
		    // 初期化
		    this.parent_activity = parent_activity;
		    this.post_url = post_url;
		    this.ui_handler = ui_handler;

		    // 送信パラメータは初期化せず，new後にsetさせる
		    post_params = new ArrayList<NameValuePair>();
		  }


		  /* --------------------- POSTパラメータ --------------------- */


		  // 追加
		  public void addPostParam( String post_name, String post_value )
		  {
		    post_params.add(new BasicNameValuePair( post_name, post_value ));
		  }


		  /* --------------------- 処理本体 --------------------- */


		  // タスク開始時
		  protected void onPreExecute() {
		    // ダイアログを表示
		    dialog = new ProgressDialog( parent_activity );
		    dialog.setMessage("通信中・・・");
		    dialog.show();

		    // レスポンスハンドラを生成
		    response_handler = new ResponseHandler<Void>() {

		      // HTTPレスポンスから，受信文字列をエンコードして文字列として返す
		      @Override
		      public Void handleResponse(HttpResponse response) throws IOException
		      {
		        Log.d(
		        "posttest",
		        "レスポンスコード：" + response.getStatusLine().getStatusCode()
		        );

		        // 正常に受信できた場合は200
		        switch (response.getStatusLine().getStatusCode()) {
		        case HttpStatus.SC_OK:
		        Log.d("posttest", "レスポンス取得に成功");

		        // レスポンスデータをエンコード済みの文字列として取得する。
		        // ※IOExceptionの可能性あり
		        HttpPostTask.this.http_ret_msg = EntityUtils.toString(
		          response.getEntity(),
		          HttpPostTask.this.response_encoding
		        );
		        break;

		        case HttpStatus.SC_NOT_FOUND:
		        // 404
		        Log.d("posttest", "存在しない");
		        HttpPostTask.this.http_err_msg = "404 Not Found";
		        break;

		        default:
		        Log.d("posttest", "通信エラー");
		        HttpPostTask.this.http_err_msg = "通信エラーが発生";
		        }

		        return null;
		      }

		    };
		  }


		  // メイン処理
		  protected Void doInBackground(Void... unused) {

		    Log.d("posttest", "postします");

		    // URL
		    URI url = null;
		    try {
		      url = new URI( post_url );
		      Log.d("posttest", "URLはOK");
		    } catch (URISyntaxException e) {
		      e.printStackTrace();
		      http_err_msg = "不正なURL";
		      return null;
		    }

		    // POSTパラメータ付きでPOSTリクエストを構築
		    HttpPost request = new HttpPost( url );
		    try {
		      // 送信パラメータのエンコードを指定
		      request.setEntity(new UrlEncodedFormEntity(post_params, request_encoding));
		    } catch (UnsupportedEncodingException e1) {
		      e1.printStackTrace();
		      http_err_msg = "不正な文字コード";
		      return null;
		    }

		    // POSTリクエストを実行
		    DefaultHttpClient httpClient = new DefaultHttpClient();
		    Log.d("posttest", "POST開始");
		    try {
		      httpClient.execute(request, response_handler);
		    } catch (ClientProtocolException e) {
		      e.printStackTrace();
		      http_err_msg = "プロトコルのエラー";
		    } catch (IOException e) {
		      e.printStackTrace();
		      http_err_msg = "IOエラー";
		    }

		    // shutdownすると通信できなくなる
		    httpClient.getConnectionManager().shutdown();

		    return null;
		  }


		  // タスク終了時
		  protected void onPostExecute(Void unused) {
		    // ダイアログを消す
		    dialog.dismiss();

		    // 受信結果をUIに渡すためにまとめる
		    Message message = new Message();
		    Bundle bundle = new Bundle();
		    if (http_err_msg != null) {
		      // エラー発生時
		      bundle.putBoolean("http_post_success", false);
		      bundle.putString("http_response", http_err_msg);
		    } else {
		      // 通信成功時
		      bundle.putBoolean("http_post_success", true);
		      bundle.putString("http_response", http_ret_msg);
		    }
		    message.setData(bundle);

		    // 受信結果に基づいてUI操作させる
		    ui_handler.sendMessage(message);
		  }

		}




package com.example.beaconrssi;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;	//BTでもBLEでもこの一行で良い
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
//import android.graphics.Color;
//import android.net.Uri;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.math.BigDecimal;
import java.util.*;


import android.util.Log;

// マニフェストファイル(～Manifest.xml)にGPSのパーミッションを追加すること！！
public class MainActivity extends Activity implements LocationListener,SensorEventListener {
    private SensorManager sensorManager;		//センサーマネージャ
    private Sensor        accelerometer;		//加速度センサー
    private Sensor        orientation;  		//回転センサー
//    private Sensor        gyroscope;			//ジャイロスコープ
    private Sensor        magneticField;  		//地磁気センサー
    private double[] senvalues=new double[6];	//加速度と傾き値
	private LocationManager manager = null; 	// GPS測位に用いる

	private double yaw,pitch,roll;	// 方位、ピッチ、ロール
	private double inityaw ,initpitch ,initroll;	// 方位、ピッチ、ロール初期値
	
	private double lowPassX,lowPassY,lowPassZ;		// 加速度を加重平均のため記録する
	private double rawAx,rawAy,rawAz;				// ハイパスフィルタを通った値
	private double ax,ay,az;				// 各方向加速度
	private double vx,vy,vz , dvx,dvy,dvz;	// 各方向速度、微小速度
	private double x,y,z , dx,dy,dz;		// 各方向移動距離、微小距離
//	private double xgyro,ygyro,zgyro ;	// 各方向各加速度（ジャイロ）
	private long nowTime;
	private long oldTime;				// 前回測定時刻
	long interval;

	static double offAx = -0.000;		// オフセット0.0072
	static double offAy = -0.000;		// オフセット0.0021
	static double offAz = -0.000;		// オフセット0.0053
	static double k = 0.95;				// 加重平均の係数 最新計測値の重み
	private int asnflag = 0,jsnflag = 0;	// センサ初期化フラグ
    
	
	//センサ改変　ここから***********************************************************
	SensorEventListener mSensorEventListener;
	float[] accelerometerValues = new float[3];
	float[] geomagneticMatrix = new float[3];
	float[] magneticValues = new float[3];
//	float[] gyroscopeValues = new float[3];
	float[] orientationValues = new float[3];
	
	float[] inR = new float[16];
    float[] outR = new float[16];
    float[] I = new float[16];
	
	boolean sensorReady;
	//センサ改変　ここまで***********************************************************
	// Bluetooth関係の変数
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
	static BluetoothAdapter mBluetoothAdapter;
    private TextView mResultView;
    
    // ライブラリを使用しないBluetooth関係の変数
	int BCONmajor;
	int BCONminor;
	String BCONuuid;
	    
    //監視対象となるビーコンの値
    private static final String TARGET_UUID = "00000000-EA00-1001-B000-001C4D25E26A";
    // ビーコンの２つのIDの最大値
    private static final int TARGET_MAJOR = 1;
    private static final int TARGET_MINOR = 5;
    // ビーコンのidentifierを設定する配列
    String beaconIDArray[][] = {{"Zero-Zero","Zero-One","Zero-Two","Zero-Three","Zero-Four","Zero-Five"},{"One-Zero","One-One","One-Two","One-Three","One-Four","One-Five"}};
    // 発見したビーコンを記録する配列
    int beaconArray[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];
    // 発見したビーコンのRSSIを記録する配列
    int beaconRSSIArray[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];	
    // 発見したBluetoothデバイスの名前とRSSIを記録する配列
    ArrayList<String> btarray = new ArrayList<String>();
    
    // 位置と時刻を保持する配列
    String locationArray[] = {"time","Latitude","Longitude"};// 時刻、緯度、経度の順

    // ハンドラを生成
    final Handler handler = new Handler();
    Timer mTimer = null;
    boolean getRSSI = false;
    
    //ID表示用変数
    private TextView DrawBeaconList;
    //済表示用ビーコン発見フラグ
    public int StampEndFlag[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];
    
    //fAddData log吐き出し用変数宣言**********************************************************************************
    String fnametext = "time";
    String foldernametime = "time";
    String foldernametext = "";

    String X = "0",Y = "0",Z = "0",Pitch = "0",Roll = "0",Azimuth = "0";
    
    String macadd;

    String sUrl = "http://fuji.opendatalab.org/BeaconRSSI/catch.php";
    String fileName = "BeaconRSSI.txt";
    String saveText = "";
    String filePath;
    
    int timetmp,timematch,timecntflag;
    
    //送信時間用定数（数字分ごとに送信）
    static final int TRANSMISSION = 5;
    
    //fAddData log吐き出し用変数宣言ここまで**********************************************************************************
    
    String strDirPath =
    	    Environment.getExternalStorageDirectory().getAbsolutePath()
    	    + "/data/data/com.example.beaconrssi/files/";


    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// スリープしない
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
    	// 変数などを設定する
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
              
        //fAddData log吐き出し用**********************************************************************************
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE); 
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
         
        macadd = wifiInfo.getMacAddress();

        //ファイルクリア***************************************************************************************************
        textclear();
        //**************************************************************************************************************
        //fAddData log吐き出し用ここまで**********************************************************************************

        // Bluetoothのサポートを確かめ、使用を求める
        boolean suportRet = supportConfirm();
        if(suportRet == false){
        	//　Bluetooth非サポートだった場合にトーストを表示する
			Log("Bluetoothがサポートされていません");
        	finish();
        }else{
	        //BluetoothがOffだった場合はダイアログ表示してONにするよう促す
	        boolean btEnabledRet = OnOffConfirm();
	        if(btEnabledRet == false){
		    //BluetoothがONならスルーする
	        }
        }
        // この部分、レガシーBT使おうとしたが、結局いらなかった部分
   		/* Bluetooth Adapter */
    	final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    	mBluetoothAdapter = bluetoothManager.getAdapter();

    	// BluetoothAdapterのインスタンス取得
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	
// 測位関係の初期化 ここから ***********************************************************
        // GPSサービス取得
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
// 測位関係の初期化 ここまで ***********************************************************
//　センサ起動部　ここから***********************************************************
        //センサーマネージャの取得(1)
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //センサーの取得(2)
        List<Sensor> list;
        list=sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (list.size()>0) accelerometer=list.get(0);
        list=sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        if(list.size()>0) magneticField = list.get(0);
//        list=sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
//        if(list.size()>0) gyroscope = list.get(0);


        
//　センサ起動部　ここまで***********************************************************
        
        // 以下表示関係-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
        // readyテキストを表示させる[uedaデバッグ]
        mResultView = (TextView)findViewById(R.id.textView);
        mResultView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mResultView.setText("ready\n");

        // それぞれのボタンを処理する
        setStartButton();	// スタートボタン関係の処理一式
        setStopButton();	// ストップボタン関係の処理一式
    }
    
// 以下、各種処理を記述***********************************************************
    
    //fAddData log吐き出し用**********************************************************************************

    // ファイル読み書き使い方
    //try {         
    // ファイル書き込み
    //saveText = "1"+ 変数1  + "," + "2" + 変数2  + ","+ 変数３ + "改行コード（不明）";//変数・文字データの入力
    //saveText(fileName, saveText);   		//書き込み　ファイル名,入力データの指定 
    // ファイル読み込み
    //String str = loadText(fileName);		//読み込み　ファイルの指定
    // TextViewに読み込んだ文字列を設定（書き出し）
    //((TextView)findViewById(R.id.textView)).setText(str);
    //}catch(IOException e){        
    //e.printStackTrace();   
    //}
     
    //　ファイル読み書き使い方ここまで
    //書き込み処理
    private void saveText(String fileName, String str) throws IOException {
	   	 // ストリームを開く
	   	FileOutputStream output = this.openFileOutput(fileName, MODE_PRIVATE | MODE_APPEND);
	   	 // 書き込み
	   	output.write(str.getBytes());
	   	 // ストリームを閉じる
	   	output.close();
  	}

    private String loadText(String fileName) throws IOException {
    	// ストリームを開く
    	FileInputStream input = this.openFileInput(fileName);
    	// 読み込み
    	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	StringBuffer strBuffer = new StringBuffer();
    	String line;
    	while ((line = reader.readLine()) != null) {
    		strBuffer.append(line);
    	}
    	// ストリームを閉じる
    	reader.close();
   	 	// 読み込んだ文字列を返す
    	return strBuffer.toString();
    }
    
    private void exec_post() {
    	String sTmp;
        
        // 非同期タスクを定義
        HttpPostTask task = new HttpPostTask(
          this,
          sUrl,

          // タスク完了時に呼ばれるUIのハンドラ
          new HttpPostHandler(){

            @Override
            public void onPostCompleted(String response) {
            // 受信結果をUIに表示
            	DrawBeaconList.setText( response );
            }

            @Override
            public void onPostFailed(String response) {
            	DrawBeaconList.setText( response );
              Toast.makeText(
                getApplicationContext(),
                "エラーが発生しました。",
                Toast.LENGTH_LONG
              ).show();
            }
          }
        );
        
        //macaddressをフォルダの名前として使用
        sTmp = macadd;
        task.addPostParam( "macadd", sTmp );
        
        //ファイル名を送信
        sTmp = fnametext;
        task.addPostParam( "post_1", sTmp );

        //logデータの送信
        try {
			sTmp = loadText(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        task.addPostParam( "post_2", sTmp );
        
        FileOutputStream output = null;
		try {
			output = this.openFileOutput(fileName, MODE_PRIVATE);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// 書き込み
		saveText = "";
	   	try {
			output.write(saveText.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	   	 // ストリームを閉じる
	   	try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // タスクを開始
        task.execute();
    }



    //ファイルクリア***************************************************************************************************
    private void textclear() {
        FileOutputStream output = null;
		try {
			output = this.openFileOutput(fileName, MODE_PRIVATE);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	   	 // 書き込み
	   	try {
			output.write(saveText.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   	 // ストリームを閉じる
	   	try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //**************************************************************************************************************


    //fAddData log吐き出し用ここまで**********************************************************************************

// スタートボタンに関する一式の処理
	private void setStartButton() {
        Button startButton = (Button)findViewById(R.id.button);
        startButton.setText("start");
        startButton.setOnClickListener(new View.OnClickListener() {
        // startボタンクリックイベント
            @Override
            public void onClick(View view) {
        //        Intent serviceIntent = new Intent(MainActivity.this, BeaconService.class);
         //       mResultView.append("start\n"); // [uedaデバッグ文字列を表示
                // 監視スタート
        		/* Bluetooth LEデバイスの検索 */
        		mBluetoothAdapter.startLeScan(mLeScanCallback);

        	    // Bluetooth周辺デバイスの検索開始
        	    mBluetoothAdapter.startDiscovery();
        //        startService(serviceIntent);
                getRSSI = true;
                moniteringRssi();
                mResultView.setBackgroundColor(0x33ff0000);	// 背景色を赤系に変える
               // setContentView(mResultView);

			    // フラグをセット
			    asnflag = 0;
			    jsnflag = 0;
			    
                yaw = pitch = roll = 0;		// 方位、ピッチ、ロール
            	rawAx = rawAy =rawAz =0; 
                
            }
        });
    }

// ストップボタンに関する一式の処理
	private void setStopButton() {
        Button stopButton = (Button)findViewById(R.id.button2);
        stopButton.setText("stop");
        stopButton.setOnClickListener(new View.OnClickListener() {
        // stopボタンクリックイベント
            @Override
            public void onClick(View view) {
                mResultView.setBackgroundColor(0x220000ff);	//背景色を青系に変える
                mBluetoothAdapter.stopLeScan(mLeScanCallback);	//スキャンをやめる

		        // Bluetooth検索中止
                if (mBluetoothAdapter.isDiscovering()) {
		            	mBluetoothAdapter.cancelDiscovery();
		            	}
                getRSSI = false;
                moniteringRssi();
        		// 画面を初期化***********************************************************
                DrawBeaconList = (TextView)findViewById(R.id.textView);
                DrawBeaconList.setMovementMethod(ScrollingMovementMethod.getInstance());
               	DrawBeaconList.setText("\n"); 
               	// 画面初期化　ここまで***********************************************************

                //fAddData log吐き出し用**********************************************************************************
               	timecntflag=0;

                exec_post();

                //fAddData log吐き出し用ここまで**********************************************************************************

            }
        });
    }
    
// インターバルでビーコンのRSSIを取得する処理
	private void moniteringRssi() {
        // タイマーを生成
        Timer mTimer = new Timer(true);
    	if(getRSSI == true){	// タイマ処理を行う場合
       //     Timer mTimer = new Timer(true);	// この位置だとストップしたときに落ちる
	        // スケジュールを設定
	        mTimer.schedule(new TimerTask() {
	        	public void run() {
	        		handler.post(new Runnable() {
	        			public void run() {
	        				getRSSI();
	        			}
	        		});
	        		}
	        	}, 1000, 1000); // 初回起動の遅延と周期を指定、
        }else if(getRSSI == false){	// タイマ処理を行なわない場合
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

	// Beacon探索 電界強度の取得(インターバル処理)*************************************************
	    public void getRSSI(){
			// 実際に処理をする内容を記述
            mBluetoothAdapter.stopLeScan(mLeScanCallback);	//スキャンをやめる		// -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-信号強度を取得する　ここから

            // 画面を初期化
            DrawBeaconList = (TextView)findViewById(R.id.textView);
            DrawBeaconList.setMovementMethod(ScrollingMovementMethod.getInstance());
           	DrawBeaconList.setText("\n"); 
           	
           	if(getRSSI == true){	// 捜索実行中なら
           	//今回追加した部分　ここから***********************************************************
                    Time time = new Time("Asia/Tokyo");	//時刻を取得
                    time.setToNow();					//現在の時刻を設定
                    locationArray[0] = time.year + "年" + (time.month+1) + "月" + time.monthDay + "日  "+ time.hour + "時" + time.minute + "分" + time.second + "秒";

                    // システム時刻 、時刻、緯度、経度を表示する
                    mResultView.append("システム時刻："+nowTime+"\n時刻："+locationArray[0]+"\t\t緯度："+locationArray[1]+"  経度："+locationArray[2]+"\n");

                    
                    // 加速度と傾きを表示する
                    mResultView.append( "X軸a :"+ (String.format("%4.2f", senvalues[0])) + "m/s^2\t\t"+
                    					"Y軸a :"+ (String.format("%4.2f", senvalues[1])) + "m/s^2\t\t"+
                    					"Z軸a :"+ (String.format("%4.2f", senvalues[2]))+"m/s^2\n");
                    mResultView.append( "rawY :"+ (String.format("%4.1f", senvalues[3])) + "deg\t\t\t" +
                    					"rawP :"+ (String.format("%4.1f", senvalues[4])) + "deg\t\t\t"+
                    					"rawR :"+ (String.format("%4.1f", senvalues[5])) +"deg\n"+
                    					"yaw  :"+ (String.format("%4.1f", yaw))  + "deg\t\t\t"+
                    					"pitch:"+ (String.format("%4.1f", pitch))+ "deg\t\t\t"+
                    					"roll :"+ (String.format("%4.1f", roll)) + "deg\n");
                    
                    mResultView.append( "lowPassX:"+(String.format("%4.2f", lowPassX))+" m/s^2\t\t"+
                    					"lowPassY:"+(String.format("%4.2f", lowPassY))+" m/s^2\t\t"+
                    					"lowPassZ:"+(String.format("%4.2f", lowPassZ))+" m/s^2\n");
                    mResultView.append( "以下変換後の値\n"+
                    					"ax:"+(String.format("%4.2f", ax))+" m/s^2\t"+
                    					"ay:"+(String.format("%4.2f", ay))+" m/s^2\t"+
                    					"az:"+(String.format("%4.2f", az))+" m/s^2\n\n");
                     
                    // 微小速度を表示する
                    mResultView.append( "dvx:"+(String.format("%5.2f",dvx))+"m/s\t"+
                    					"dvY:"+(String.format("%5.2f",dvy))+"m/s\t"+
                    					"dvz:"+(String.format("%5.2f",dvz))+"m/s\n");
                    // 速度を表示する
                    mResultView.append("vX:"+(String.format("%5.2f",vx))+"m/s\tvY:"+(String.format("%5.2f",vy))+"m/s\tvZ:"+(String.format("%5.2f",vz))+"m/s\n\n");
                    // 微小距離を表示する
                    mResultView.append("dx:"+(String.format("%5.2f",dx))+"m\tdY:"+(String.format("%5.2f",dy))+"m\tdz:"+(String.format("%5.2f",dz))+"m\n");
                    // 移動距離を表示する
                    mResultView.append("X:"+(int)x+"m\tY:"+(int)y+"m\tz:"+(int)z+"m\n\n");
                    
           	//今回追加した部分　ここまで***********************************************************

                    // UUIDを表示
                    mResultView.append("UUID : "+TARGET_UUID+"\n");
                                        
              	    //fAddData log吐き出し用**********************************************************************************
                    if((time.month+1) >= 10){
                    	textlocationArray[0] = time.year + "/";
                    	fnametext = time.year + "";
                    }else{
                    	textlocationArray[0] = time.year + "/0";
                    	fnametext = time.year + "0";
                    }
                    if((time.monthDay) >= 10){
                    	textlocationArray[0] = textlocationArray[0] + (time.month+1);
                    	fnametext = fnametext + time.monthDay;
                    }else{
                    	textlocationArray[0] = textlocationArray[0] + "/0" + time.monthDay;
                    	fnametext = fnametext + "0" + time.monthDay;
                    }
                    if((time.hour) >= 10){
                    	textlocationArray[0] = textlocationArray[0] + "/" + time.hour;
                    	fnametext = fnametext + time.hour;
                    }else{
                    	textlocationArray[0] = textlocationArray[0] + "/0"+ time.hour;
                    	fnametext = fnametext + "0"+ time.hour;
                    }
                    if((time.minute) >= 10){
                       	textlocationArray[0] = textlocationArray[0] + "/" + time.minute;
                       	fnametext = fnametext + time.minute;
                    }else{
                    	textlocationArray[0] = textlocationArray[0] + "/0" + time.minute;
                    	fnametext = fnametext + "0" + time.minute;
                    }
                    if((time.second) >= 10 ){
                    	textlocationArray[0] = textlocationArray[0] + "/" + time.second + "";
                    	fnametext = fnametext + time.second + "";
                    }else{
                    	textlocationArray[0] = textlocationArray[0] + "/0" + time.second + "";
                    	fnametext = fnametext + "0" + time.second + "";
                    }
                    
                    timematch = time.minute;
                    if(timecntflag==1){
                 	   timetmp = (timematch+TRANSMISSION)%60;
                 	   timecntflag=2;
                    }

		    //データ代入
                    X = senvalues[0] + "";
                    Y = senvalues[1] + "";
                    Z = senvalues[2] + "";
                    Pitch = pitch + "";
                    Roll = roll + "";
                    Azimuth = yaw + "";

                    // ファイル書き込み
                    try {
                    	saveText = textlocationArray[0] + "," + textlocationArray[1] + "," + textlocationArray[2] + "," + X + "," + Y + "," + Z + "," + Pitch + "," + Roll + "," + Azimuth + ",";

                    	saveText(fileName, saveText);
                    }catch(IOException e){
                    	e.printStackTrace();
                    }
                	// ファイル書き込みここまで

                    // 画面にIDと信号強度を描画
		            for( int i=0 ; i <= TARGET_MAJOR ; i++ ){
		    	        for( int j=0 ; j <= TARGET_MINOR ; j++ ){
		    	        	mResultView.append("主ID:" +i+ "副ID:" + j+ "強度:"+beaconRSSIArray[i][j]+"\n");

	                    	// ファイル書き込み
		                    try {
		                    	saveText = /*i + "," + j + "," +*/ beaconRSSIArray[i][j] + ",";
		                    	saveText(fileName, saveText);
		                    }catch(IOException e){
		                    	e.printStackTrace();
		                    }
	                    	// ファイル書き込みここまで
		    	        }
	    	        	mResultView.append("\n");	// メジャーIDごとに改行を挟む
		    	    }
                	// ファイル書き込み
                    try {
                    	saveText = "\t";
                    	saveText(fileName, saveText);
                    }catch(IOException e){
                    	e.printStackTrace();
                    }
                	// ファイル書き込みここまで

	            if(timecntflag==2 && timematch==timetmp){
	            	   timetmp = (timematch+TRANSMISSION)%60;
	            	   exec_post();
	            }
                    //fAddData log吐き出し用ここまで**********************************************************************************
	        
	      	    //Bluetoothのリストを表示
	            //ArrayListを1件ずつ取り出し画面に表示する
	            Iterator<String> it = btarray.iterator();
	            while (it.hasNext()) {
	            	mResultView.append(it.next());
	            }
        		mBluetoothAdapter.startLeScan(mLeScanCallback);	//スキャン再開
		    }else if(getRSSI == false){
		    	mResultView.append("stop\n");	// stopを表示
		    }
			// -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-信号強度を取得する　ここまで
	    }

	// ディバイス名、RSSIを取得し、UUID、majorID、minorIDを生成する
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan( final BluetoothDevice device , int rssi , byte[] scanRecord) {
			if (scanRecord.length > 30) {
					if ((scanRecord[5] == (byte)0x4c) && (scanRecord[6] == (byte)0x00) && (scanRecord[7] == (byte)0x02) && (scanRecord[8] == (byte)0x15)) {
					String uuid = IntToHex2(scanRecord[9] & 0xff)
					+ IntToHex2(scanRecord[10] & 0xff)
					+ IntToHex2(scanRecord[11] & 0xff)
					+ IntToHex2(scanRecord[12] & 0xff)
					+ "-"
					+ IntToHex2(scanRecord[13] & 0xff)
					+ IntToHex2(scanRecord[14] & 0xff)
					+ "-"
					+ IntToHex2(scanRecord[15] & 0xff)
					+ IntToHex2(scanRecord[16] & 0xff)
					+ "-"
					+ IntToHex2(scanRecord[17] & 0xff)
					+ IntToHex2(scanRecord[18] & 0xff)
					+ "-"
					+ IntToHex2(scanRecord[19] & 0xff)
					+ IntToHex2(scanRecord[20] & 0xff)
					+ IntToHex2(scanRecord[21] & 0xff)
					+ IntToHex2(scanRecord[22] & 0xff)
					+ IntToHex2(scanRecord[23] & 0xff)
					+ IntToHex2(scanRecord[24] & 0xff);
				// 変数を代入する
					BCONuuid = uuid ;		
					//BCONmajor = IntToHex2(scanRecord[25] & 0xff) + IntToHex2(scanRecord[26] & 0xff);
					BCONmajor=Integer.parseInt(IntToHex2(scanRecord[26] & 0xff));
					//BCONminor = IntToHex2(scanRecord[27] & 0xff) + IntToHex2(scanRecord[28] & 0xff);
					BCONminor=Integer.parseInt(IntToHex2(scanRecord[28] & 0xff));
					beaconRSSIArray[BCONmajor][BCONminor] = rssi ;
				}
			}
		}
	};
		// 10進数を16進数へ変換する。
		private String IntToHex2(int i) {
		char hex_2[] = {Character.forDigit((i>>4) & 0x0f,16),Character.forDigit(i&0x0f, 16)};
		String hex_2_str = new String(hex_2);
		return hex_2_str.toUpperCase();
	}
		
		// Bluetoothサポートの有無判定処理***********************************************************  
		// Bluetoothの使用に関する処理=========================================================
		// Bluetoothの有無を確かめる処理**********************************************
		    private boolean supportConfirm(){
		        //Bluetoothがサポートされているか否かを確かめる
		        BluetoothAdapter Bt = BluetoothAdapter.getDefaultAdapter();
		        if(Bt.equals(null)){
		        	return false;
		        }
		        mBluetoothAdapter = Bt;
		        return true;
		    }

		 // BluetoothのON/OFF判定処理***********************************************************
		 // BluetoothのON/OFFを確かめる処理*******************************************
		    private boolean OnOffConfirm(){
		    	//BluetoothがOnになっているかどうかを確かめる
		    	boolean btEnable = mBluetoothAdapter.isEnabled();
		    	if(btEnable == true){
		    		return true;
		    	}
		    	//Onでない場合、Onにするためのダイアログを表示する
		    	Intent BtOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    	startActivityForResult(BtOn, REQUEST_ENABLE_BLUETOOTH);
		    	return false;
		    }

		 // Bluetooth使用要求処理***********************************************************
		 // Bluetoothの使用許可を求める確かめる処理**************************************
		    @Override
		    protected void onActivityResult(int requestCode, int ResultCode, Intent date){
		    	if(requestCode == REQUEST_ENABLE_BLUETOOTH){
		    		if(ResultCode == Activity.RESULT_OK){
		    			//BluetoothがOnだった場合の処理
		    			Log("BluetoothがONになりました");
		        		
		    		}else{
		                error("Bluetoothを利用出来ません");
		    		}
		    	}
		    }

//　センサ　ここから***********************************************************		
		    @Override
		    public void onSensorChanged(SensorEvent event) {
		    	//ネタ元　http://seesaawiki.jp/w/moonlight_aska/d/%BC%A7%B5%A4/%B2%C3%C2%AE%C5%D9%A5%BB%A5%F3%A5%B5%A1%BC%A4%C7%CA%FD%B0%CC%B3%D1/%B7%B9%A4%AD%A4%F2%B8%A1%BD%D0%A4%B9%A4%EB
		        //加速度の取得
		        if(event.sensor == accelerometer){
		            accelerometerValues[0] = event.values[0];
		            accelerometerValues[1] = event.values[1];
		            accelerometerValues[2] = event.values[2];
		        }
		        //地磁気の取得
		        if(event.sensor == magneticField){
		            magneticValues[0] = event.values[0];
		            magneticValues[1] = event.values[1];
		            magneticValues[2] = event.values[2];
		        }
		        /*
		        // ジャイロ（角加速度）の取得
		        if(event.sensor == gyroscope) {
		        	gyroscopeValues[0] = event.values[0];	// X軸の回転速度
		        	gyroscopeValues[1] = event.values[1];	// Y軸の回転速度
		        	gyroscopeValues[2] = event.values[2];	// Z軸の回転速度
		          }
		          */

			//傾きの算出-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
		       // if (magneticValues != null && accelerometerValues != null && gyroscopeValues != null) {
		        if (magneticValues != null && accelerometerValues != null ) {

		            SensorManager.getRotationMatrix(inR, I, accelerometerValues, magneticValues);

		            //画面の向きによって軸の変更可
		            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
		            SensorManager.getOrientation(outR, orientationValues);
		            
		            //ラジアンから度への変換 及び方位の範囲を-180～180度から0～359度に変換
		            /*
		            float angle = radianToDegree(orientationValues[0]);
		            if		(angle >= 0){ orientationValues[0] = angle;}
		            else if (angle <  0){ orientationValues[0] = 360 + angle;}
		            */

		            orientationValues[0] = radianToDegree(orientationValues[0]);// 範囲の変換をしない
		            orientationValues[1] = radianToDegree(orientationValues[1]);
		            orientationValues[2] = radianToDegree(orientationValues[2]);
		        }
		    

		        //出力するための配列に格納
		        senvalues[0] = accelerometerValues[0]- offAx;
		        senvalues[1] = accelerometerValues[1]- offAy;
		        senvalues[2] = accelerometerValues[2]- offAz;
		        senvalues[3] = orientationValues[0];
		        senvalues[4] = orientationValues[1];
		        senvalues[5] = orientationValues[2];
		        //senvalues[6] = gyroscopeValues[0];
		        //senvalues[7] = gyroscopeValues[1];
		        //senvalues[8] = gyroscopeValues[2];
		        
		        // 切り取り線　-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
		        // TODO Auto-generated method stubf
				// Low Pass Filter
		        /*
				lowPassX += (senvalues[0] - lowPassX) * k;
				lowPassY += (senvalues[1] - lowPassY) * k;
				lowPassZ += (senvalues[2] - lowPassZ) * k;
				
				// High Pass Filter
				rawAx = senvalues[0];
				rawAy = senvalues[1];
				rawAz = senvalues[2];
				*/
	            // 各加速度の初回測定値を初期値としてセット
	            if(asnflag == 0){
	            	lowPassX = senvalues[0] ;// 前回測定値との差分、座標変換しない仮の値を放り込む
	            	lowPassY = senvalues[1] ;
	            	lowPassZ = senvalues[2] ;
	            }else{
					// Low Pass Filter
					// 「信号 平滑化」でGoogle検索すると詳しい情報がたくさん見つかります。
					lowPassX += (senvalues[0] - lowPassX) * k; // event.values[0から2] はそれぞれ端末座標系での生の加速度です
					lowPassY += (senvalues[1] - lowPassY) * k;
					lowPassZ += (senvalues[2] - lowPassZ) * k;
	            }

				// High Pass Filter
				rawAx = senvalues[0] - lowPassX;
				rawAy = senvalues[1] - lowPassY;
				rawAz = senvalues[2] - lowPassZ;
				ax = rawAx;
				ay = rawAy;
				az = rawAz;
				/*
	            // ヨー、ピッチ、ロールの初回測定値を初期値としてセット
	            if(jsnflag ==0){
				    inityaw =  senvalues[3];
				    initpitch =  senvalues[4];
				    initroll =  senvalues[5];
	            	// フラグをセット
				    jsnflag = 1;
	            }
	            // ジャイロからヨー、ピッチ、ロール
				xgyro   = senvalues[6]* interval/1000 +inityaw;
				ygyro  = senvalues[7]* interval/1000 +initpitch;
				zgyro  = senvalues[8]* interval/1000 +initroll;
			    
				yaw   = xgyro * interval/1000 +inityaw;
				pitch = ygyro * interval/1000 +initpitch;
				roll  = zgyro * interval/1000 +initroll;
				*/
				/*
	            // ヨー、ピッチ、ロールの変化分を取る
				yaw   = senvalues[3] -inityaw;
				pitch = senvalues[4] -initpitch;
				roll  = senvalues[5] -initroll;
				inityaw   = senvalues[3] ;
				initpitch = senvalues[4] ;
				initroll  = senvalues[5] ;
				*/
	            // ヨー、ピッチ、ロール
				yaw   = senvalues[3] ;
				pitch = senvalues[4] ;
				roll  = senvalues[5] ;

		        // TODO Auto-generated method stub
				/*
				// 座標変換
				// ピッチ・ロール
				double nPitchRad = Math.toRadians(-pitch); // n means negative
				double sinNPitch = Math.sin(nPitchRad);
				double cosNPitch = Math.cos(nPitchRad);
				 
				double nRollRad = Math.toRadians(-roll);
				double sinNRoll = Math.sin(nRollRad);
				double cosNRoll = Math.cos(nRollRad);
				 
				double bx, by; // 一時退避
				 
				bx = rawAx * cosNRoll + rawAz * sinNRoll;
				by = rawAx * sinNPitch * sinNRoll + rawAy * cosNPitch - rawAz * sinNPitch * cosNRoll;
				az = -rawAx * cosNPitch * sinNRoll + rawAy * sinNPitch * cosNRoll + rawAz * cosNPitch * cosNRoll;
				 
				// 方位
				double nAzimuthRad = Math.toRadians(-yaw);
				double sinNAzimuth = Math.sin(nAzimuthRad);
				double cosNAzimuth = Math.cos(nAzimuthRad);
				 
				ax = bx * cosNAzimuth - by * sinNAzimuth;
				ay = bx * sinNAzimuth + by * cosNAzimuth;
				*/
			    // ax, ay, az が求まった後で、測定間隔を求める
			    nowTime = System.currentTimeMillis();	//システムの現在時刻をミリ秒（long型の数値）で取得
			    interval = nowTime - oldTime; 			// intervalを求める
			    oldTime = nowTime;						// 現在の時刻を保存
			    /*
		        Log.d("Sensor", interval+"ms X軸加速度:"+senvalues[0]+"　Y軸加速度:"+senvalues[1]+"　Z軸加速度:"+senvalues[2]+"\n" +
		        		"方位:"+senvalues[3]+"　ピッチ:"  +senvalues[4]+"　ロール:"  +senvalues[5]);
		        */
	           // 加速度を積分する
	           //加速度を[mm/s^2]、インターバルタイムを[s]とみなして、
	           dvx = ax * interval/1000; 	// 速度[m/s] の変化分にする
	           dvy = ay * interval/1000;
	           dvz = az * interval;
	           vx += dvx;					// 速度に変化分を足しこむ
	           vy += dvy;
	           vz += dvz;

			    // ***********************************************************
	           if( asnflag == 1 ){
	        	   dx = vx * interval/1000;	// 距離[m] の変化分にする
	        	   dy = vy * interval/1000;
	        	   dz = vz * interval/1000;
	        	   x += dx;					// 距離[m] にする
	        	   y += dy;
	        	   z += dz;
	            }else{
	            	vx = vy = vz = 0;		//　速度初期化
	            	x = y = z = 0 ;			//　移動距離初期化
	            	// フラグをセット
				    asnflag = 1;
	            }				
		    }

		    /* ラジアンから度への変換*/ 
		    int radianToDegree(float rad){
		        return (int) Math.floor( Math.toDegrees(rad) ) ;
		    }
		    

		    //精度変更イベントの処理
		    public void onAccuracyChanged(Sensor sensor,int accuracy) {
		    }

//　センサ　ここまで***********************************************************	
// 測位　ここから ***********************************************************
		    @Override
		    protected void onPause() {
		        if(manager != null) {
		            manager.removeUpdates(this);
		        }
		        super.onPause();	//これを省くとアプリが落ちる
		        
		    }

		    @Override
		    protected void onResume() {
		        if(manager != null) {
		            manager.requestLocationUpdates(LocationManager.
		            		NETWORK_PROVIDER,	//ネットワーク測位ならこれ
		            	//	GPS_PROVIDER		//GPS測位ならこれ
		            		0, 0, this);
		        }
		        super.onResume();	//これを省くとアプリが落ちる

		        //センサの処理の開始
		        if(accelerometer != null)
		            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		        if(magneticField != null)
		            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
		       // if(gyroscope != null)
		       //     sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
		        
		        
		    }
		    // 測位が実行されると呼ばれる
		    @Override
		    public void onLocationChanged(Location location) {
    			Log("測位！");	//***********************************************************テストポイント（測位時にトースト出力）
		    	locationArray[1] = "" + location.getLatitude();	//緯度を取得し配列に入れる
		        locationArray[2] = "" + location.getLongitude();//経度を取得し配列に入れる
		    }

		    @Override
		    public void onProviderDisabled(String provider) {}

		    @Override
		    public void onProviderEnabled(String provider) {}

		    @Override
		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    @Override
		    protected void onStop() {
		      super.onStop();
		      //センサーの処理の停止
		      sensorManager.unregisterListener(this);
		    }

//　測位　ここまで***********************************************************	
// Logをトーストで表示する処理***********************************************************    
// 表示に関する処理=====================================================================
//ログをトーストで表示**********************************************************
    public void Log(String string){
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();    	
    }
    
 // エラー表示処理***********************************************************        
//エラーメッセージをアラートで表示**************************************************
    private void error(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder (this);
        builder.setTitle("エラー");
        builder.setMessage(msg);
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
/* 測位のもとはここ↓
 * http://seesaawiki.jp/w/moonlight_aska/d/%B0%CC%C3%D6%BE%F0%CA%F3%A4%F2%BC%E8%C6%C0%A4%B9%A4%EB
 */ 
//-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-ソースここまで-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-

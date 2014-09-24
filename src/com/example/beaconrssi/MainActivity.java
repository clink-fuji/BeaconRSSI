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
    private double[] senvalues=new double[6];	//加速度と傾き値
	private LocationManager manager = null; 	// GPS測位に用いる

	double lowPassX,lowPassY,lowPassZ;		// 加速度を加重平均のため記録する
	double rawAx,rawAy,rawAz;				// ハイパスフィルタを通った値
    
	private static double yaw,pitch,roll;	// 方位、ピッチ、ロール
	private static double inityaw ,initpitch ,initroll;	// 方位、ピッチ、ロール初期値
	
	private static double ax,ay,az;					// 各方向加速度
	private static int vx,vy,vz , dvx,dvy,dvz;		// 各方向速度、微小速度
	private static int x,y,z , dx,dy,dz;				// 各方向移動距離、微小距離
	private static long nowTime;
	private static long oldTime;					// 前回測定時刻

	static double offAx = +0.0;		// オフセット
	static double offAy = -0.0;		// オフセット
	static double offAz = +0.0;		// オフセット
	static double k = 0.3;	// 加重平均の係数
	static int asnflag = 0,jsnflag = 0;	// センサ初期化フラグ
    
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// スリープしない
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
    	// 変数などを設定する
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
              
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

        //センサーマネージャの取得(1)
        sensorManager=(SensorManager)getSystemService(
            Context.SENSOR_SERVICE);
        //センサーの取得(2)
        List<Sensor> list;
        list=sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (list.size()>0) accelerometer=list.get(0);
        list=sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (list.size()>0) orientation=list.get(0);
        
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
			    
                yaw = pitch = roll = 0;			// 方位、ピッチ、ロール
            	lowPassX = lowPassY = 0;	// オフセット
            	lowPassZ = 9.8066;
            	rawAx = rawAy = rawAz = 0;			// 初期化
                
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

                    // システム時刻を表示する
                    mResultView.append("システム時刻："+nowTime+"\n");
                    // 時刻、緯度、経度を表示する
                    mResultView.append("時刻："+locationArray[0]+"\t\t緯度："+locationArray[1]+"  経度："+locationArray[2]+"\n");
                    // 加速度と傾きを表示する
                    mResultView.append( "X軸a:"+senvalues[0]+"　Y軸a:"+senvalues[1]+"　Z軸a:"+senvalues[2]+"\n");
                    mResultView.append( "Y:"+senvalues[3]+"\t\t\tyaw："+yaw+"\nP:"  +senvalues[4]+"\t\t\tpitch："+pitch+"\nR:"  +senvalues[5]+"\t\t\troll："+roll+"\n");
                    mResultView.append( "rawAx:"+rawAx+"\n rawAy:"+rawAy+"\n rawAz:"+rawAz+"\n以下座標変換後の値\nax:"+ax+"mm/s^2\n ay:"+ay+"mm/s^2\n az:"+az+"mm/s^2\n\n");

                    // 微小速度を表示する
                    mResultView.append("dvx:"+dvx+"mm/s\tdvY:"+dvy+"mm/s\tdvz:"+dvz+"mm/s\n");
                    // 速度を表示する
                    mResultView.append("vX:"+vx+"mm/s\tvY:"+vy+"mm/s\tvZ:"+vz+"mm/s\n\n");
                    // 微小距離を表示する
                    mResultView.append("dx:"+dx+"mm\tdY:"+dy+"mm\tdz:"+dz+"mm\n");
                    // 移動距離を表示する
                    mResultView.append("X:"+(int)x+"cm\tY:"+(int)y+"cm\tz:"+(int)z+"cm\n\n");
           	//今回追加した部分　ここまで***********************************************************

                    // UUIDを表示
                    mResultView.append("UUID : "+TARGET_UUID+"\n");
                    // 画面にIDと信号強度を描画     
		            for( int i=0 ; i <= TARGET_MAJOR ; i++ ){
		    	        for( int j=0 ; j <= TARGET_MINOR ; j++ ){
		    	        	mResultView.append("主ID:" +i+ "副ID:" + j+ "強度:"+beaconRSSIArray[i][j]+"\n");
		    	        }
	    	        	mResultView.append("\n");	// メジャーIDごとに改行を挟む
		    	    }
	        	mResultView.append("\n");
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
		    //センサーリスナーの処理(5)
		    public void onSensorChanged(SensorEvent event) {
		        //加速度の取得0
		        if (event.sensor==accelerometer) {
		            senvalues[0]=event.values[0];	// 取得した値を配列に保存
		            senvalues[1]=event.values[1];
		            senvalues[2]=event.values[2];
				    // ***********************************************************
					// Low Pass Filter
					lowPassX += (senvalues[0] - lowPassX) * k;
					lowPassY += (senvalues[1] - lowPassY) * k;
					lowPassZ += (senvalues[2] - lowPassZ) * k;
					// High Pass Filter
					rawAx = senvalues[0] - lowPassX;
					rawAy = senvalues[1] - lowPassY;
					rawAz = senvalues[2] - lowPassZ;
					//オフセット
					rawAx -= offAx;
					rawAy -= offAy;
					rawAz -= offAz;
					Log.d("Count","加速度");
		        }
		        //方向の取得
		        if (event.sensor==orientation) {
		            senvalues[3]=event.values[0];	// 取得した値を配列に保存
		            senvalues[4]=event.values[1];
		            senvalues[5]=event.values[2];
		            
		            if(jsnflag ==0){
					    inityaw =  senvalues[3];
					    initpitch =  senvalues[4];
					    initroll =  senvalues[5];
		            	// フラグをセット
					    jsnflag = 1;
		            }
					yaw = senvalues[3] -inityaw;
					pitch = senvalues[4] -initpitch;
					roll = senvalues[5] -initroll;

					double nPitchRad = Math.toRadians(-pitch); // 各変数をラジアンへ変換、n means negative
					double nRollRad = Math.toRadians(-roll);
					double nyawRad = Math.toRadians(-yaw);
		            
					Log.d("Count","回転");
			// ***********************************************************
					// ピッチ
					double sinNPitch = Math.sin(nPitchRad);
					double cosNPitch = Math.cos(nPitchRad);
					// ロール
					double sinNRoll = Math.sin(nRollRad);
					double cosNRoll = Math.cos(nRollRad);
					// 方位（アジマス）
					double sinNyaw = Math.sin(nyawRad);
					double cosNyaw = Math.cos(nyawRad);
		        
					// 一時退避変数
					double bx, by; 
					bx = rawAx * cosNRoll + rawAz * sinNRoll;
					by = rawAx * sinNPitch * sinNRoll + rawAy * cosNPitch - rawAz * sinNPitch * cosNRoll;
					// 端末内部の座標から実空間での座標へ変換し、ローパスフィルタを通す
					ax += ((bx * cosNyaw - by * sinNyaw)*1000 - ax )*k ;
					ay += ((bx * sinNyaw + by * cosNyaw)*1000 - ay )*k ;
					az += ((-rawAx * cosNPitch * sinNRoll + rawAy * sinNPitch * cosNRoll + rawAz * cosNPitch * cosNRoll)*1000 - az )*k ;
			}

			   // ax, ay, az が求まった後で
			   nowTime = System.currentTimeMillis();	//システムの現在時刻をミリ秒（long型の数値）で取得
			   long interval = nowTime - oldTime; // intervalをミリ秒から秒へ変換
			   oldTime = nowTime;
		        Log.d("Sensor", interval+"ms X軸加速度:"+senvalues[0]+"　Y軸加速度:"+senvalues[1]+"　Z軸加速度:"+senvalues[2]+"\n" +
		        		"方位:"+senvalues[3]+"　ピッチ:"  +senvalues[4]+"　ロール:"  +senvalues[5]);
			   
	           if(asnflag == 1){
	        	   // ***********************************************************
	        	   // 加速度を積分する
	        	   dvx = (int) (ax * interval/1000); // 速度[mm/s] の変化分にする
	        	   dvy = (int) (ay * interval/1000);
	        	   dvz = (int) (az * interval/1000);
	        	   vx += dvx;	// 速度に変化分を足しこむ
	        	   vy += dvy;
	        	   vz += dvz;
	        	   
	        	   
	        	   dx = (int) (vx * interval/1000);	// 距離[mm] の変化分にする
	        	   dy = (int) (vy * interval/1000);
	        	   dz = (int) (vz * interval/1000);
	        	   x += dx/10;						// 距離[cm] にする
	        	   y += dy/10;
	        	   z += dz/10;
	        	   // ***********************************************************
	            }else{
	            	vx = vy = vz = 0;					//　速度初期化
	            	x = y = z = 0 ;						//　移動距離初期化
	            	// フラグをセット
				    asnflag = 1;
	            }
			}

		    //精度変更イベントの処理
		    public void onAccuracyChanged(Sensor sensor,int accuracy) {
		    }

//　センサ　ここまで***********************************************************	
// 測位　ここから ***********************************************************
		    @Override
		    protected void onPause() {
		        // TODO Auto-generated method stub
		        if(manager != null) {
		            manager.removeUpdates(this);
		        }
		        super.onPause();	//これを省くとアプリが落ちる
		    }

		    @Override
		    protected void onResume() {
		        // TODO Auto-generated method stub
		        if(manager != null) {
		            manager.requestLocationUpdates(LocationManager.
		            		NETWORK_PROVIDER,	//ネットワーク測位ならこれ
		            	//	GPS_PROVIDER		//GPS測位ならこれ
		            		0, 0, this);
		        }
		        super.onResume();	//これを省くとアプリが落ちる
		        
		        //センサーの処理の開始(3)
		        if (accelerometer!=null) {
		            sensorManager.registerListener(this,accelerometer,
		                SensorManager.SENSOR_DELAY_GAME);
		        }
		        if (orientation!=null) {
		            sensorManager.registerListener(this,orientation,
		                SensorManager.SENSOR_DELAY_GAME);
		        }
		        
		    }
		    // 測位が実行されると呼ばれる
		    @Override
		    public void onLocationChanged(Location location) {
    			Log("測位！");	//***********************************************************テストポイント（測位時にトースト出力）
		        // TODO Auto-generated method stub
		    	locationArray[1] = "" + location.getLatitude();	//緯度を取得し配列に入れる
		        locationArray[2] = "" + location.getLongitude();//経度を取得し配列に入れる
		    }

		    @Override
		    public void onProviderDisabled(String provider) {
		        // TODO Auto-generated method stub
		    }

		    @Override
		    public void onProviderEnabled(String provider) {
		        // TODO Auto-generated method stub
		    }

		    @Override
		    public void onStatusChanged(String provider, int status, Bundle extras) {
		        // TODO Auto-generated method stub
		    }

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
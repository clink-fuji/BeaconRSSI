package com.example.beaconrssi;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;	//BT�ł�BLE�ł����̈�s�ŗǂ�
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

// �}�j�t�F�X�g�t�@�C��(�`Manifest.xml)��GPS�̃p�[�~�b�V������ǉ����邱�ƁI�I
public class MainActivity extends Activity implements LocationListener,SensorEventListener {
    private SensorManager sensorManager;		//�Z���T�[�}�l�[�W��
    private Sensor        accelerometer;		//�����x�Z���T�[
    private Sensor        orientation;  		//��]�Z���T�[
    private double[] senvalues=new double[6];	//�����x�ƌX���l
	private LocationManager manager = null; 	// GPS���ʂɗp����

	double lowPassX,lowPassY,lowPassZ;		// �����x�����d���ς̂��ߋL�^����
	double rawAx,rawAy,rawAz;				// �n�C�p�X�t�B���^��ʂ����l
    
	private static double yaw,pitch,roll;	// ���ʁA�s�b�`�A���[��
	private static double inityaw ,initpitch ,initroll;	// ���ʁA�s�b�`�A���[�������l
	
	private static double ax,ay,az;					// �e���������x
	private static int vx,vy,vz , dvx,dvy,dvz;		// �e�������x�A�������x
	private static int x,y,z , dx,dy,dz;				// �e�����ړ������A��������
	private static long nowTime;
	private static long oldTime;					// �O�񑪒莞��

	static double offAx = +0.0;		// �I�t�Z�b�g
	static double offAy = -0.0;		// �I�t�Z�b�g
	static double offAz = +0.0;		// �I�t�Z�b�g
	static double k = 0.3;	// ���d���ς̌W��
	static int asnflag = 0,jsnflag = 0;	// �Z���T�������t���O
    
	// Bluetooth�֌W�̕ϐ�
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
	static BluetoothAdapter mBluetoothAdapter;
    private TextView mResultView;
    
    // ���C�u�������g�p���Ȃ�Bluetooth�֌W�̕ϐ�
	int BCONmajor;
	int BCONminor;
	String BCONuuid;
	    
    //�Ď��ΏۂƂȂ�r�[�R���̒l
    private static final String TARGET_UUID = "00000000-EA00-1001-B000-001C4D25E26A";
    // �r�[�R���̂Q��ID�̍ő�l
    private static final int TARGET_MAJOR = 1;
    private static final int TARGET_MINOR = 5;
    // �r�[�R����identifier��ݒ肷��z��
    String beaconIDArray[][] = {{"Zero-Zero","Zero-One","Zero-Two","Zero-Three","Zero-Four","Zero-Five"},{"One-Zero","One-One","One-Two","One-Three","One-Four","One-Five"}};
    // ���������r�[�R�����L�^����z��
    int beaconArray[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];
    // ���������r�[�R����RSSI���L�^����z��
    int beaconRSSIArray[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];	
    // ��������Bluetooth�f�o�C�X�̖��O��RSSI���L�^����z��
    ArrayList<String> btarray = new ArrayList<String>();
    
    // �ʒu�Ǝ�����ێ�����z��
    String locationArray[] = {"time","Latitude","Longitude"};// �����A�ܓx�A�o�x�̏�

    // �n���h���𐶐�
    final Handler handler = new Handler();
    Timer mTimer = null;
    boolean getRSSI = false;
    
    //ID�\���p�ϐ�
    private TextView DrawBeaconList;
    //�ϕ\���p�r�[�R�������t���O
    public int StampEndFlag[][] = new int[TARGET_MAJOR+1][TARGET_MINOR+1];
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// �X���[�v���Ȃ�
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
    	// �ϐ��Ȃǂ�ݒ肷��
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
              
        // Bluetooth�̃T�|�[�g���m���߁A�g�p�����߂�
        boolean suportRet = supportConfirm();
        if(suportRet == false){
        	//�@Bluetooth��T�|�[�g�������ꍇ�Ƀg�[�X�g��\������
			Log("Bluetooth���T�|�[�g����Ă��܂���");
        	finish();
        }else{
	        //Bluetooth��Off�������ꍇ�̓_�C�A���O�\������ON�ɂ���悤����
	        boolean btEnabledRet = OnOffConfirm();
	        if(btEnabledRet == false){
		    //Bluetooth��ON�Ȃ�X���[����
	        }
        }
        // ���̕����A���K�V�[BT�g�����Ƃ������A���ǂ���Ȃ���������
   		/* Bluetooth Adapter */
    	final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    	mBluetoothAdapter = bluetoothManager.getAdapter();

    	// BluetoothAdapter�̃C���X�^���X�擾
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	
// ���ʊ֌W�̏����� �������� ***********************************************************
        // GPS�T�[�r�X�擾
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
// ���ʊ֌W�̏����� �����܂� ***********************************************************

        //�Z���T�[�}�l�[�W���̎擾(1)
        sensorManager=(SensorManager)getSystemService(
            Context.SENSOR_SERVICE);
        //�Z���T�[�̎擾(2)
        List<Sensor> list;
        list=sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (list.size()>0) accelerometer=list.get(0);
        list=sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (list.size()>0) orientation=list.get(0);
        
        // �ȉ��\���֌W-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
        // ready�e�L�X�g��\��������[ueda�f�o�b�O]
        mResultView = (TextView)findViewById(R.id.textView);
        mResultView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mResultView.setText("ready\n");

        // ���ꂼ��̃{�^������������
        setStartButton();	// �X�^�[�g�{�^���֌W�̏����ꎮ
        setStopButton();	// �X�g�b�v�{�^���֌W�̏����ꎮ
    }
    
// �ȉ��A�e�폈�����L�q***********************************************************
    
// �X�^�[�g�{�^���Ɋւ���ꎮ�̏���
	private void setStartButton() {
        Button startButton = (Button)findViewById(R.id.button);
        startButton.setText("start");
        startButton.setOnClickListener(new View.OnClickListener() {
        // start�{�^���N���b�N�C�x���g
            @Override
            public void onClick(View view) {
        //        Intent serviceIntent = new Intent(MainActivity.this, BeaconService.class);
         //       mResultView.append("start\n"); // [ueda�f�o�b�O�������\��
                // �Ď��X�^�[�g
        		/* Bluetooth LE�f�o�C�X�̌��� */
        		mBluetoothAdapter.startLeScan(mLeScanCallback);

        	    // Bluetooth���Ӄf�o�C�X�̌����J�n
        	    mBluetoothAdapter.startDiscovery();
        //        startService(serviceIntent);
                getRSSI = true;
                moniteringRssi();
                mResultView.setBackgroundColor(0x33ff0000);	// �w�i�F��Ԍn�ɕς���
               // setContentView(mResultView);

			    // �t���O���Z�b�g
			    asnflag = 0;
			    jsnflag = 0;
			    
                yaw = pitch = roll = 0;			// ���ʁA�s�b�`�A���[��
            	lowPassX = lowPassY = 0;	// �I�t�Z�b�g
            	lowPassZ = 9.8066;
            	rawAx = rawAy = rawAz = 0;			// ������
                
            }
        });
    }

// �X�g�b�v�{�^���Ɋւ���ꎮ�̏���
	private void setStopButton() {
        Button stopButton = (Button)findViewById(R.id.button2);
        stopButton.setText("stop");
        stopButton.setOnClickListener(new View.OnClickListener() {
        // stop�{�^���N���b�N�C�x���g
            @Override
            public void onClick(View view) {
                mResultView.setBackgroundColor(0x220000ff);	//�w�i�F��n�ɕς���
                mBluetoothAdapter.stopLeScan(mLeScanCallback);	//�X�L��������߂�

		        // Bluetooth�������~
                if (mBluetoothAdapter.isDiscovering()) {
		            	mBluetoothAdapter.cancelDiscovery();
		            	}
                getRSSI = false;
                moniteringRssi();
        		// ��ʂ�������***********************************************************
                DrawBeaconList = (TextView)findViewById(R.id.textView);
                DrawBeaconList.setMovementMethod(ScrollingMovementMethod.getInstance());
               	DrawBeaconList.setText("\n"); 
               	// ��ʏ������@�����܂�***********************************************************
            }
        });
    }
    
// �C���^�[�o���Ńr�[�R����RSSI���擾���鏈��
	private void moniteringRssi() {
        // �^�C�}�[�𐶐�
        Timer mTimer = new Timer(true);
    	if(getRSSI == true){	// �^�C�}�������s���ꍇ
       //     Timer mTimer = new Timer(true);	// ���̈ʒu���ƃX�g�b�v�����Ƃ��ɗ�����
	        // �X�P�W���[����ݒ�
	        mTimer.schedule(new TimerTask() {
	        	public void run() {
	        		handler.post(new Runnable() {
	        			public void run() {
	        				getRSSI();
	        			}
	        		});
	        		}
	        	}, 1000, 1000); // ����N���̒x���Ǝ������w��A
        }else if(getRSSI == false){	// �^�C�}�������s�Ȃ�Ȃ��ꍇ
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

	// Beacon�T�� �d�E���x�̎擾(�C���^�[�o������)*************************************************
	    public void getRSSI(){
			// ���ۂɏ�����������e���L�q
            mBluetoothAdapter.stopLeScan(mLeScanCallback);	//�X�L��������߂�		// -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-�M�����x���擾����@��������

            // ��ʂ�������
            DrawBeaconList = (TextView)findViewById(R.id.textView);
            DrawBeaconList.setMovementMethod(ScrollingMovementMethod.getInstance());
           	DrawBeaconList.setText("\n"); 
           	
           	if(getRSSI == true){	// �{�����s���Ȃ�
           	//����ǉ����������@��������***********************************************************
                    Time time = new Time("Asia/Tokyo");	//�������擾
                    time.setToNow();					//���݂̎�����ݒ�
                    locationArray[0] = time.year + "�N" + (time.month+1) + "��" + time.monthDay + "��  "+ time.hour + "��" + time.minute + "��" + time.second + "�b";

                    // �V�X�e��������\������
                    mResultView.append("�V�X�e�������F"+nowTime+"\n");
                    // �����A�ܓx�A�o�x��\������
                    mResultView.append("�����F"+locationArray[0]+"\t\t�ܓx�F"+locationArray[1]+"  �o�x�F"+locationArray[2]+"\n");
                    // �����x�ƌX����\������
                    mResultView.append( "X��a:"+senvalues[0]+"�@Y��a:"+senvalues[1]+"�@Z��a:"+senvalues[2]+"\n");
                    mResultView.append( "Y:"+senvalues[3]+"\t\t\tyaw�F"+yaw+"\nP:"  +senvalues[4]+"\t\t\tpitch�F"+pitch+"\nR:"  +senvalues[5]+"\t\t\troll�F"+roll+"\n");
                    mResultView.append( "rawAx:"+rawAx+"\n rawAy:"+rawAy+"\n rawAz:"+rawAz+"\n�ȉ����W�ϊ���̒l\nax:"+ax+"mm/s^2\n ay:"+ay+"mm/s^2\n az:"+az+"mm/s^2\n\n");

                    // �������x��\������
                    mResultView.append("dvx:"+dvx+"mm/s\tdvY:"+dvy+"mm/s\tdvz:"+dvz+"mm/s\n");
                    // ���x��\������
                    mResultView.append("vX:"+vx+"mm/s\tvY:"+vy+"mm/s\tvZ:"+vz+"mm/s\n\n");
                    // ����������\������
                    mResultView.append("dx:"+dx+"mm\tdY:"+dy+"mm\tdz:"+dz+"mm\n");
                    // �ړ�������\������
                    mResultView.append("X:"+(int)x+"cm\tY:"+(int)y+"cm\tz:"+(int)z+"cm\n\n");
           	//����ǉ����������@�����܂�***********************************************************

                    // UUID��\��
                    mResultView.append("UUID : "+TARGET_UUID+"\n");
                    // ��ʂ�ID�ƐM�����x��`��     
		            for( int i=0 ; i <= TARGET_MAJOR ; i++ ){
		    	        for( int j=0 ; j <= TARGET_MINOR ; j++ ){
		    	        	mResultView.append("��ID:" +i+ "��ID:" + j+ "���x:"+beaconRSSIArray[i][j]+"\n");
		    	        }
	    	        	mResultView.append("\n");	// ���W���[ID���Ƃɉ��s������
		    	    }
	        	mResultView.append("\n");
	        	//Bluetooth�̃��X�g��\��
	            //ArrayList��1�������o����ʂɕ\������
	            Iterator<String> it = btarray.iterator();
	            while (it.hasNext()) {
	            	mResultView.append(it.next());
	            }
        		mBluetoothAdapter.startLeScan(mLeScanCallback);	//�X�L�����ĊJ
		    }else if(getRSSI == false){
		    	mResultView.append("stop\n");	// stop��\��
		    }
			// -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-�M�����x���擾����@�����܂�
	    }

	// �f�B�o�C�X���ARSSI���擾���AUUID�AmajorID�AminorID�𐶐�����
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
				// �ϐ���������
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
		// 10�i����16�i���֕ϊ�����B
		private String IntToHex2(int i) {
		char hex_2[] = {Character.forDigit((i>>4) & 0x0f,16),Character.forDigit(i&0x0f, 16)};
		String hex_2_str = new String(hex_2);
		return hex_2_str.toUpperCase();
	}
		
		// Bluetooth�T�|�[�g�̗L�����菈��***********************************************************  
		// Bluetooth�̎g�p�Ɋւ��鏈��=========================================================
		// Bluetooth�̗L�����m���߂鏈��**********************************************
		    private boolean supportConfirm(){
		        //Bluetooth���T�|�[�g����Ă��邩�ۂ����m���߂�
		        BluetoothAdapter Bt = BluetoothAdapter.getDefaultAdapter();
		        if(Bt.equals(null)){
		        	return false;
		        }
		        mBluetoothAdapter = Bt;
		        return true;
		    }

		 // Bluetooth��ON/OFF���菈��***********************************************************
		 // Bluetooth��ON/OFF���m���߂鏈��*******************************************
		    private boolean OnOffConfirm(){
		    	//Bluetooth��On�ɂȂ��Ă��邩�ǂ������m���߂�
		    	boolean btEnable = mBluetoothAdapter.isEnabled();
		    	if(btEnable == true){
		    		return true;
		    	}
		    	//On�łȂ��ꍇ�AOn�ɂ��邽�߂̃_�C�A���O��\������
		    	Intent BtOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    	startActivityForResult(BtOn, REQUEST_ENABLE_BLUETOOTH);
		    	return false;
		    }

		 // Bluetooth�g�p�v������***********************************************************
		 // Bluetooth�̎g�p�������߂�m���߂鏈��**************************************
		    @Override
		    protected void onActivityResult(int requestCode, int ResultCode, Intent date){
		    	if(requestCode == REQUEST_ENABLE_BLUETOOTH){
		    		if(ResultCode == Activity.RESULT_OK){
		    			//Bluetooth��On�������ꍇ�̏���
		    			Log("Bluetooth��ON�ɂȂ�܂���");
		        		
		    		}else{
		                error("Bluetooth�𗘗p�o���܂���");
		    		}
		    	}
		    }

//�@�Z���T�@��������***********************************************************		

			@Override
		    //�Z���T�[���X�i�[�̏���(5)
		    public void onSensorChanged(SensorEvent event) {
		        //�����x�̎擾0
		        if (event.sensor==accelerometer) {
		            senvalues[0]=event.values[0];	// �擾�����l��z��ɕۑ�
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
					//�I�t�Z�b�g
					rawAx -= offAx;
					rawAy -= offAy;
					rawAz -= offAz;
					Log.d("Count","�����x");
		        }
		        //�����̎擾
		        if (event.sensor==orientation) {
		            senvalues[3]=event.values[0];	// �擾�����l��z��ɕۑ�
		            senvalues[4]=event.values[1];
		            senvalues[5]=event.values[2];
		            
		            if(jsnflag ==0){
					    inityaw =  senvalues[3];
					    initpitch =  senvalues[4];
					    initroll =  senvalues[5];
		            	// �t���O���Z�b�g
					    jsnflag = 1;
		            }
					yaw = senvalues[3] -inityaw;
					pitch = senvalues[4] -initpitch;
					roll = senvalues[5] -initroll;

					double nPitchRad = Math.toRadians(-pitch); // �e�ϐ������W�A���֕ϊ��An means negative
					double nRollRad = Math.toRadians(-roll);
					double nyawRad = Math.toRadians(-yaw);
		            
					Log.d("Count","��]");
			// ***********************************************************
					// �s�b�`
					double sinNPitch = Math.sin(nPitchRad);
					double cosNPitch = Math.cos(nPitchRad);
					// ���[��
					double sinNRoll = Math.sin(nRollRad);
					double cosNRoll = Math.cos(nRollRad);
					// ���ʁi�A�W�}�X�j
					double sinNyaw = Math.sin(nyawRad);
					double cosNyaw = Math.cos(nyawRad);
		        
					// �ꎞ�ޔ�ϐ�
					double bx, by; 
					bx = rawAx * cosNRoll + rawAz * sinNRoll;
					by = rawAx * sinNPitch * sinNRoll + rawAy * cosNPitch - rawAz * sinNPitch * cosNRoll;
					// �[�������̍��W�������Ԃł̍��W�֕ϊ����A���[�p�X�t�B���^��ʂ�
					ax += ((bx * cosNyaw - by * sinNyaw)*1000 - ax )*k ;
					ay += ((bx * sinNyaw + by * cosNyaw)*1000 - ay )*k ;
					az += ((-rawAx * cosNPitch * sinNRoll + rawAy * sinNPitch * cosNRoll + rawAz * cosNPitch * cosNRoll)*1000 - az )*k ;
			}

			   // ax, ay, az �����܂������
			   nowTime = System.currentTimeMillis();	//�V�X�e���̌��ݎ������~���b�ilong�^�̐��l�j�Ŏ擾
			   long interval = nowTime - oldTime; // interval���~���b����b�֕ϊ�
			   oldTime = nowTime;
		        Log.d("Sensor", interval+"ms X�������x:"+senvalues[0]+"�@Y�������x:"+senvalues[1]+"�@Z�������x:"+senvalues[2]+"\n" +
		        		"����:"+senvalues[3]+"�@�s�b�`:"  +senvalues[4]+"�@���[��:"  +senvalues[5]);
			   
	           if(asnflag == 1){
	        	   // ***********************************************************
	        	   // �����x��ϕ�����
	        	   dvx = (int) (ax * interval/1000); // ���x[mm/s] �̕ω����ɂ���
	        	   dvy = (int) (ay * interval/1000);
	        	   dvz = (int) (az * interval/1000);
	        	   vx += dvx;	// ���x�ɕω����𑫂�����
	        	   vy += dvy;
	        	   vz += dvz;
	        	   
	        	   
	        	   dx = (int) (vx * interval/1000);	// ����[mm] �̕ω����ɂ���
	        	   dy = (int) (vy * interval/1000);
	        	   dz = (int) (vz * interval/1000);
	        	   x += dx/10;						// ����[cm] �ɂ���
	        	   y += dy/10;
	        	   z += dz/10;
	        	   // ***********************************************************
	            }else{
	            	vx = vy = vz = 0;					//�@���x������
	            	x = y = z = 0 ;						//�@�ړ�����������
	            	// �t���O���Z�b�g
				    asnflag = 1;
	            }
			}

		    //���x�ύX�C�x���g�̏���
		    public void onAccuracyChanged(Sensor sensor,int accuracy) {
		    }

//�@�Z���T�@�����܂�***********************************************************	
// ���ʁ@�������� ***********************************************************
		    @Override
		    protected void onPause() {
		        // TODO Auto-generated method stub
		        if(manager != null) {
		            manager.removeUpdates(this);
		        }
		        super.onPause();	//������Ȃ��ƃA�v����������
		    }

		    @Override
		    protected void onResume() {
		        // TODO Auto-generated method stub
		        if(manager != null) {
		            manager.requestLocationUpdates(LocationManager.
		            		NETWORK_PROVIDER,	//�l�b�g���[�N���ʂȂ炱��
		            	//	GPS_PROVIDER		//GPS���ʂȂ炱��
		            		0, 0, this);
		        }
		        super.onResume();	//������Ȃ��ƃA�v����������
		        
		        //�Z���T�[�̏����̊J�n(3)
		        if (accelerometer!=null) {
		            sensorManager.registerListener(this,accelerometer,
		                SensorManager.SENSOR_DELAY_GAME);
		        }
		        if (orientation!=null) {
		            sensorManager.registerListener(this,orientation,
		                SensorManager.SENSOR_DELAY_GAME);
		        }
		        
		    }
		    // ���ʂ����s�����ƌĂ΂��
		    @Override
		    public void onLocationChanged(Location location) {
    			Log("���ʁI");	//***********************************************************�e�X�g�|�C���g�i���ʎ��Ƀg�[�X�g�o�́j
		        // TODO Auto-generated method stub
		    	locationArray[1] = "" + location.getLatitude();	//�ܓx���擾���z��ɓ����
		        locationArray[2] = "" + location.getLongitude();//�o�x���擾���z��ɓ����
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
		      //�Z���T�[�̏����̒�~
		      sensorManager.unregisterListener(this);
		    }

//�@���ʁ@�����܂�***********************************************************	
// Log���g�[�X�g�ŕ\�����鏈��***********************************************************    
// �\���Ɋւ��鏈��=====================================================================
//���O���g�[�X�g�ŕ\��**********************************************************
    public void Log(String string){
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();    	
    }
    
 // �G���[�\������***********************************************************        
//�G���[���b�Z�[�W���A���[�g�ŕ\��**************************************************
    private void error(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder (this);
        builder.setTitle("�G���[");
        builder.setMessage(msg);
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
/* ���ʂ̂��Ƃ͂�����
 * http://seesaawiki.jp/w/moonlight_aska/d/%B0%CC%C3%D6%BE%F0%CA%F3%A4%F2%BC%E8%C6%C0%A4%B9%A4%EB
 */ 
//-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-�\�[�X�����܂�-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-
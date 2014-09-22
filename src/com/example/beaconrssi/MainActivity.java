package com.example.beaconrssi;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;	//BT�ł�BLE�ł����̈�s�ŗǂ�
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
//import android.graphics.Color;
//import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.*;

// import android.util.Log;

// �}�j�t�F�X�g�t�@�C��(�`Manifest.xml)��GPS�̃p�[�~�b�V������ǉ����邱�ƁI�I
public class MainActivity extends Activity implements LocationListener {
	private LocationManager manager = null; // GPS���ʂɗp����
	// private Musictest view;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
	static BluetoothAdapter mBluetoothAdapter;
    private TextView mResultView;
    // ���C�u�������g�p���Ȃ�Bluetooth�֌W�̕ϐ�
	int BCONmajor;
	int BCONminor;
	String BCONuuid;
	    
    //�Ď��ΏۂƂȂ�r�[�R���̒l
    private static final String TARGET_UUID = "������UUID���L��";
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
           		
                    // �����A�ܓx�A�o�x��\������
                    mResultView.append("�����F"+locationArray[0]+"\n�ܓx�F"+locationArray[1]+"  �o�x�F"+locationArray[2]+"\n\n");
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
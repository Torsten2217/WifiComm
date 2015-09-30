package com.app.wificommunicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
//import com.app.UATController.R;


public class MainActivity extends Activity implements View.OnClickListener {

	private View btnSubmit;
	private View btnVFR;
	private ToggleButton  btnSelfAddr;
	private View btnExit;
	
	private ToggleButton btnOn;
	private ToggleButton btnOff;
	private ToggleButton btnAlt;
	private ToggleButton btnStdby;

	private TextView txtN_Number;
	private TextView txtSquawk;
	private TextView txtICAO_Address;

	private String oldstrN_Number = "";
	private String oldstrSquawk = "";
	private String oldstrICAO_Address = "";
	
	public boolean bSendFlag = false;

	
	public static boolean bPacketSend = false;
	public static boolean bRefreshFlag = true;
	public static byte [] dataOfScreen = new byte[256];
	public static boolean bShutDownFlag = false;
	
	
	private String callSign = "N";
	public boolean IDENT = true;
	
	WifiAdmin wifiAdmin ;
	
	udpReceiveAndSend udpRecv = null;
	
	private BufferedReader reader = null;
	private InputStreamReader inStreamRead = null; 
	
	
	
	public Handler handler_for_udpReceiveAndtcpSend = new Handler() {
    	byte[] strUDPData = new byte[22];
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 0x404) {
            	if(!isWifiConnect()){
            		String str = "Could not connect to UAT, please check Wifi connection";
            		showMessage(str);
            	}
            	else
            	{
            		wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo("NavWorxAP".trim(), null, 1));
            	}
            }
            else if (msg.what == 0x200 ){
            	strUDPData = (byte[])msg.obj;
            	
            	if(strUDPData != null)
            	{

            		if(MainActivity.bRefreshFlag)
            		{
            			UpdateValuesofCtrls(strUDPData);
            			MainActivity.bRefreshFlag = false;
            		}
            	}
            }
        }
    };
	
	public enum Transmiter {
	    OFF (0), 
	    STDBY (1),
	    ON (2), 
	    ALT (3); 
	    
	    private final int val;
	    
	    Transmiter(int intval){
	    	this.val = intval;
	    }
		public int getVal() {
			return val;
		}
		
		 private static Transmiter[] values = null;
		    public static Transmiter fromInt(int i) {
		        if(Transmiter.values == null) {
		        	Transmiter.values = Transmiter.values();
		        }
		        return Transmiter.values[i];
		    }
	}
	
	public enum EmergencyEnum {
		None(0),
		Gen(1),
		LifMed(2),
		Min(3),
		NoComm(4),
		Unlaw(5),
		Down(6);
		
	    private final int val;
	    
	    EmergencyEnum(int intval){
	    	this.val = intval;
	    }
		public int getVal() {
			return val;
		}
		
		 private static EmergencyEnum[] values = null;
		    public static EmergencyEnum fromInt(int i) {
		        if(EmergencyEnum.values == null) {
		        	EmergencyEnum.values = EmergencyEnum.values();
		        }
		        return EmergencyEnum.values[i];
		    }
	}
	
	public Transmiter tranFlag; // Transmitter Flag(on, off, alt, )
	public EmergencyEnum emergencyEnumFlag; // Transmitter Flag(on, off, alt, )
	
	
	

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
    // Assign the click event to every Button.
    	btnSubmit = findViewById(R.id.but_submit);
    	btnVFR = findViewById(R.id.but_vfr);
    	btnSelfAddr = (ToggleButton)findViewById(R.id.btn_SelfAddr);
    	btnExit = findViewById(R.id.but_exit);
    	
    	btnOn = (ToggleButton)findViewById(R.id.but_selfaddron);
    	btnOff = (ToggleButton)findViewById(R.id.but_selfaddroff);
    	btnAlt = (ToggleButton)findViewById(R.id.but_alt);
    	btnStdby = (ToggleButton)findViewById(R.id.but_stdby);
    	
    	txtN_Number = (TextView)findViewById(R.id.edit_number);
    	txtN_Number.setText("N35195");
    	txtSquawk = (TextView)findViewById(R.id.edit_squawk);
    	txtSquawk.setText("1200");
    	txtICAO_Address = (TextView)findViewById(R.id.edit_icao);
    	txtICAO_Address.setText("50766065");
 
    	
    	btnSubmit.setOnClickListener(this);
    	btnVFR.setOnClickListener(this);
    	btnSelfAddr.setOnClickListener(this);
    	btnExit.setOnClickListener(this);;
    	
    	btnOn.setOnClickListener(this);;
    	btnOff.setOnClickListener(this);;
    	btnAlt.setOnClickListener(this);;
    	btnStdby.setOnClickListener(this);
    	
        
        this.tranFlag = Transmiter.ALT;
    	btnAlt.setChecked(true);
        this.emergencyEnumFlag = EmergencyEnum.None;
        
		btnSelfAddr.setChecked(true);
		txtSquawk.setEnabled(true);
		
		
		MainActivity.bPacketSend = false;
		MainActivity.bRefreshFlag = true;
		MainActivity.bShutDownFlag = false;

    	// Wifi Process part
		wifiAdmin= new WifiAdmin(this);
        wifiAdmin.openWifi();
        
        onRunWifiCommunicate();
        
        // Search Part
        

		  onTextChangeProc(1);

   }

	private void onTextChangeProc(final int minLength){

		final EditText myEdit = (EditText)txtN_Number;
	

		myEdit.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) || (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                if (myEdit.getText().length() <= minLength) {
	                	//myEdit.setText("N");
			            return true;
	                }
	            }
	            return false;
			}
	    });
		
		txtN_Number.addTextChangedListener(new TextWatcher() {

		    public void onTextChanged(CharSequence s, int start, int before,
		            int count) {
		        if(!s.equals("") )
		        {
		        	String strSearch = txtN_Number.getText().toString();
		        	String newStr = "";
		        	if(strSearch.length() > 1 )
		        	{
		        		if(strSearch.length() == 2)
		        		{
		        			newStr = strSearch + "0";
		        		}
		        		else
		        			newStr = strSearch;
		        			
		        		String strTemp = "";
		        		int intLimit = 0;
		        		String strFileName = "";

		        		int intValue = 0;
		        		try {
			        		strTemp = strSearch.substring(1,2);
		        			intValue = Integer.valueOf(strTemp);
		        			strTemp = newStr.substring(1,3);
			        		intLimit = 0; Integer.valueOf(strTemp);
			        		
		        			intLimit = Integer.valueOf(strTemp);
			        		strFileName = onSwitchBlog(intLimit, intValue); 
		        		}
		        		catch(NumberFormatException e){
		        			if(intValue == 0)
		        				return;
		        			
		    				switch(intValue){
							case 1:
								strFileName = "ICAO1.txt";
								break;
							case 2:
								strFileName = "ICAO2.txt";
								break;
							case 3:
								strFileName = "ICAO3.txt";
								break;
							case 4:
								strFileName = "ICAO4.txt";
								break;
							case 5:
								strFileName = "ICAO5.txt";
								break;
							case 6:
								strFileName = "ICAO6.txt";
								break;
							case 7:
								strFileName = "ICAO7.txt";
								break;
							case 8:
								strFileName = "ICAO8.txt";
								break;
							case 9:
								strFileName = "ICAO9.txt";
								break;
							default:
								strFileName = "";
								break;
		    				}
		        		}

		        		String str = onSearchSquawk(((strSearch).substring(1) + ","), strFileName);
			        	if(str.length() > 0)
			        	{
			        		txtICAO_Address.setText(str);
			        	}
		        	}
		        }
		    }
			private String onSwitchBlog(int intLimit, int intValue) {
				String strFileName = "";
				switch(intValue){
					case 1:
						if(intLimit >= 15 && intLimit <=19)
						{
							strFileName = "ICAO15.txt";
						}
						else
							strFileName = "ICAO1.txt";
						break;
					case 2:
						if(intLimit >= 25 && intLimit <=29)
						{
							strFileName = "ICAO25.txt";
						}
						else
							strFileName = "ICAO2.txt";
						break;
					case 3:
						if(intLimit >= 35 && intLimit <=39)
						{
							strFileName = "ICAO35.txt";
						}
						else
							strFileName = "ICAO3.txt";
						break;
					case 4:
						if(intLimit >= 45 && intLimit <=49)
						{
							strFileName = "ICAO45.txt";
						}
						else
							strFileName = "ICAO4.txt";
						break;
					case 5:
						if(intLimit >= 55 && intLimit <=59)
						{
							strFileName = "ICAO55.txt";
						}
						else
							strFileName = "ICAO5.txt";
						break;
					case 6:
						if(intLimit >= 65 && intLimit <=69)
						{
							strFileName = "ICAO65.txt";
						}
						else
							strFileName = "ICAO6.txt";
						break;
					case 7:
						if(intLimit >= 75 && intLimit <=79)
						{
							strFileName = "ICAO75.txt";
						}
						else
							strFileName = "ICAO7.txt";
						break;
					case 8:
						if(intLimit >= 85 && intLimit <=89)
						{
							strFileName = "ICAO85.txt";
						}
						else
							strFileName = "ICAO8.txt";
						break;
					case 9:
						if(intLimit >= 95 && intLimit <=99)
						{
							strFileName = "ICAO95.txt";
						}
						else
							strFileName = "ICAO9.txt";
						break;
					default:
						strFileName = "";
						break;
				}
				return strFileName;
			}
		    public void beforeTextChanged(CharSequence s, int start, int count, int after)
		    {
			}
			@Override
			public void afterTextChanged(Editable arg) {
				;
			}
		});
	}
	
	
	private String onSearchSquawk(String strIndex, String strFile)
	{
		String retStr = "";
		if(strIndex == null || strIndex.length() == 0 || strFile == "")
			return retStr;
		try {
		    // do reading, usually loop until end of file reading
			inStreamRead = new InputStreamReader( getAssets().open(strFile));
			
			reader = new BufferedReader(inStreamRead);
			
			String mLine = reader.readLine();
		    while (mLine != null) {
    			int iIndex = mLine.toUpperCase().indexOf(strIndex.toUpperCase());
				if(iIndex == 0)
				{
					retStr = mLine.substring(strIndex.length());
	    			retStr.trim();
	    			break;
				}
	    		mLine = reader.readLine();
		    }
		} catch (IOException e) {
			retStr = "";
		} finally{
			
		    if (reader != null) {
		         try {
		             reader.close();
		         } catch (IOException e) {
		             //log the exception
		         }
		    }	
		}
		return retStr;
	}
	// Click Event process part
	@Override
	public void onClick(View v){
		switch(v.getId()){
		case R.id.but_submit:
			onSubmitProc();
			break;
		case R.id.but_vfr:
			onVFRProc();
			break;
		case R.id.but_exit:
			onExitProc();
			break;
		case R.id.but_selfaddron:
			onOnProc();
			break;
		case R.id.but_selfaddroff:
			onOffProc();
			break;
		case R.id.but_alt:
			onAltProc();
			break;
		case R.id.but_stdby:
			onStdbyProc();
			break;
		case R.id.btn_SelfAddr:
			onSelfAddrProc();
			break;
		default:
			break;
		}
	}
	@Override
    protected void onDestroy(){
        super.onDestroy();
   
        MainActivity.bShutDownFlag = true; 
    }
	private void showMessage(String str){
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(getApplicationContext(), str, duration);
		toast.show();
	}

	// Define the function of Submit button
	private void onSubmitProc()
	{
//		showMessage("onSubmitProc");
		if(isSendDataToUAT())
		{
			MainActivity.dataOfScreen = makeScreenDataToBytes();
			MainActivity.bPacketSend = true;
		}
	}
	private void onRunWifiCommunicate()
	{
		wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo("NavWorxAP".trim(), null, 1));
//		wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo("FAST_B4BA".trim(), "sec12345".trim(),3));
		
        udpRecv = new udpReceiveAndSend(handler_for_udpReceiveAndtcpSend);
        if(udpRecv != null)
        	udpRecv.start();
	}
	// Define the function of VFR button
	private void onVFRProc()
	{
//		showMessage("onVFRProc");
		txtSquawk.setText("1200");
	}
	// Define the function of Exit button
	private void onExitProc()
	{
//		showMessage("onExitProc");
		MainActivity.bShutDownFlag = true;
		finish();
		moveTaskToBack(true);
	}

	// Define the function of Exit button
	private void onOffProc()
	{
//		showMessage("onOffProc");
		if(btnOff.isChecked())
		{
			setOnOff(0);
		}
		if(tranFlag != Transmiter.OFF)
		{
			bSendFlag = true;
			tranFlag = Transmiter.OFF;
		}
	}
	// Define the function of Exit button
	private void onStdbyProc()
	{
//		showMessage("onStubyProc");
		if(btnStdby.isChecked())
		{
			setOnOff(1);
		}

		if(tranFlag != Transmiter.STDBY)
		{
			bSendFlag = true;
			tranFlag = Transmiter.STDBY;
		}		
	}
	// Define the function of Exit button
	private void onOnProc()
	{
//		showMessage("onOnProc");
		
		if(btnOn.isChecked())
		{
			setOnOff(2);
		}

		if(tranFlag != Transmiter.ON)
		{
			bSendFlag = true;
	        tranFlag = Transmiter.ON;
	    }
	}	
	// Define the function of Exit button
	private void onAltProc()
	{
//		showMessage("onAltProc");
		if(btnAlt.isChecked())
		{
			setOnOff(3);
		}

		if(tranFlag != Transmiter.ALT)
		{
			bSendFlag = true;
			tranFlag = Transmiter.ALT;
		}
	}
	private void setOnOff(int intSel)
	{
		btnOff.setChecked(true);
    	btnStdby.setChecked(true);
		btnOn.setChecked(true);
    	btnAlt.setChecked(true);
    	
		switch(intSel){
		case 0:
	    	btnStdby.setChecked(false);
	    	btnOn.setChecked(false);
	    	btnAlt.setChecked(false);
			break;
		case 1:
	    	btnOff.setChecked(false);
			btnOn.setChecked(false);
	    	btnAlt.setChecked(false);
			break;
		case 2:
			btnOff.setChecked(false);
	    	btnStdby.setChecked(false);
	    	btnAlt.setChecked(false);
			break;
		case 3:
	    	btnOff.setChecked(false);
	    	btnStdby.setChecked(false);
		  	btnOn.setChecked(false);
			break;
		default:
			break;
		}
	}
	
private void onSelfAddrProc()
{
//		showMessage("onSelfAddrProc");
	if(btnSelfAddr.isChecked())
	{
		txtSquawk.setEnabled(false);
		txtSquawk.setText("1200");
	}
	else
	{
		txtSquawk.setEnabled(true);
	}
	bSendFlag = true;
}	
public boolean isSendDataToUAT(){
	boolean bRes = false;
	
	String newStrN_Number = "";
	String newStrSquawk = "";
	String newStrICAO_Address = "";

	if(bSendFlag)
	{
		return true;
	}
	
	newStrN_Number = txtN_Number.getText().toString();
	newStrSquawk = txtSquawk.getText().toString();
	newStrICAO_Address = txtICAO_Address.getText().toString();
	
	if(!newStrN_Number.equals(oldstrN_Number))
	{
		return true;
	}
	if(!newStrSquawk.equals(oldstrSquawk))
	{
		return true;
	}
	if(!newStrICAO_Address.equals(oldstrICAO_Address))
	{
		return true;
	}
	return bRes;
}
// Read data from UDP data and set the values of controls in Screen
    public  void UpdateValuesofCtrls(byte[] data)
    {
        int squawkValue = 0;
        int ident = 0;
        int address = 0;
        boolean selfaddress = false;
        String callSign;
        String squawk;
        String icasAddress;
        Transmiter transmitType;

        squawkValue = ((int)(Common.unsignedToBytes(data[1]))) << 16;
        squawkValue += ((int)(Common.unsignedToBytes(data[2]))) << 8;
        squawkValue += ((int)(Common.unsignedToBytes(data[3])));

        squawk = FromOctal(squawkValue);
        if((0x01 & data[7]) != 0x00)
        {
        	selfaddress = true;
        }

        ident = (int)(0x01 & data[8]);

        callSign = new String(Arrays.copyOfRange(data, 9, 16));
        address = ((int)(Common.unsignedToBytes(data[17]))) << 16;
        address += ((int)(Common.unsignedToBytes(data[18]))) << 8;
        address += ((int)(Common.unsignedToBytes(data[19])));
        

        icasAddress = FromOctal(address);
        transmitType = Transmiter.fromInt(data[20] & 0x03);
        
        emergencyEnumFlag = EmergencyEnum.fromInt(data[21] & 0x07);
        
        txtN_Number.setText(callSign);
       	txtSquawk.setText(squawk);
       	btnSelfAddr.setChecked(selfaddress);
       	txtICAO_Address.setText(icasAddress);
       	tranFlag = transmitType;
       	
       	this.setOnOff(tranFlag.val);
       	
       	// Save previous values
       	oldstrN_Number = callSign;
    	oldstrSquawk = squawk;
    	oldstrICAO_Address = icasAddress;      	
 
    }
    // Make UDP data packet form data of the screen

    public byte[] makeScreenDataToBytes()
    {
        byte[] data = new byte[22];
        int idx = 9;
        //ICAO address is octal, so convert from decimal to octal
        int icaoAddr = 0;


        byte MessageID = (byte)0x82; //MessageIdEnum.PilotInput;
//        byte MessageID = (byte)0x87; //MessageIdEnum.PilotInput;

        data[0] = MessageID;
       
        //squawk (flight id) is octal, so convert from octal to decimal
        int squawkValue = Integer.valueOf(txtSquawk.getText().toString(), 8); 
        data[1] = (byte)((0xFF0000 & squawkValue) >> 16);
        data[2] = (byte)((0xFF00 & squawkValue) >> 8);
        data[3] = (byte)(0xFF & squawkValue);
        data[4] = 0; //(byte)0xFF;
        data[5] = 0;
        data[6] = 0;
        boolean blStatus = btnSelfAddr.isChecked();
        int intSelfAddr = (blStatus ? 0x01 : 0x00);
        
        data[7] = (byte)(0x01 & intSelfAddr);
        data[8] = (byte) (0x01 & (IDENT ? 0x01 : 0x00));
        String str = txtN_Number.getText().toString(); 
        
        callSign = String.format("%1$-8s", str);
        
        if (callSign.length() != 8)
        {
            return null;
        }
        
        byte [] bytes = callSign.getBytes();
        for (int i = 0; i < bytes.length; i++)
        {
            data[idx] = bytes[i];
            idx++;
        }
        
        icaoAddr =  Integer.valueOf(txtICAO_Address.getText().toString(), 8);
  
        data[17] = (byte)((0xFF0000 & icaoAddr) >> 16);
        data[18] = (byte)((0xFF00 & icaoAddr) >> 8);
        data[19] = (byte)(0xFF & icaoAddr);
        data[20] = (byte)(0x03 & (int)tranFlag.val);
        data[21] = (byte)(0x07 & (int)emergencyEnumFlag.val);

        //_isDirty = false;

        //Reset IDENT value in case it was set.
        IDENT = true;

        return data;
    }
    
    // Convert from Integer to Octal
    private String FromOctal(int val)
    {
    	return Integer.toOctalString(val);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
  //Check if current network is Wifi connect.	
   	private boolean isWifiConnect(){
		try{
			ConnectivityManager conManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			return mWifi.isConnected();
		}catch(Exception e){
			return false;
		}
	}
   	public void onHelpShow(View v)
   	{
   		final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
		
		// Setting Dialog Title
		alertDialog.setTitle("UAT Communicator Help");
		
		// Setting Dialog Message
		alertDialog.setMessage(" UAT Communicator is designed to work with the Skyvision Salus-3," + 
			    "NavWorx PADS-B, and Other NavWorx UAT's with Wifi Connections."+ "\n"+ "\n" +" Make sure your device is connected to the UAT via Wifi. "+ "\n" + "\n" + 
			    "UAT Communicator begins by connecting with the UAT and reads the Values in the UAT. It displays the Values on the screen. " +
			    "You can change any of the values like N number, Squawk Code, Transmitter, etc. " +
			    "After changing the values touch the Submit button on the screen to send the Values to the UAT. "+ "\n" +
			    "UAT communicator will update the Values on the UAT and read from the UAT again and display the read Values. " +
			    "If everything is as you want simply touch the exit button to close the app. "+ "\n" +
			    "Note:  Touching the VFR button will set the squawk code to 1200. "+ "\n" +
			    "Moving the Self Assigned slide switch to the right will set the transponder code to 1200 and send a random ICAO number when the UAT transmits to the ground stations. " +
			    "On the Salus-3 and PADS-B the Self Assigned LED will illuminate if Self Assigned is set to on. "+ "\n" +
			    "The Transmitter options are: Off, On, ALT (Mode C), and Stby (Standby). "+ "\n" +
			    "The default value is Mode C but the settings in the UAT will override this when the UAT data is read on initial Startup," +
			    " Therefore if the UAT was set to Off on startup it should be set to Alt to begin transmitting. "+ "\n" +
			    "The ICAO number is automatically provided via a lookup when you enter the N Number.");
		
		// Setting Icon to Dialog
		alertDialog.setIcon(R.drawable.ic_uat);
		
		// Setting OK Button
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	alertDialog.cancel();
		        }
		});
		
		// Showing Alert Message
		alertDialog.show();
   	}
}

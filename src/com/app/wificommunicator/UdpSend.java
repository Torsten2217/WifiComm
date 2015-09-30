package com.app.wificommunicator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;

import android.os.Handler;


public class UdpSend extends Thread {

	Handler handler;
	int sendPort = 43211;
	String strServerIP = "169.254.1.1";
//	String strServerIP = "192.168.2.107";
	byte[] data = new byte[256];
	
	public UdpSend(Handler handler, byte [] dataOfScreen) {
        this.handler = handler;
        this.data = Common.Packetize(dataOfScreen);
 
    }
    
    @Override
    public void run() {
        
     	if(data == null)
    		return;
    	
        DatagramSocket sender = null;

        DatagramPacket packet = null;
        InetAddress serverIP = null;

            try {
              sender = new DatagramSocket();
              serverIP = InetAddress.getByName(strServerIP);
              packet = new DatagramPacket(data,data.length,serverIP,sendPort);

              sender.send(packet);
              sender.close();
          } catch(IOException e) {
              e.printStackTrace();
          } finally{
        	  if(sender != null)
        	  {
        		  sender.close();
        	  }
          }

        }	
}

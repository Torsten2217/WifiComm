package com.app.wificommunicator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class udpReceiveAndSend extends Thread {
	DatagramSocket socket = null;
	DatagramPacket dp;
	Handler handler = new Handler();
    int sendPort;
    int recvPort;
    private byte [] information;
	private String SERVERIP = "169.254.1.1";
//    private String SERVERIP = "192.168.2.103";
    MulticastSocket ms = null;
    DatagramPacket packet;
    
    public byte []  sendData;
    
	private static final String TAG = "ServerHeartService";
	
    public udpReceiveAndSend(Handler handler) {
        this.handler = handler;
        sendPort = recvPort = 43211;
    }
    
    @Override
    public void run() {
    	
    	InetAddress groupAddress = null;
        try {
            groupAddress = InetAddress.getByName("224.0.0.1");

            ms = new MulticastSocket(43211);
            ms.joinGroup(groupAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while(true){
        	try{
        		if(MainActivity.bShutDownFlag)
        			break;
        		
        		GetMsg();

        		if(MainActivity.bShutDownFlag)
        			break;
				Thread.sleep(100);
        	}
        	catch (InterruptedException e) {
        		e.printStackTrace();
			}
        }

        try {
		    if (ms != null) {
		    	if(groupAddress != null)
		    		ms.leaveGroup(groupAddress);
			    ms.close();
		    }
		 }
		 catch (  IOException ex) {
			 ex.printStackTrace();
		 }
    }
    public void SendMsg(String str) throws IOException{
    	
        try {
            Thread.sleep(1000);
            socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVERIP);

            byte data[] = str.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, sendPort);
            socket.send(packet);
            Log.d(TAG, "Server send success");
        } catch (SocketException e) {
            Log.d(TAG, "Server is not found");
            e.printStackTrace();
        } catch (UnknownHostException e) {
            Log.d(TAG, "Server connect failure");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "IOException");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.d(TAG, "InterruptedException");
            e.printStackTrace();
        } finally {
    	    if(socket != null)
               socket.close();
        }
    }
	public byte[] makeByte()
	{
		String str = "7E 00 81 81 FE 17 00 00 FE F5 7E 7E 83 01 03 03 E9 5D 7E 7E 86 01 01 07 05 E7 68 19 00 C9 3B C7 00 5E 03 00 00 2D 00 00 00 00 00 00 00 00 00 00 00 DF 07 09 03 13 36 25 00 00 00 00 00 00 00 00 00 00 00 08 09 03 00 00 00 00 00 00 00 00 00 60 00 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 50 C3 00 00 00 01 00 25 95 7E 7E 0B 00 AC 7F FF 68 FB 7E 7E 0A 01 2C 77 E0 19 68 E7 C7 3B C9 04 A1 89 00 00 00 20 01 4E 33 35 31 39 35 20 20 00 E8 94 7E 7E 8C 20 08 00 01 2C 77 E0 32 D1 CF 8E 77 93 04 B8 80 05 20 0A 38 00 8C 78 7E 7E 87 00 02 80 FF 00 00 01 00 4E 33 35 31 39 35 00 00 A3 EC 35 03 00 84 7C 7E 7E E6 00 4E 33 35 31 39 35 20 20 00 00 00 01 04 03 00 01 00 03 00 00 23 14 00 01 01 00 00 00 00 00 01 04 01 00 00 4B 00 5A 00 00 05 80 02 00 00 35 EC A3 00 1F 02 00 00 6E 19 00 00 00 00 01 00 00 00 00 00 00 00 00 0D E7 7E";
		
		String strArray[] = str.split(" ");
		byte packet [] = new byte[strArray.length];
		
		for(int i = 0; i < packet.length; i++)
		{
			packet[i] = (byte)Integer.parseInt(strArray[i], 16);
					
		}
		return packet;
	}
    // Get Packet Receive And Send
	public void GetMsg(){
        try {
        	//socket = new DatagramSocket(recvPort);
            Log.d(TAG, "Server connect !");
    		information = new byte[26];
            byte data[] = new byte[256];
            byte realData[] = new byte[0];
            try {
            	packet = new DatagramPacket(data, data.length);
            	ms.setSoTimeout(10000);
                if (ms != null)
                    ms.receive(packet);
            } catch (Exception e) {
             	 Message msg;
            	 msg = new Message();
            	 msg.what = 0x404;
            	 msg.obj = "Check Wifi status";
            	 if(information != null)
            	 {
            		 handler.sendMessage(msg);
            	 }               
            	e.printStackTrace();
            }
            
            
            if (packet.getAddress() != null) {
                final String guest_ip = packet.getAddress().toString();

                String host_ip = getLocalHostIp();

                System.out.println("host_ip:  --------------------  " + host_ip);
                System.out.println("guest_ip: --------------------  " + guest_ip.substring(1));

                 if( (!host_ip.equals(""))  && host_ip.equals(guest_ip.substring(1)) ) {
                    return;
              }
             if(MainActivity.bRefreshFlag){
                 realData = Common.getRealData(data);
                 
                 if(realData != null){
                	 information = Common.Depacketize(realData);
                	 Message msg;
                	 msg = new Message();
                	 msg.what = 0x200;
                	 msg.obj = information;
                	 if(information != null)
                	 {
 	        			handler.sendMessage(msg);
 	        		}
         		}
             }
              //  Check the Packet accuracy from UAT                
//                data = makeByte();

              if(MainActivity.bPacketSend)
              {
      				MainActivity.bPacketSend = false;
					sendData = Common.Packetize(MainActivity.dataOfScreen);
					try {

						InetAddress IPAddress = InetAddress.getByName(SERVERIP);
						DatagramPacket sendpacket = new DatagramPacket(sendData,sendData.length, IPAddress, sendPort);
						ms.send(sendpacket);
	
	        			MainActivity.bRefreshFlag = true;
						
					} catch (IOException e) {
						MainActivity.bPacketSend = true;
						e.printStackTrace();
					}
              }
            }
  
	    } finally {
//    	    if(socket != null)
//                socket.close();
        }
        
    }
 
 // Check if current network is Wifi connect.	
 
    private String getLocalHostIp() {
        String ipaddress = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
           
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress()
                            && InetAddressUtils.isIPv4Address(ip
                            .getHostAddress())) {
                        return ip.getHostAddress();
                    }
                }
            }
        }
        catch(SocketException e)
        {
            Log.e("feige", "The getting local ip address is failure");
            e.printStackTrace();
        }
        return ipaddress;
    }
 
 }

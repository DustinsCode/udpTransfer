import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Path.*;
import java.nio.file.*;
import java.util.*;

/**
 * UDP File Transfer Project
 *
 * @author Dustin Thurston
 * @author Ryan Walt
 * */

class server{
    private static final int SWS = 5;
    public static SocketAddress client = null;
    public static DatagramChannel c;
    public static DatagramSocket ds;
    public static int fileSize;
    public static int numPackets, bytesSent, bytesToSend, numSent;
    public static boolean[] packetBoolArray;
    public static DatagramPacket[] packetArray;
    public static FileInputStream fis;
    public static BufferedInputStream bis;

    public static void main(String args[]){

        try{

            c = DatagramChannel.open();

            Console cons = System.console();

            ds = c.socket();
            ds.setSoTimeout(10000);

            //Check for valid port number
            try{
                int port = 0;
                if(args.length != 1){
                    port = Integer.parseInt(cons.readLine("Enter port number: "));
                }else{
                    port = Integer.parseInt(args[0]);
                }
                if(port < 1024 || port > 65535){
                    throw new NumberFormatException();
                }
            }catch(NumberFormatException nfe){

                System.out.println("Port must be a valid integer between 1024 and 65535 Closing program...");
                return;
            }

            while(true){

              //Buffer for the name of the file requested by the client.
              ByteBuffer fileNameBuf = ByteBuffer.allocate(1024);
              client = c.receive(fileNameBuf);
              String fileName = new String(fileNameBuf.array());
              fileName = fileName.trim();
              fileName = fileName.substring(1,fileName.length());

              //Search for the file in the directory of the server.
              File clientFile = findFile(fileName);


              if (clientFile == null){
                System.out.println("File not found on the server.");
                ByteBuffer errorBuf = ByteBuffer.wrap("filenotfound".getBytes());
                c.send(errorBuf, client);
              }
              else{
                //Get the size of the file
                fileSize = (int)clientFile.length();
                //Get the total number of packets to send to the client.

                if (fileSize % 1024 == 0)
                  numPackets = fileSize / 1024;
                else
                  numPackets = (fileSize / 1024) + 1;

                String tempPacketString = numPackets + "";
                ByteBuffer numPacketsBuf = ByteBuffer.wrap(tempPacketString.getBytes());
                c.send(numPacketsBuf, client);

                packetBoolArray = new boolean[numPackets];
                Arrays.fill(packetBoolArray, false);
                packetArray = new DatagramPacket[numPackets];
                Arrays.fill(packetArray, null);

                fis = new FileInputStream(clientFile);
                bis = new BufferedInputStream(fis);
                bytesSent = 0;
                bytesToSend = 0;
                numSent = 0;
                boolean missing = false;

                while (numSent < numPackets){
                  if (!missing)
                    sendStandard();
                  else
                    sendMissing();

                  ArrayList<Integer> ackArray = getAck();
                  setNulls(ackArray, packetArray);

                  missing = isEmpty(packetArray);

                }
              }

            }
        }catch(IOException e){
            System.out.println("Got an io exception ya dingus");

        }
    }

    public static void sendMissing() throws IOException{
      for (int i = 0; i < packetArray.length; i++){
        int indexToSend;
        if (packetArray[i] != null){
          indexToSend = i;
          ds.send(packetArray[indexToSend]);
        }
      }
      //ds.send(packetArray[indexToSend]);
    }

    public static void sendStandard(){
      int count = 0;

      try{
        while (count < 5){
          if (fileSize - bytesSent < 1024)
          bytesToSend = fileSize - bytesSent;
          else
          bytesToSend = 1024;

          int tempNumSent = numSent;
          byte[] sendBytes = new byte[bytesToSend + 3];
          byte b3 = (byte)(tempNumSent & 0xFF);
          byte b2 = (byte)((tempNumSent >> 8) & 0xFF);
          byte b1 = (byte)((tempNumSent >> 16)&0xFF);
          sendBytes[0] = b1;
          sendBytes[1] = b2;
          sendBytes[2] = b3;

          bis.read(sendBytes, 3, bytesToSend);

          DatagramPacket d = new DatagramPacket(sendBytes, bytesToSend+3);
          ds.send(d);
          System.out.println("Packet sent.");
          packetArray[numSent] = d;

          bytesSent += 1024;
          numSent++;
          count++;
        }
      }
      catch(IOException e){
        System.out.println("IOException!");
      }

    }



    public static ArrayList<Integer> getAck() throws IOException{
      int count = 0;
      ArrayList<Integer> ackArray = new ArrayList<>();
      while (count < 5){
        DatagramPacket tempPacket = null;
        try{
          ds.receive(tempPacket);
          String tempString = new String(tempPacket.getData());
          tempString = tempString.trim();
          int tempNum = Integer.parseInt(tempString);
          ackArray.add(tempNum);
        }
        catch(SocketTimeoutException e){
          System.out.println("timed out");
        }
      }
      return ackArray;
    }

    public static void setNulls(ArrayList<Integer> arr, DatagramPacket[] packets){
      for (int i : arr){
        packets[i] = null;
      }
    }

    public static boolean isEmpty(DatagramPacket[] packets){
      for (int i = 0; i < packets.length; i++){
        if (packets[i] != null)
          return false;
      }
      return true;
    }


    //Return the file the user searched for.
    public static File findFile(String type){
      File sendFile = null;
      File dir = new File(server.class.getProtectionDomain().getCodeSource().getLocation().getPath());
      String newType = type.trim();

      File[] matches = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith(newType);
        }
      });

      if (matches.length < 1)
      return null;
      else{
        for(int i = 0; i < matches.length; i++){
          //System.out.println(matches[i]);
          sendFile = matches[0];
        }
        return sendFile;
      }

    }
  }

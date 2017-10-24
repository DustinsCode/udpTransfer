import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Path.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
* UDP File Transfer Project
*
* @author Dustin Thurston
* @author Ryan Walt
* */

class server{
    private static final int TIMEOUT = 3000;
    public static SocketAddress client = null;
    public static DatagramChannel c;
    public static DatagramSocket ds;
    public static int fileSize;
    public static int numPackets, bytesSent, bytesToSend, numSent;
    public static boolean[] packetBoolArray;
    public static DatagramPacket[] packetArray;
    public static FileInputStream fis;
    public static BufferedInputStream bis;
    public static String newType;
    public static CRC32 crc;

    public static void main(String args[]){

        try{
            crc = new CRC32();
            c = DatagramChannel.open();

            Console cons = System.console();

            ds = c.socket();


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
                c.bind(new InetSocketAddress(port));
            }catch(NumberFormatException nfe){

                System.out.println("Port must be a valid integer between 1024 and 65535 Closing program...");
                return;
            }

            while(true){

                //Buffer for the name of the file requested by the client.
                ByteBuffer fileNameBuf = ByteBuffer.allocate(1024);

                //testing

                ds.setSoTimeout(3000);

                DatagramPacket namePacket = new DatagramPacket(new byte[1024], 1024);
                File clientFile;

                while(true){
                    try{
                        ds.receive(namePacket);
                        client = namePacket.getSocketAddress();
                        String fileName = new String(namePacket.getData());
                        fileName = fileName.trim();
                        fileName = fileName.substring(1,fileName.length());

                        //Search for the file in the directory of the server.
                        clientFile = findFile(fileName);

                        //TODO: change this so that it sends a packet
                        if (clientFile == null){
                            System.out.println("File not found on the server.");
                            ByteBuffer errorBuf = ByteBuffer.wrap("filenotfound".getBytes());
                            c.send(errorBuf, client);
                        }else{
                            fileSize = (int)clientFile.length();
                            //Get the total number of packets to send to the client.

                            if (fileSize % 1024 == 0)
                            numPackets = fileSize / 1024;
                            else
                            numPackets = (fileSize / 1024) + 1;

                            String tempPacketString = numPackets + "";
                            //ByteBuffer numPacketsBuf = ByteBuffer.wrap(tempPacketString.getBytes());
                            DatagramPacket sizePacket = new DatagramPacket(tempPacketString.getBytes(), tempPacketString.getBytes().length, client);
                            ds.send(sizePacket);
                        }


                        break;
                    }catch(SocketTimeoutException ste){
                        //System.out.println("Timed out on file request");
                    }
                }
                //testing

                /****************
                client = c.receive(fileNameBuf);
                String fileName = new String(fileNameBuf.array());
                fileName = fileName.trim();
                System.out.println("Client wants: " + fileName);
                fileName = fileName.substring(1,fileName.length());

                //Search for the file in the directory of the server.
                File clientFile = findFile(fileName);


                if (clientFile == null){
                System.out.println("File not found on the server.");
                ByteBuffer errorBuf = ByteBuffer.wrap("filenotfound".getBytes());
                c.send(errorBuf, client);
            }

            if(clientFile != null){
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
            **************/

            packetBoolArray = new boolean[numPackets];
            Arrays.fill(packetBoolArray, false);
            packetArray = new DatagramPacket[numPackets];
            Arrays.fill(packetArray, null);

            fis = new FileInputStream(clientFile);
            bis = new BufferedInputStream(fis);
            bytesSent = 0;
            bytesToSend = 0;
            numSent = 0;
            boolean empty = true;
            int loops = 0;

            while (numSent < numPackets || !empty){
                if (empty){
                    sendStandard();
                    loops = 0;
                }
                else{
                    sendMissing();
                    loops++;
                }

            ArrayList<Integer> ackArray = getAck();
            setNulls(ackArray, packetArray);

            if (loops > 5)
                break;

            empty = isEmpty(packetArray);

        }
        System.out.println("File sent!");
    }
    // }
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
            System.out.println("Lost packet sent: " + indexToSend);
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

            if(bytesToSend < 0){
                break;
            }
            int tempNumSent = numSent;
            byte[] sendBytes = new byte[bytesToSend + 11];

            byte b10 = (byte)((tempNumSent) & 0xFF);
            byte b9 = (byte)((tempNumSent >> 8) & 0xFF);
            byte b8 = (byte)((tempNumSent >> 16)&0xFF);
            sendBytes[8] = b8;
            sendBytes[9] = b9;
            sendBytes[10] = b10;

            bis.read(sendBytes, 11, bytesToSend);

            setCheckSum(sendBytes, 8, sendBytes.length-8);

            DatagramPacket d = new DatagramPacket(sendBytes, bytesToSend+11, client);
            Random r = new Random();
            int ranNum = r.nextInt(100);

            ds.send(d);

            // if (ranNum < 10){
            //   ds.send(d);
            //   System.out.println("Packet sent: " + tempNumSent);
            // }

            System.out.println("Packet sent: " + tempNumSent);
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
        DatagramPacket tempPacket = new DatagramPacket(new byte[1024], 1024);
        try{
            ds.setSoTimeout(TIMEOUT);
            ds.receive(tempPacket);
            //System.out.println("received ack");
            byte[] data1 = tempPacket.getData();
            long tempCheck = getCheckSum(data1, 8, tempPacket.getLength()-8);

            ByteBuffer bb = ByteBuffer.wrap(new byte[] {data1[0], data1[1], data1[2], data1[3], data1[4], data1[5], data1[6], data1[7]});
            long tempCheck2 = bb.getLong();



            if (tempCheck2 == tempCheck){
                String tempString = new String(tempPacket.getData(), 8, tempPacket.getLength()-8);
                tempString = tempString.trim();
                int tempNum = Integer.parseInt(tempString);
                System.out.println("received ack: " + tempNum);

                ackArray.add(tempNum);
            }



        }
        catch(SocketTimeoutException e){
            System.out.println("timed out");
            break;
        }
        if(numPackets == ackArray.size())
        break;
        count++;
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

public static void setCheckSum(byte[] b, int off, int len){
    crc.update(b, off, len);
    long checkSum = crc.getValue();

    byte b7 = (byte)(checkSum & 0xFFFF);
    byte b6 = (byte)((checkSum >> 8)&0xFFFF);
    byte b5 = (byte)((checkSum >> 16)&0xFFFF);
    byte b4 = (byte)((checkSum >> 24)&0xFFFF);
    byte b3 = (byte)((checkSum >> 32)&0xFFFF);
    byte b2 = (byte)((checkSum >> 40)&0xFFFF);
    byte b1 = (byte)((checkSum >> 48)&0xFFFF);
    byte b0 = (byte)((checkSum >> 56)&0xFFFF);

    b[0] = b0;
    b[1] = b1;
    b[2] = b2;
    b[3] = b3;
    b[4] = b4;
    b[5] = b5;
    b[6] = b6;
    b[7] = b7;

    ByteBuffer bb = ByteBuffer.wrap(new byte[] {b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]});
    long l = bb.getLong();

    crc.reset();
}

public static long getCheckSum(byte[] b, int off, int len){
    crc.update(b, off, len);
    long checkSum = crc.getValue();
    crc.reset();
    return checkSum;
}


//Return the file the user searched for.
public static File findFile(String type){
    File sendFile = null;
    File dir = new File(server.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    newType = type.trim();

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

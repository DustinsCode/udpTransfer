import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.Path;
import java.io.File;



/**
 * UDP File Transfer Project
 *
 * @author Dustin Thurston
 * @author Ryan Walt
 */
class client{

    //private static final int SWS = 5;
    private static final int TIMEOUT = 100;

    public static void main(String[] args){
        String ip = "";
        int port = 0;
        boolean commands = false;
        int tempNum = 0;
        InetSocketAddress server;

        if(args.length == 2){
            tempNum = Integer.parseInt(args[1]);

            if(!validitycheck(args[0].trim())){
                return;
            }else if(tempNum < 1024 || tempNum > 65535){
                System.out.println("Invalid port num. Closing program..");
                return;
            }else{
                port = tempNum;
                ip = args[0];
                commands = true;
            }
        }

        try{
            DatagramChannel sc = DatagramChannel.open();
            Console cons = System.console();

            if(!commands){
                ip = cons.readLine("Enter IP address: ");
                if(!validitycheck(ip)){
                    return;
                }

                //Checks for valid port number
                try{
                    port = Integer.parseInt(cons.readLine("Enter port number: "));
                    if(port < 1024 || port > 65535){
                        throw new NumberFormatException();
                    }
                }catch(NumberFormatException nfe){
                    System.out.println("Port must be a valid integer between 1024 and 65535. Closing program...");
                    return;
                }
            }
                server = new InetSocketAddress(ip, port);
                DatagramSocket ds = sc.socket();


                //Read file name from user and make sure it's not empty
                String fileName = "";
                while(fileName.equals("")){
                  fileName = cons.readLine("Enter command or file to send: ");
                  fileName = fileName.trim();
                }
                //Remove the file if it already exists
                deleteFile(fileName);

                //String message;
                ByteBuffer buff = ByteBuffer.allocate(1024);
                ByteBuffer buffer;
                switch(fileName){

                    case "exit":
						//TODO: Change to send packet instead of buffer.
						buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.send(buffer, server);
                        return;
                    default:

                        //Check that format is correct
                        if(fileName.charAt(0) != '/'){
                            System.out.println("File name must start with '/'.");
                            break;
                        }

                        DatagramPacket namePacket = new DatagramPacket(fileName.getBytes(), fileName.getBytes().length, server);
						DatagramPacket sizePacket = new DatagramPacket(new byte[1024], 1024);

                        //buffer = ByteBuffer.wrap(fileName.getBytes());
                        //sc.send(buffer, server);
						ds.setSoTimeout(TIMEOUT);
                        while(true){
							try{
                        		ds.send(namePacket);
                        		System.out.println("Sent file name");
								ds.receive(sizePacket);

								break;
							}catch(SocketTimeoutException ste){
								System.out.println("Timed out");
							}
                        }
                        //sc.receive(buff);
                        String code = new String(sizePacket.getData());
                        code = code.trim();
                        System.out.println(code);

                        if(code.equals("error")){
                            System.out.println("There was an error retrieving the file");
                        }else if(code.equals("filenotfound")){
                            System.out.println("The file was not found.");
                        }else{
                            try{
                                //Receive amount of packets to expect
                                //buffer = ByteBuffer.allocate(1024);
                                //sc.receive(buffer);
                                //System.out.println("Packet Received");
                                //String sizeString = new String(buffer.array());
                                String sizeString = code;

                                //print out value for testing
                                long numPackets = Long.valueOf(sizeString).longValue();

                                receive(ds, fileName, (int)(numPackets), server);

                            }catch(NumberFormatException nfe){
                                System.out.println("NumberFormatException occurred");
                            }
                        }
                        break;
                }

        }catch(IOException e){
            System.out.println("An IO exception has occurred.");
            return;
        }
    }

    /***********
     * Retreive packet and return sequence number
     *
     * @return sequence number
     * **********/
    public static void receive(DatagramSocket ds, String fileName, int numPackets, InetSocketAddress server) throws IOException{

        ds.setSoTimeout(TIMEOUT);

        //create new file
        File f = new File(fileName.substring(1));

        //TODO: HANDLE TRUE PARAMETER HERE LATER.
        //     CHECK IF FILE ALREADY EXISTS
        FileChannel fc = null;
        try{
            fc = new FileOutputStream(f, true).getChannel();
        }catch(FileNotFoundException fnfe){
            System.out.println("File not found");
        }
        ByteBuffer fileBuff;

        ArrayList<DatagramPacket> packetArray = new ArrayList<>();
        int packetsRecd = 0;

        boolean[] arrived = new boolean[numPackets];
        Arrays.fill(arrived, false);

        int lastRecd = -1;
        int index = 0;

        //Store sequence nums to send acks
        ArrayList<Integer>  seqNums = new ArrayList<Integer>();

        //Start retrieving file and stuff
        while(packetsRecd < numPackets){
            try{
                DatagramPacket packet = new DatagramPacket(new byte[1027], 1027);
                ds.receive(packet);
                packetArray.add(packet);
                //packetsRecd ++;
                byte[] data = packet.getData();

                //USE THIS FOR GETTING SEQUENCE NUM FROM BYTE[]
                int sequenceNum = ((data[0] & 0xFF)<<16) +((data[1] & 0xFF)<<8) + (data[2] & 0xFF);
                seqNums.add(sequenceNum);
                System.out.println("Packet Received: " + sequenceNum);


                //If packet not already There, put data into filechannel
                //else, otherwise, throws away packet and will resend ack
                if(!arrived[sequenceNum]){
                    arrived[sequenceNum] = true;

                    int iterate = 0;
                    while(!packetArray.isEmpty() && iterate < 10){

                      for(int i = 0; i < packetArray.size(); i++){
                          data = packetArray.get(i).getData();
                          sequenceNum = ((data[0] & 0xFF)<<16) +((data[1] & 0xFF)<<8) + (data[2] & 0xFF);
                          if(sequenceNum == lastRecd+1){
                              index = i;
                              break;
                          }
                      }

                        if(sequenceNum == lastRecd+1){
                            packetsRecd++;
                            lastRecd = sequenceNum;
                            fileBuff = ByteBuffer.allocate(packetArray.get(index).getLength()-3);
                            fileBuff = ByteBuffer.wrap(packetArray.get(index).getData(), 3, packetArray.get(index).getLength()-3);
                            fc.write(fileBuff);
                            System.out.println("Packet written: " + sequenceNum);
                            packetArray.remove(index);

                            if(packetArray.isEmpty()){
                                break;
                            }
                        }


                        iterate++;
                    }
                }else{
                    packetArray.remove(packetArray.size()-1);
                }
                //send acknowledgments
                while(!seqNums.isEmpty()){
                    sendAck(ds, seqNums.get(0), server);
                    seqNums.remove(0);
                }
            }catch(SocketTimeoutException ste){
                System.out.println("Receive Timed Out");
            }
        }
        fc.close();
    }

    public static void sendAck(DatagramSocket ds, int seqNum, InetSocketAddress server) throws IOException{

        String ackNum = "" + seqNum;

        DatagramPacket p = new DatagramPacket(ackNum.getBytes(), ackNum.getBytes().length, server);
        ds.send(p);
        System.out.println("Ack sent");
        return;
    }

    /**
     * Checks validity of user given IP address
     *
     * @param ip user typed IP address\
     * @return true if valid, false if not
     * */
    public static boolean validitycheck(String ip){
        try{
            String[] iparray = ip.split("\\.");
            int[] ipintarray = new int[iparray.length];
            for(int i = 0; i < iparray.length; i++){
                ipintarray[i] = Integer.parseInt(iparray[i]);
            }
            if(ipintarray.length != 4){
                throw new NumberFormatException();
            }else{
                return true;
            }
        }catch(NumberFormatException nfe){
            System.out.println("Invalid IP address. Closing program..");
            return false;
        }
    }

    public static void deleteFile(String name){
      File dir = new File(client.class.getProtectionDomain().getCodeSource().getLocation().getPath() + name);
      if (dir.delete())
        System.out.println("File removed.");


    }
}

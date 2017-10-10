import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class client{
    private static final int SWS = 5;

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

                server = new InetSocketAddress(ip, port);
                DatagramSocket ds = sc.socket();

                while(true){
                //Read file name from user and make sure it's not empty
                String fileName = "";
                while(fileName.equals("")){
                  fileName = cons.readLine("Enter command or file to send: ");
                  fileName = fileName.trim();
                }


                String message;
                ByteBuffer buff = ByteBuffer.allocate(1024);
                ByteBuffer buffer;
                switch(fileName){
                    case "exit":
                        buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.send(buffer, server);
                        return;
                    default:

                        //Check that format is correct
                        if(fileName.charAt(0) != '/'){
                            System.out.println("File name must start with '/'.");
                            break;
                        }

                        buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.send(buffer, server);

                        sc.receive(buff);
                        String code = new String(buff.array());
                        code = code.trim();

                        if(code.equals("error")){
                            System.out.println("There was an error retrieving the file");
                        }else if(code.equals("filenotfound")){
                            System.out.println("The file was not found.");
                        }else{
                            try{
                                //Receive amount of packets to expect
                                buffer = ByteBuffer.allocate(1024);
                                sc.receive(buffer);
                                String sizeString = new String(buffer.array());
                                sizeString = sizeString.trim();
                                //print out value for testing
                                System.out.println(sizeString);
                                long numPackets = Long.valueOf(sizeString).longValue();

                                //create new file
                                File f = new File(fileName.substring(1);
                                FileChannel fc = new FileOutoutStream(f, false).getChannel();

                                DatagramPacket[] packetArray = new DatagramPacket[5];
                                int packetsRecd = 0;

                                //Start retrieving file and stuff
                                while(packetsRecd < numPackets){
                                    DatagramPacket packet = ds.receive();
                                    byte[] data = packet.getData();

                                    //USE THIS FOR GETTING SEQUENCE NUM FROM BYTE[]
                                    int sequenceNum = ((data[0] & 0xFF)<<16) +((data[1] & 0xFF)<<8) + (data[2] & 0xFF);;

                                }


                            }catch(NumberFormatException nfe){
                                System.out.println("NumberFormatException occurred");
                            }
                        }
                }
            }
        }catch(IOException e){
            System.out.println("An IO exception has occurred.");
            return;
        }

    }

    /**
     * Checks validity of user given IP address
     *
     * @param ip user tyuped IP address\
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
}

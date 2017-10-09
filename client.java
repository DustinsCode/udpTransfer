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

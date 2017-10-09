import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Path.*;
import java.nio.file.*;

/**
 * UDP File Transfer Project
 *
 * @author Dustin Thurston
 * @author Ryan Walt
 * */

class server{
    private static final int SWS = 5;

    public static void main(String args[]){
        try{
            DatagramChannel c = DatagramChannel.open();
            Console cons = System.console();

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
                System.out.println("Port must be a valid integer between 1024 and 65535 Closing program..");
                return;
            }

        }catch(IOException e){
            System.out.println("Got an io exception ya dingus");
        }
    }
}

import java.util.*;

/**
 * Testing bit shifting to store ints into bytes
 *
 * @author Dustin Thurston
 * */
class test{
    public static void main(String[] args){
        byte[] b = new byte[3];
        int yes = 1234567;
        byte b3 = (byte)(yes & 0xFF);
        byte b2 = (byte)((yes >> 8) & 0xFF);
        byte b1 = (byte)((yes >> 16)&0xFF);
        b[0] = b1;
        b[1] = b2;
        b[2] = b3;
        int newInt = ((b[0] & 0xFF)<<16) +((b[1] & 0xFF)<<8) + (b[2] & 0xFF);;
        System.out.println("Original:\t" + yes + "\nAfter Byte Conversion:\t" + newInt);
        return;
    }
}

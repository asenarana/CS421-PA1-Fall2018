/*
Asena Rana Yozgatli
21000132

CS421 Computer Networks
Programming Assignment 1: Application Level Routing
Fall 2018
*/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Router {

    private static ServerSocket serverSocket;
    private static List<AbstractMap.SimpleEntry<String, String>> operators = new ArrayList<>();

    public static void main(String[] args) {
        AbstractMap.SimpleEntry<String, String> operator;

        if(args.length < 4 || (args.length & 1) == 1)
        {
            System.out.println("Insufficient arguments!");
            System.exit(-1);
        }
        for(int i = 2; i<args.length ; i += 2)
        {
            operator = new AbstractMap.SimpleEntry<>(args[i], args[i + 1]);
            operators.add(operator);
        }

        if(!initRouter(args[1]))
        {
            System.exit(-1);
        }
        listen();
        System.exit(0);
    }

    private static boolean initRouter(String address)
    {
        int port= Integer.parseInt(address.split(":")[1]);
        InetAddress IP = strToINet(address.split(":")[0]);
        if(IP == null) return false;
        try
        {
            serverSocket = new ServerSocket(port, 10, IP);
        }
        catch (IOException e)
        {
            System.out.println("Error while creating the client socket:\n" + e);
            return false;
        }
        return true;
    }

    private static void listen()
    {
        byte[] clientMessage = new byte[1024];
        byte[] data;
        String message;
        String ops;
        int messageSize;
        try
        {
            Socket server = serverSocket.accept();
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            messageSize = in.read(clientMessage);
            message = new String(clientMessage, 0, messageSize);
            data = message.split("OPS:")[0].strip().getBytes();
            ops = message.split("OPS:")[1].strip();
            for(String op : ops.split(","))
            {
                data = doOp(op, data);
                if(data == null) System.exit(-1);
            }
            out.write(data);
            server.close();
        }
        catch (IOException e)
        {
            System.out.println("Error while communicating with the client:\n" + e);
            System.exit(-1);
        }
    }

    private static byte[] doOp(String key, byte[] data)
    {
        Socket opSocket;
        int resultSize;
        byte[] result = new byte[14];
        int opIndex = findOpIndex(key);
        if(opIndex == -1) return null;
        String address = operators.get(opIndex).getValue();
        int port= Integer.parseInt(address.split(":")[1]);
        InetAddress IP = strToINet(address.split(":")[0]);
        if(IP == null) return null;
        try
        {
            opSocket = new Socket(IP, port);
            DataInputStream in = new DataInputStream(opSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(opSocket.getOutputStream());
            out.write(data);
            resultSize = in.read(result);
            if(resultSize == -1)
            {
                System.out.println("Operator " + key + " returned invalid result!");
                return null;
            }
            if(resultSize < 14)
            {
                String temp = "DATA:";
                for(int i = 0; i < 14-resultSize; i++)
                {
                    temp = temp.concat("0");
                }
                temp = temp.concat((new String(result)).split("DATA:")[1]);
                result = temp.getBytes();
            }
            result = Arrays.copyOfRange(result, 0, 13);
            opSocket.close();
        }
        catch (IOException e)
        {
            System.out.println("Error while creating the operator " + key + " socket:\n" + e);
            return null;
        }
        return result;
    }

    private static InetAddress strToINet( String address)
    {
        InetAddress IP;
        try {
            IP = InetAddress.getByName(address);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Error while resolving the address:\n" + e);
            return null;
        }
        return IP;
    }

    private static int findOpIndex(String key)
    {
        for( int i = 0; i < operators.size(); i++)
        {
            if(operators.get(i).getKey().equals(key)) return i;
        }
        System.out.println("Operator " + key + " is not defined!");
        return -1;
    }
}

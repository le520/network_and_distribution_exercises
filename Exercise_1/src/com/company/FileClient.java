package com.company;

import java.io.*;
import java.net.*;

/**
 * This class implements the file client, information transmission using TCP protocol, file transmission using UDP.
 * Support disconnection and reconnection, file verification, file management.
 *
 * @author Noorall
 * @version 1.0
 */
public class FileClient {
    private Socket tcpClient;
    private BufferedReader bufferedReader;
    private String serverName;
    private int port;

    /**
     * File management client's constructor.
     *
     * @param initServerName The address of the server to be connected.
     * @param initPort       The port of the server to be connected.
     */
    public FileClient(String initServerName, int initPort) {
        serverName = initServerName;
        port = initPort;
        System.out.println(serverName + ":" + port + ">连接成功");
    }

    /**
     * Send instructions to the server and return receipt information.
     *
     * @param order Command to be sent.
     * @return Receipt information of the remote server to the instruction.
     */
    private String sentOrder(String order) throws IOException {
        OutputStream outToServer = tcpClient.getOutputStream();
        DataOutputStream outData = new DataOutputStream(outToServer);
        outData.writeUTF(order);
        InputStream inFromServer = tcpClient.getInputStream();
        DataInputStream inData = new DataInputStream(inFromServer);
        return inData.readUTF();
    }

    /**
     * Used for file reception, it is a follow-up supplement to the get command.
     *
     * @param fileName   File name to be stored.
     * @param fileLength File length information.
     * @return Server receipt information
     */
    private String getFile(String fileName, Long fileLength) throws IOException {
        if (fileLength < 0) {
            return "文件长度有误!";
        } else {
            System.out.println("开始接收文件:" + fileName);
        }
        DatagramSocket udpClient = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        packet.setAddress(InetAddress.getByName("127.0.0.1"));
        packet.setPort(2020);
        udpClient.send(packet);
        File file = new File(fileName);
        OutputStream os = new FileOutputStream(file);
        try {
            int buffLength;
            while (!udpClient.isClosed()) {
                buffLength = Integer.parseInt(sentOrder("getMessage"));
                packet.setLength(buffLength);
                udpClient.receive(packet);
                os.write(packet.getData(), 0, buffLength);
                os.flush();
                if (file.length() >= fileLength) {
                    os.close();
                    udpClient.close();
                    return sentOrder("getMessage");
                }
            }
        } catch (IOException e) {
            udpClient.close();
        }
        udpClient.disconnect();
        os.close();
        udpClient.close();
        return "未知错误";
    }

    /**
     * Reconnect when the server is accidentally disconnected, it will try three times with an interval of 5 seconds.
     *
     * @return true will be returned if the reconnection is successful.
     */
    private boolean reConnect() {
        try {
            tcpClient.close();
            tcpClient = new Socket(serverName, port);
        } catch (IOException e) {
            return false;
        }
        System.out.println("重连成功！");
        System.out.println(serverName + ":" + port + ">连接成功");
        return true;
    }

    /**
     * Entry method for program operation.
     */
    public void run() throws InterruptedException {
        String order;
        try {
            tcpClient = new Socket(serverName, port);
            bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        } catch (ConnectException e) {
            System.out.println("服务器拒绝连接！");
            return;
        } catch (IOException e) {
            System.out.println("客户端出错！");
        }
        while (true) {
            try {
                order = bufferedReader.readLine();
                String result = sentOrder(order);
                if (order.equals("bye")) {
                    tcpClient.close();
                } else if (order.startsWith("get") && !result.startsWith("你获取的不是一个文件!")) {
                    result = getFile(order.split("\\s+")[1], Long.parseLong(result));
                } else {

                }
                System.out.print(result);
            } catch (SocketException e) {
                System.out.println("服务器连接中断，正在重连。。。");
                for (int i = 0; i < 3; i++) {
                    System.out.println("尝试第" + String.valueOf(i) + "次连接。");
                    if (reConnect()) {
                        break;
                    }
                    Thread.sleep(5000);
                }
            } catch (IOException e) {
                System.out.println("系统异常！");
            }
        }
    }

    /**
     * Main function
     *
     * @param args args
     */
    public static void main(String[] args) throws InterruptedException {
        FileClient fileClient = new FileClient("127.0.0.1", 2021);
        fileClient.run();
    }
}
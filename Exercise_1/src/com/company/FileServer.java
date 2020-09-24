package com.company;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * This class implements the basics of the file transfer protocol, which allows you to tailor the client and create multiple threads.
 *
 * @author Noorall
 * @version 1.0
 */
public class FileServer extends Thread {
    private ServerSocket tcpServerSocket;
    private DatagramSocket udpServerSocket;
    private String rootDir;

    /**
     * This method is the constructor of the class used to start the FileServer.
     *
     * @param tcpPort The port of tcp socket.
     * @param udpPort The port of udp socket.
     */
    public FileServer(int tcpPort, int udpPort, String initRootDir) throws IOException {
        tcpServerSocket = new ServerSocket(tcpPort);
        udpServerSocket = new DatagramSocket(udpPort);
        rootDir = initRootDir;
    }

    /**
     * This method detects the existence of the specified directory.
     *
     * @param dir The directory need to check.
     * @return true will be returned if the directory exists
     */
    private boolean DirCheck(String dir) {
        File file = new File(dir);
        return file.isDirectory();
    }

    /**
     * Run FileServer
     */
    public void run() {
        if (!DirCheck(rootDir)) {
            System.out.println("路径违法，程序终止");
            return;
        }
        while (true) {
            try {
                System.out.println("服务器启动,等待用户连接。");
                Socket server = tcpServerSocket.accept();
                new Thread(new FileServerHandler(server, udpServerSocket, rootDir)).start();
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Main function
     *
     * @param args args
     */
    public static void main(String[] args) {
        try {
            Thread t = new FileServer(2021, 2020, args[0]);
            t.run();
        } catch (IOException e) {
            System.out.println("服务器异常");
        }
    }
}

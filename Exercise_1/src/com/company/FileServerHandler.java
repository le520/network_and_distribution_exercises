package com.company;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;

/**
 * This class implements the specific instruction operation of file transfer protocol, including remote transfer and so on.
 *
 * @author Noorall
 * @version 1.0
 */
public class FileServerHandler implements Runnable {
    private Socket tcpServer;
    private Stack<String> currentDir;
    private DatagramSocket udpServer;

    /**
     * FileServer multi-threaded constructor.
     *
     * @param initTcpServer  Copy of the connected tcp socket.
     * @param initUdpServer  Created udp server.
     * @param initCurrentDir Initial root directory.
     */
    public FileServerHandler(Socket initTcpServer, DatagramSocket initUdpServer, String initCurrentDir) {
        tcpServer = initTcpServer;
        udpServer = initUdpServer;
        currentDir = new Stack<>();
        currentDir.push(initCurrentDir);
    }

    /**
     * Implement remote list commands.
     *
     * @return List information of the directory.
     */
    private String lsDir() {
        String result = "";
        File file = new File(getCurrentDir());
        if (file.isDirectory()) {
            String[] list = file.list();
            File[] fileList = file.listFiles();
            for (File fileInfo : fileList) {
                if (fileInfo.isFile()) {
                    result += "<File>";
                } else {
                    result += "<Dir>";
                }
                result = result + "    " + fileInfo.getName() + "    " + fileInfo.length() + '\n';
            }
        } else {
            result = "当前路径不是一个文件夹\n";
        }
        result = result + getCurrentVirtualDir() + " >";
        return result;
    }

    /**
     * Implement remote cd commands.
     *
     * @return Receipt information for command execution.
     */
    private String cdDir(String dir) {
        String result = "";
        File file;
        if (dir.startsWith("/")) {
            file = new File(currentDir.get(0) + dir);
        } else {
            file = new File(getCurrentDir() + dir);
        }
        if (file.isDirectory()) {
            dirProcess(dir);
            result = getCurrentVirtualDir() + " >";
        } else {
            currentDir.pop();
            result = "当前路径不是一个文件夹\n" +
                    getCurrentVirtualDir() + " >";
        }
        return result;
    }

    /**
     * Convert String to real directory path information.
     *
     * @param dir The directory to be processed.
     */
    private void dirProcess(String dir) {
        dir = dir.replace(" ", "");
        String[] originDirs = dir.split("/");
        if (dir.startsWith("/")) {
            while (currentDir.size() != 1) {
                currentDir.pop();
            }
        }
        for (String originDir : originDirs) {
            if (originDir.equals("..")) {
                if (currentDir.size() > 1) {
                    currentDir.pop();
                }
            } else if (originDir.equals(".")) {
                continue;
            } else {
                currentDir.push(originDir);
            }
        }
    }

    /**
     * Get the current real directory path.
     *
     * @return Current director path.
     */
    private String getCurrentDir() {
        String result = "";
        for (int i = 0; i < currentDir.size(); i++) {
            result = result + currentDir.get(i) + "/";
        }
        return result;
    }

    /**
     * Get the current virtual directory path.
     *
     * @return Current virtual director path.
     */
    private String getCurrentVirtualDir() {
        String result = tcpServer.getLocalSocketAddress().toString() + "/";
        for (int i = 1; i < currentDir.size(); i++) {
            result = result + currentDir.get(i) + "/";
        }
        return result;
    }

    /**
     * Realize the file transfer protocol, use tcp to transfer file length, udp to transfer file content.
     *
     * @param fileName         File name to be obtained.
     * @param dataOutputStream Reference used to send tcp information.
     * @return Receipt information for command execution.
     */
    private String getFile(String fileName, DataOutputStream dataOutputStream) throws IOException {
        File file = new File(getCurrentDir() + "/" + fileName);
        String result = "";
        if (file.isFile()) {
            dataOutputStream.writeUTF(String.valueOf(file.length()));
            DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
            udpServer.receive(datagramPacket);
            InputStream inputStream = new FileInputStream(file);
            DatagramPacket filePacket = new DatagramPacket(new byte[1024], 1024, datagramPacket.getAddress(), datagramPacket.getPort());
            byte[] buffer = new byte[1024];
            int buffLength;
            while (true) {
                buffLength = inputStream.readNBytes(buffer, 0, 1024);
                if (buffLength <= 0) {
                    break;
                }
                dataOutputStream.writeUTF(String.valueOf(buffLength));
                filePacket.setData(buffer);
                udpServer.send(filePacket);
            }
            result = "传输完成\n" +
                    getCurrentVirtualDir() + " >";
        } else {
            result = "你获取的不是一个文件!\n" +
                    getCurrentVirtualDir() + " >";
        }
        return result;
    }

    @Override
    public void run() {
        boolean isOK = true;
        while (isOK) {
            try {
                DataInputStream dataIn = new DataInputStream(tcpServer.getInputStream());
                String[] order = dataIn.readUTF().split("\\s+");
                DataOutputStream outData = new DataOutputStream(tcpServer.getOutputStream());
                switch (order[0]) {
                    case "cd":
                        outData.writeUTF(cdDir(order[1]));
                        break;
                    case "ls":
                        outData.writeUTF(lsDir());
                        break;
                    case "get":
                        outData.writeUTF(getFile(order[1], outData));
                        break;
                    case "bye":
                        outData.writeUTF("bye");
                        tcpServer.close();
                        isOK = false;
                        System.out.println(tcpServer.getRemoteSocketAddress() + "断开链接");
                        break;
                    case "getMessage":
                        break;
                    default:
                        outData.writeUTF("未知指令: " + order[0] + "\n" + getCurrentVirtualDir());
                }
            } catch (SocketException e) {
                System.out.println(tcpServer.getRemoteSocketAddress() + "断开链接");
                isOK = false;
            } catch (IOException e) {
                isOK = false;
                System.out.println("服务器出现错误断开链接。");
            }
        }
    }
}

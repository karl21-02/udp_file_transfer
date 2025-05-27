package com.kangwon.sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FileSender {

    private static DatagramSocket udpSocket;

    public static void main(String[] args) {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String fileName = args[2];
        start(ip, port, fileName);
    }

    public static void start(String ip, int port, String fileName) {
        try {
            InetSocketAddress newIp = new InetSocketAddress(InetAddress.getByName(ip), port);
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(5000);

            byte[] greeting = "Greeting".getBytes(StandardCharsets.UTF_8);
            byte[] sendingFileName = fileName.getBytes(StandardCharsets.UTF_8);

            DatagramPacket greetingPacket = new DatagramPacket(
                    greeting, greeting.length, newIp
            );
            DatagramPacket fileNamePacket = new DatagramPacket(
                    sendingFileName, sendingFileName.length, newIp
            );

            // Greeting 문자열과 파일 이름 전송
            udpSocket.send(greetingPacket);
            udpSocket.send(fileNamePacket);

            // 이제 OK 문자열 수신
            if(waitFromServerConnectionAnswerFirst().equals("OK")) {
                // 파일 전송 시작
                startFileTransfer(fileName, fileNamePacket.getAddress(), port);

                // 파일 전송이 끝나면 "Finish"를 Receiver에게 보낸다.
                finishTransferCheck(greetingPacket.getAddress(), port);

                // Receiver에게 "WellDone"을 받는다.
                getWellDoneFromReceiver();
            }
        } catch (IOException e) {
            System.err.println("start 함수에서 에러 발생");
            throw new RuntimeException(e);
        }
        finally {
            udpSocket.close();
        }
    }

    public static void getWellDoneFromReceiver() throws IOException {
        String responseFromReceiverWellDone = "";
        byte[] buffer = new byte[1024];
        while(true) {
            DatagramPacket responseFromServer = new DatagramPacket(buffer, 1024);
            udpSocket.receive(responseFromServer);
            responseFromReceiverWellDone = new String(responseFromServer.getData(), 0, responseFromServer.getLength()).trim();
            if(!responseFromReceiverWellDone.isEmpty()) {
                System.out.println("From FileReceiver : " + responseFromReceiverWellDone);
                break;
            }
        }
    }

    public static void finishTransferCheck(InetAddress receiverAddress, int port) throws IOException {
        try {
            String finish = "Finish";
            byte[] finishBytes = finish.getBytes(StandardCharsets.UTF_8);
            DatagramPacket filePacket = new DatagramPacket(finishBytes, finishBytes.length, receiverAddress, port);
            udpSocket.send(filePacket);
        } catch (IOException ioException) {
            System.err.println("finishTransferCheck 함수에서 에러 발생");
            ioException.printStackTrace();
        }
    }

    public static void startFileTransfer(String fileName, InetAddress receiverAddress, int port) throws FileNotFoundException {
        File fileToSend = new File("sender/fileForSender", fileName);
        byte[] buffer = new byte[2048];

        try(FileInputStream fis = new FileInputStream(fileToSend)) {
            DatagramPacket filePacket = new DatagramPacket(buffer, buffer.length, receiverAddress, port);
            int read = 0;
            while((read = fis.read(buffer)) != -1) {
                filePacket.setData(buffer, 0, read);
                udpSocket.send(filePacket);
            }
            DatagramPacket eofPacket = new DatagramPacket(new byte[0], 0, receiverAddress, port);
            udpSocket.send(eofPacket);
        } catch (IOException e) {
            System.err.println("startFileTransfer 함수에서 에러 발생");
            throw new RuntimeException(e);
        }
    }

    public static String waitFromServerConnectionAnswerFirst() throws IOException {
        String responseFromServerString = "";
        byte[] buffer = new byte[1024];
        while(true) {
            DatagramPacket responseFromServer = new DatagramPacket(buffer, 1024);
            udpSocket.receive(responseFromServer);
            responseFromServerString = new String(responseFromServer.getData(), 0, responseFromServer.getLength()).trim();
            if(!responseFromServerString.isEmpty()) {
                System.out.println("From FileReceiver : " + responseFromServerString);
                break;
            }
        }
        return responseFromServerString;
    }
}

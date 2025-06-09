package com.kangwon.receiver;

import com.kangwon.global.entity.Packet;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class FileReceiver {
    public static void main(String[] args) {
        int ReceiverPort = Integer.parseInt(args[0]);
        double ackDropProbability = Double.parseDouble(args[1]);
        start(ReceiverPort, ackDropProbability);
    }
    private static DatagramSocket udpSocket;
    public static void start(int port, double ackDropProbability) {
        try {
            udpSocket = new DatagramSocket(port);
            int count = 2;
            DatagramPacket packetFromSender = null;
            InetAddress address = null;
            String fileToSavedName = "";
            String receivedStringPacket = "";
            while (count > 0) {
                // 1. Greeting 문자열과 파일 이름 출력
                while(true) {
                    packetFromSender = new DatagramPacket(new byte[1024], 1024);

                    udpSocket.receive(packetFromSender);
                    address = packetFromSender.getAddress();

                    receivedStringPacket = new String(packetFromSender.getData(), 0, packetFromSender.getLength()).trim();
                    if(!receivedStringPacket.equals("Greeting")) {
                        fileToSavedName = receivedStringPacket;
                    }

                    if(!receivedStringPacket.isEmpty()) {
                        System.out.println(receivedStringPacket);
                        break;
                    }
                }
                count--;
            }
            responseAtStart(address, packetFromSender.getPort());

            // 초기 연결 확인 끝 파일 받기 시작
            getFiles(fileToSavedName);

            // Sender로 부터 "Finish" 받고 출력
            getFinishCheckFromSender();

            // Receiver가 Sender에게 "WellDone" 재전송하기
            sendWellDoneToSender(address, packetFromSender.getPort());

        } catch (IOException ioException) {
            System.err.println("start 함수에서 에러 발생");
            ioException.printStackTrace();
        }
        finally {
            udpSocket.close();
        }
    }

    public static void sendWellDoneToSender(InetAddress ip, int port) throws IOException {
        InetSocketAddress newIp = new InetSocketAddress(ip, port);
        byte[] greeting = "WellDone".getBytes(StandardCharsets.UTF_8);

        DatagramPacket greetingPacket = new DatagramPacket(
                greeting, greeting.length, newIp
        );
        udpSocket.send(greetingPacket);
    }

    public static void getFinishCheckFromSender() throws IOException {
        String finishFromServer = "";
        byte[] buffer = new byte[1024];
        while(true) {
            DatagramPacket responseFromServer = new DatagramPacket(buffer, 1024);
            udpSocket.receive(responseFromServer);
            finishFromServer = new String(responseFromServer.getData(), 0, responseFromServer.getLength()).trim();
            if(!finishFromServer.isEmpty()) {
                System.out.println("From FileSender : " + finishFromServer);
                break;
            }
        }
    }

    public static void getFiles(String savedFileName) throws IOException {
        DatagramPacket packetFileFromServer = new DatagramPacket(new byte[65536], 65536);
        File fileToSaved = new File("com/kangwon/receiver/fileForReceiver/" + savedFileName);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileToSaved));
        try {
            while(true) {
                udpSocket.receive(packetFileFromServer);
                if(packetFileFromServer.getLength() > 0) {
                    byte[] chunk = packetFileFromServer.getData();
                    Packet chunkReceived = null;
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(chunk);
                        ObjectInputStream ois = new ObjectInputStream(bais)) {
                        chunkReceived = (Packet) ois.readObject();

                        if(chunkReceived.getType() == 0) {
                            dos.write(chunkReceived.getData(), 0, chunkReceived.getLength());

                            // 받은 패킷의 시퀀스 번호(seqNum)로 ACK 패킷을 생성합니다. (type=1)
                            Packet ackPacket = new Packet(1, chunkReceived.getSeqNum());

                            // ACK 패킷을 직렬화하여 바이트 배열로 만듭니다.
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(ackPacket);
                            byte[] ackData = baos.toByteArray();

                            // Sender가 보낸 패킷의 주소와 포트 정보를 사용하여 응답할 목적지를 설정합니다.
                            DatagramPacket datagramPacketACK = new DatagramPacket(
                                    ackData,
                                    ackData.length,
                                    packetFileFromServer.getAddress(), // Sender의 IP 주소
                                    packetFileFromServer.getPort()    // Sender의 포트 번호
                            );

                            // 생성된 ACK 패킷을 Sender에게 전송합니다.
                            udpSocket.send(datagramPacketACK);
                        }
                        else if(chunkReceived.getType() == 2) { // EOT
                            Packet ackPacket = new Packet(1, chunkReceived.getSeqNum());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(ackPacket);
                            byte[] ackData = baos.toByteArray();

                            DatagramPacket datagramPacketACK = new DatagramPacket(
                                    ackData,
                                    ackData.length,
                                    packetFileFromServer.getAddress(),
                                    packetFileFromServer.getPort()
                            );

                            // 생성된 ACK 패킷을 Sender에게 전송합니다.
                            udpSocket.send(datagramPacketACK);
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        throw new IOException("Failed to deserialize packet", e);
                    }
                }
                else {
                    break;
                }
            }
        } catch (IOException ioException) {
            System.err.println("getFile 함수에서 에러 발생");
            ioException.printStackTrace();
        }
    }

    public static void responseAtStart(InetAddress ip, int port) throws IOException {
        InetSocketAddress newIp = new InetSocketAddress(ip, port);
        byte[] greeting = "OK".getBytes(StandardCharsets.UTF_8);

        DatagramPacket greetingPacket = new DatagramPacket(
                greeting, greeting.length, newIp
        );
        udpSocket.send(greetingPacket);
    }
}

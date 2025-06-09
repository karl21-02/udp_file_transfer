package com.kangwon.sender;

import com.kangwon.global.entity.Packet;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileSender {

    private static DatagramSocket udpSocket;
    private static int retransmissionCount = 0;

    public static void main(String[] args) {
        int senderPort = Integer.parseInt(args[0]);
        String receiverIpAddress = args[1];
        int receiverPort = Integer.parseInt(args[2]);
        int timeoutInterval = Integer.parseInt(args[3]);
        String fileName = args[4];
        double ackDropProbability = Double.parseDouble(args[5]);
        start(senderPort, receiverIpAddress, receiverPort, timeoutInterval, fileName, ackDropProbability);
    }

    public static void start(int senderPort, String receiverIpAddress, int receiverPort, int timeoutInterval, String fileName, double ackDropProbability) {
        try {
            InetSocketAddress newIp = new InetSocketAddress(InetAddress.getByName(receiverIpAddress), receiverPort);
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(timeoutInterval);
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
                startFileTransferWithRDT(fileName, greetingPacket.getAddress(), receiverPort, ackDropProbability);

                // 파일 전송이 끝나면 "Finish"를 Receiver에게 보낸다.
                finishTransferCheck(greetingPacket.getAddress(), receiverPort);

                // Receiver에게 "WellDone"을 받는다.
                getWellDoneFromReceiver();
            }
        } catch (IOException e) {
            System.err.println("start 함수에서 에러 발생");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            udpSocket.close();
        }
    }

    public static void startFileTransferWithRDT(String fileName, InetAddress address, int port, double ackDropProbability) throws IOException, ClassNotFoundException {
        File fileToSend = new File("com/kangwon/sender/fileForSender", fileName);
        List<byte[]> chunks = new ArrayList<>();

        try(FileInputStream fis = new FileInputStream(fileToSend)) {
            // 파일을 읽어와서 청크 단위로 나누기
            byte[] tmp = new byte[1000];
            int read;
            while((read = fis.read(tmp)) != -1) {
                byte[] chunk = new byte[read];
                System.arraycopy(tmp, 0, chunk, 0, read);
                chunks.add(chunk);
            }
        } catch (IOException e) {
            System.err.println("error while reading file");
            throw new RuntimeException(e);
        }

        int curSeqNum = 0;
        int retransmissionCount = 0;

        // 현재 순서 번호가 보내야하는 청크 개수보다 작으면 true
        while(curSeqNum < chunks.size()) {
            // ack를 받지 못하면 계속 청크 전송
            Packet packet = new Packet(0, curSeqNum, chunks.get(curSeqNum));
            boolean ack = false;
            while(!ack) {
                try {
                    // 1. packet 보내기
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(packet);

                    byte[] sendData = baos.toByteArray();
                    udpSocket.send(new DatagramPacket(sendData, sendData.length, address, port));


                    // 2. ACK 수신 대기
                    byte[] ackData = new byte[1000];
                    DatagramPacket datagramPacketACK = new DatagramPacket(ackData, ackData.length);
                    System.out.println("waiting");
                    udpSocket.receive(datagramPacketACK);

                    Packet receivedACK = null;
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacketACK.getData());
                        ObjectInputStream ois = new ObjectInputStream(bais);

                        receivedACK = (Packet) ois.readObject();
                    }
                    catch (ClassNotFoundException classNotFoundException) {
                        throw new IOException("패킷을 찾을 수 없습니다");
                    }

                    // ACK를 잘 받았을 경우와 다른 번호의 ACK가 올 경우
                    if(receivedACK.getType() == 1 && receivedACK.getAckNum() == curSeqNum) {
                        System.out.println(String.format("ACK-RCVD: ACK, ackNum=%d", receivedACK.getAckNum()));
                        ack = true;
                    }
                    else {
                        System.out.println(String.format("WRONG-ACK: Received ackNum=%d, expected=%d", receivedACK.getAckNum(), curSeqNum));
                    }

                } catch (SocketTimeoutException e) {
                    // 3. 타임아웃 발생! 패킷 재전송
                    System.out.println("Timeout event!");
                    retransmissionCount++;
                } catch (IOException e) {
                    System.err.println("Error while waiting for ACK");
                    break;
                }
                // 종료를 알리는 EOT 전송

                // 3. ACK 도착 전 타이머 강제 종료
                    // 타이머 종료시 실패이므로, 패킷 재전송

                // 4. ACK를 올바르게 수신한 경우 패킷 재전송 루프 진행
            }
            curSeqNum++;
        }

        // EOT
        sendPacketToReceiver(new Packet(2, curSeqNum), address, port);

        // ACK Receive of EOT
        byte[] ackData = new byte[1000];
        DatagramPacket datagramPacketACK = new DatagramPacket(ackData, ackData.length);
        udpSocket.receive(datagramPacketACK);

        Packet receivedACK = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacketACK.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);

            receivedACK = (Packet) ois.readObject();
            if(receivedACK.getType() == 1 && receivedACK.getAckNum() == curSeqNum) {
                System.out.println("connection terminates");
            }
            System.out.println(retransmissionCount);
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new IOException("패킷을 찾을 수 없습니다");
        }
    }

    private static void sendPacketToReceiver(Packet packet, InetAddress address, int port) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(packet);

        byte[] sendData = baos.toByteArray();
        udpSocket.send(new DatagramPacket(sendData, sendData.length, address, port));
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

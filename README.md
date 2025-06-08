# udp_file_transfer



1. 개요 (Overview)

이 프로그램에는 두 가지 자바 객체가 있습니다.

(1) FileSender로 fileForSender안에 있는 파일 중 사용자가 입력한 파일을 FileReceiver에게 전송합니다.

(2) FileReceive로 fileForSender에 사용자가 입력한 파일을 전송 받아 저장합니다.

또한, 파일 전송을 하기 전후로 UDP 소켓 연결을 테스트하기 위해 "Greeting", "Finish" 등 정해진 메시지를 주고 받습니다.



2. 개발 환경 (Environment)

본 프로젝트는 리눅스 환경에서 작동하도록 진행하였습니다. 현 개발 OS가 윈도우이기 때문에 WSL환경에서 진행했습니다.

WSL은 Windows Subsystem for Linux로 윈도우에서 리눅스 환경을 지원해줍니다.



- WSL2(2.3.26.0), Linux 배포판(Ubuntu, Ubuntu-22.04)

- openjdk version "11.0.27" 2025-04-15

OpenJDK Runtime Environment (build 11.0.27+6-post-Ubuntu-0ubuntu122.04)

OpenJDK 64-Bit Server VM (build 11.0.27+6-post-Ubuntu-0ubuntu122.04, mixed mode, sharing)



3. 실행(컴파일) 방법

A. /mnt/c/workspace/UdpFileTransfer/src/main/java/com/kangwon로 이동한다.(2개의 CMD창을 열고 WSL 하에서 진행하였다. 각각 Receiver와 Sender를 따로 실행 시켰다. 두 CMD에서 모두 동일한 프로젝트 위치로 이동)

B. fileForSender 폴더에 전송하기 원하는 파일을 만든다.(e.g helloWorld.txt)

C. java receiver/FileReceiver.java 8888 <- 이 명령어를 통해 첫 번째 CMD에서 Receiver를 먼저 실행 시킨다.

D. java sender/FileSender.java "127.0.0.1" 8888 "helloWorld.txt" <- 이 명령어를 통해 두 번째 CMD에서 Sender를 실행 시킨다.



4. 결과

[ Receiver ]

Greeting
helloWorld.txt
From FileSender : Finish



----



[ Sender ]

From FileReceiver : OK
From FileReceiver : WellDone
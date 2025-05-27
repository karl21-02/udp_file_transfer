# udp_file_transfer

## 1. 개요 (Overview) 📜
이 프로그램에는 두 가지 자바 객체가 있습니다:

1.  **`FileSender`**: `fileForSender` 디렉토리 안에 있는 파일 중 사용자가 입력한 파일을 `FileReceiver`에게 전송합니다.
2.  **`FileReceiver`**: `FileSender`로부터 사용자가 입력한 파일을 전송받아 저장합니다.

또한, 파일 전송 전후로 UDP 소켓 연결을 테스트하기 위해 "Greeting", "Finish" 등 정해진 메시지를 주고받습니다.

---
## 2. 개발 환경 (Environment) 💻
본 프로젝트는 리눅스 환경에서 작동하도록 진행하였습니다. 현재 개발 OS가 윈도우이기 때문에 WSL 환경에서 진행했습니다.
WSL(Windows Subsystem for Linux)은 윈도우에서 리눅스 환경을 지원해주는 도구입니다.

* **WSL**: WSL2 (버전 2.3.26.0)
* **Linux 배포판**: Ubuntu (Ubuntu-22.04)
* **Java**:
    ```
    openjdk version "11.0.27" 2025-04-15
    OpenJDK Runtime Environment (build 11.0.27+6-post-Ubuntu-0ubuntu122.04)
    OpenJDK 64-Bit Server VM (build 11.0.27+6-post-Ubuntu-0ubuntu122.04, mixed mode, sharing)
    ```

---
## 3. 실행 방법 (How to Run) 🚀

**A. 작업 디렉토리로 이동**
   두 개의 터미널 창(CMD 또는 다른 터미널 에뮬레이터)을 열고, WSL 환경 하에서 다음 경로로 각각 이동합니다. (Receiver와 Sender를 별도로 실행합니다.)
   ```bash
       cd /mnt/c/workspace/UdpFileTransfer/src/main/java/com/kangwon
   ```

**B. 전송할 파일 준비**
   fileForSender 폴더에 전송하기 원하는 파일을 생성합니다.
   ```
     # /mnt/c/workspace/UdpFileTransfer/src/main/java/com/kangwon/sender/fileForSender/ 디렉토리에 생성
     # 예시: echo "Hello WSL!" > sender/fileForSender/helloWorld.txt
   ```
**C. Receiver 실행: 첫 번째 터미널 창에서 다음 명령어를 통해 FileReceiver를 먼저 실행시킵니다.**
     ```
         java receiver/FileReceiver.java 8888
     ```

**D. Sender 실행: 두 번째 터미널 창에서 다음 명령어를 통해 FileSender를 실행시킵니다.**
     ```
         java sender/FileSender.java "127.0.0.1" 8888 "helloWorld.txt"
     ```

** 4. 실행 결과 (Output Examples) 📊 **

Receiver 출력 예시
Greeting
helloWorld.txt
From FileSender : Finish

Sender 출력 예시
From FileReceiver : OK
From FileReceiver : WellDone

package com.company;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.Semaphore;


public class DownloadPhoto implements Runnable {
    private String link;
    private String path;
    private Semaphore semaphore;
    private int port;
    private String host;

    public DownloadPhoto(String link, Semaphore semaphore,int port,String host) {
        this.link = link;
        this.semaphore = semaphore;
        this.port = port;
        this.host = host;
        this.path = "./photos\\" + link.substring(link.lastIndexOf("/"), link.length());
    }

    @Override
    public void run() {

        try {

            Socket socket = getNewSocket();

            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.print("GET " + link + " HTTP/1.1\r\n");
            pw.print("Host: " + host + "\r\n");
            pw.print("Connection: keep-alive\r\n");
            pw.print("Accept-Language: ro,en\r\n");
            pw.print("DNT: 1\r\n");
            pw.print("Save-Data: on\r\n");
            pw.print("\r\n");
            pw.flush();


            System.out.println("\n Imaginea se descarca din: " + link);

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final FileOutputStream fileOutputStream = new FileOutputStream(path);//creem filul unde vom scri imaginea
            final InputStream inputStream = socket.getInputStream();





// Header end flag.
            boolean isHeaderEnded = false;//

            byte[] bytes = new byte[2048];
            int length;

            while ((length = inputStream.read(bytes)) != -1) {
                // Dacă sfârșitul antetului a fost deja atins, scriem octeții în fișier ca de obicei.

                    fileOutputStream.write(bytes, 0, length);

                    // Aceasta localizează sfârșitul antetului comparând octetul curent și următorii 3 octeți
                    // cu sfârșitul antetului HTTP „\r\n\r\n” care în reprezentarea întregului ar fi 13 10 13 10.
                    // Dacă se ajunge la sfârșitul antetului, indicatorul este setat la adevărat și datele rămase în
                    // currently buffered byte array is written into the file.
                else {
                    for (int i = 0; i < 2048; i++) {
                        if (bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                            isHeaderEnded = true;
                            fileOutputStream.write(bytes, i + 4, 2048 - i - 4);
                            break;
                        }
                    }
                }
            }
            inputStream.close();
            fileOutputStream.close();
            socket.close();


        } catch (IOException e) {
            System.out.println(e);
        }
        semaphore.release();
    }

    public Socket getNewSocket() throws IOException {

        if (this.port == 80) {
            SocketFactory socketFactory = (SocketFactory) javax.net.SocketFactory.getDefault();
            return (Socket) socketFactory.createSocket("me.utm.md", port);
        } else if (this.port == 443) {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("utm.md", port);
            sslSocket.startHandshake();
            return sslSocket;
        }

        return null;
    }
}

package com.company;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("De unde vreai sa extragi imaginile?");
        System.out.println("me.utm.md          a");
        System.out.println("utm.md             b");
        String answer = scanner.nextLine();

        if (answer.equals("a")) {
            String host = "me.utm.md";
            int port = 80;

            Socket socket = new Socket(host, port);
            String response = sendGetRequest(socket,host);
            Set<String> photos = extractLinks(response);
            startDownload(photos,port, host);


        } else if (answer.equals("b")) {
            String host = "utm.md";
            int port = 443;

            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
            String response = sendGetRequest(sslSocket, host);

            Set<String> photos = extractLinks (response);

            startDownload( photos,port, host);

        }

    }

    private static void startDownload(Set<String> allPhotosLinks,int port , String host) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        final Semaphore semaphore = new Semaphore(2);

        List<Runnable> taskList = new LinkedList<>();

        for (String link : allPhotosLinks) {

            Runnable task = new DownloadPhoto(link, semaphore, port, host);
            taskList.add(task);
        }

        for (Runnable task : taskList) {
            executor.execute(task);
        }
        executor.shutdown();

        printFinalMessage(executor);

    }

    public static Set<String> extractLinks (String response) {
        Pattern pattern = Pattern.compile("<img\\s+[^>]*src=\"([^\"]*)\"[^>]*>");

        Set<String> allPhotos = new HashSet<>();

        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            allPhotos.add(matcher.group(1));
        }

        Set<String> allPhotosLinks = new HashSet<>();
        allPhotos.forEach((photo) -> {

            if (photo.endsWith(".jpg") || photo.endsWith(".png") || photo.endsWith(".gif")) {

                if (photo.startsWith("http:") || photo.startsWith("https:")) {
                    allPhotosLinks.add(photo);
                } else {
                    allPhotosLinks.add("http://me.utm.md/" + photo);
                }

            }
        });
        allPhotos.clear();

        return allPhotosLinks;
    }

    public static void printFinalMessage(ThreadPoolExecutor executor) {
        try {
            if (executor.awaitTermination(3, TimeUnit.MINUTES)) {
                System.out.println("\n  S-a descarcat cu succes");
            } else {
                System.out.println("\n Timpul a expirat ");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String sendGetRequest(Socket socket, String host) throws IOException {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String response = "";
        String outStr;
        printWriter.print("GET / HTTP/1.1\r\n");
        printWriter.print("Host: " + host + "\r\n");
        printWriter.print("Connection: keep-alive\r\n");
        printWriter.print("Accept-Language: ro,en\r\n");
        printWriter.print("DNT: 1\r\n");
        printWriter.print("Save-Data: <sd-token>\r\n");
        printWriter.print("\r\n");
        printWriter.flush();
        // printWritter transmite date in socket

        while((outStr = bufferedReader.readLine()) != null){
            response += outStr + "\n";
        }
        socket.shutdownInput();
        bufferedReader.close();
        printWriter.close();

        return response;

    }

}


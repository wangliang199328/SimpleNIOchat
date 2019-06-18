package com.cn.study.chat.client;

import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * @description:
 * @author: wangl
 * @date: 2019-06-17 15:44
 */
public class ChatClient {

    private final String contentSplit = "#@#";
    private final String userNameExists = "当前用户名已存在.";
    private Selector selector;
    private SocketChannel client;
    private Charset charset = Charset.forName("UTF-8");
    private String userName = "";


    public ChatClient() {
        try {
            selector = Selector.open();
            client = SocketChannel.open(new InetSocketAddress("localhost", 8081));
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(){
        new Reader().start();

        new Writer().start();
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }

    //读取服务端消息
    class Reader extends Thread{

        @Override
        public void run() {
            while (true){
                try {
                    int s = selector.select();
                    if (s<=0) continue;

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        SelectionKey next = iterator.next();
                        iterator.remove();

                        //接受server端消息
                        SocketChannel channel = (SocketChannel) next.channel();

                        ByteBuffer bf = ByteBuffer.allocate(1024);
                        StringBuilder sb = new StringBuilder();

                        while (channel.read(bf)>0) {
                            bf.flip();
                            sb.append(charset.decode(bf));
                        }

                        if (sb.length()>0){
                            if (sb.toString().equals(userNameExists)){
                                userName = "";
                            }

                            System.out.println(sb);
                            next.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //想服务端发送消息
    class Writer extends Thread{

        @Override
        public void run() {
            try {
                Scanner sc = new Scanner(System.in);
                while (sc.hasNext()){
                    String s = sc.nextLine();

                    if (StringUtils.isEmpty(s)) continue;

                    String message;
                    if (StringUtils.isEmpty(userName)){
                        userName = s;
                        message = s+contentSplit;
                    }
                    else {
                        message = userName+contentSplit+s;
                    }

                    client.write(charset.encode(message));
                }
                sc.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

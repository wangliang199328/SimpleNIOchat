package com.cn.study.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @description:
 * @author: wangl
 * @date: 2019-06-17 11:46
 */
public class ChatServer {
    private final Integer port = 8081;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");
    private final String contentSplit = "#@#";
    private final String userNameExists = "当前用户名已存在.";
    private final String welcomeFormat = "欢迎 %s 进入房间. 当前在线人数 : %d.";
    private final String messageFormat = "%s 说: %s";
    private HashSet<String> users = new HashSet<>();

    public ChatServer() {
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);

            log.info("chat client 启动完成,监听端口 : {}",port);
            System.out.println("chat client 启动完成,监听端口 : "+port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatServer().listen();
    }

    /**
     * 功能描述:
     * 监听
     *
     * @return : void
     * @author : wangl
     * @date : 2019/6/17 11:57
     */
    public void listen(){

        while (true){
            try {
                int num = selector.select();
                if (num<=0) continue;

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    //process
                    if (key.isAcceptable()){
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel client = channel.accept();
                        client.configureBlocking(false);
                        //将当前channel注册进selector中,并设置为读取状态
                        client.register(selector,SelectionKey.OP_READ);

                        //设置此channel为准备接受其他客户端消息的状态
                        key.interestOps(SelectionKey.OP_ACCEPT);

                        client.write(charset.encode("请输入姓名."));
                    }

                    if (key.isReadable()){
                        SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
                        StringBuilder sb = new StringBuilder();
                        try {
                            while (channel.read(byteBuffer)>0){
                                byteBuffer.flip();
                                sb.append(charset.decode(byteBuffer));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();

                            key.cancel();
                            if (key.channel()!=null){
                                key.channel().close();
                            }
                        }

                        //处理接收到的消息
                        if (sb.length()>0){
                            String content = sb.toString();
                            String[] arr = content.split(contentSplit);

                            key.interestOps(SelectionKey.OP_READ);

                            //1.注册用户名
                            if (arr.length==1){
                                String name = arr[0];
                                if (users.contains(name)) {
                                    channel.write(charset.encode(userNameExists));
                                }else {
                                    users.add(name);

                                    //广播当前所有用户 ：欢迎xxx进入房间
                                    //1 获取当前在线用户数
                                    int userNum = getUserNum();
                                    String welcome = String.format(welcomeFormat, name, userNum);

                                    //2 广播所有人
                                    sendUsers(null,welcome);
                                }
                            }

                            //2.正常聊天
                            else if (arr.length>1){
                                String name = arr[0];
                                String message = arr[1];
                                message = String.format(messageFormat,name,message);
                                sendUsers(channel,message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendUsers(SocketChannel client, String welcome) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey sk : keys) {
            try {
                Channel channel = sk.channel();
                if (channel instanceof SocketChannel&& channel != client){
                    ((SocketChannel) channel).write(charset.encode(welcome));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getUserNum() {
        int num = 0;
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            SelectableChannel channel = key.channel();
            if (channel instanceof SocketChannel){
                num++;
            }
        }
        return num;
    }

}

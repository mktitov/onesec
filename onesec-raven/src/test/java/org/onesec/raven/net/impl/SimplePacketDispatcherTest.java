/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.net.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class SimplePacketDispatcherTest {
    private AtomicBoolean serverReadyToReceive = new AtomicBoolean(false);
    
//    @Test
    public void selectorTest() throws Exception {
        ServerThread thread = new ServerThread();
        thread.start();
        sendData();
        Thread.sleep(5000);
    }
    
//    public void 
    
    private class ServerThread extends Thread {
        private final AtomicBoolean stopFlag = new AtomicBoolean(false);
        
        public void stopSelector() {
            stopFlag.compareAndSet(false, true);
        }
        
        @Override public void run() {
            //create selector
            try {
                System.out.println(">> Server. Creating selector");
                Selector selector = Selector.open();
                try {
                    System.out.println(">> Server. Creating server socket channel");
                    ServerSocketChannel serverSocket = ServerSocketChannel.open();
                    serverSocket.configureBlocking(false);
                    System.out.println(">> Server. Binding server socket to the port 1234");
                    serverSocket.socket().bind(new InetSocketAddress(1234));
                    serverReadyToReceive.set(true);
                    System.out.println(">> Server. Registering in the selector");
                    serverSocket.register(selector, SelectionKey.OP_ACCEPT);
                    while (!stopFlag.get()) {
                        int selectedCount = selector.select(500);
                        System.out.println(">> Server. Selections count: "+selectedCount);
                        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                        while (keys.hasNext()) {
                            SelectionKey key = keys.next();
                            if (key.isAcceptable()) {
                                System.out.println(">> Server. Accepting incoming connection");
                                SocketChannel channel = ((ServerSocketChannel)key.channel()).accept();
                                channel.configureBlocking(false);
                                channel.register(selector, SelectionKey.OP_READ);
                                key.channel().close();
                                keys.remove();
                            } else  if (key.isReadable()) {
                                System.out.println(">> Server. Reading data from socket");
                            }
                        }
                    }
                } finally {
                    selector.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendData() throws Exception {
        while(!serverReadyToReceive.get())
            Thread.sleep(10);
        System.out.println("Sending data to the server");
        InetAddress addr = Inet4Address.getLocalHost();
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(addr, 1234));
//        ByteBuffer buffer = ByteBuffer.allocate(4);
//        buffer.put((byte)1);
//        buffer.put((byte)2);
//        buffer.put((byte)3);
//        buffer.put((byte)4);
//        buffer.flip();
//        int count = channel.write(buffer);
//        System.out.println("Written "+count+" bytes");
    }
}

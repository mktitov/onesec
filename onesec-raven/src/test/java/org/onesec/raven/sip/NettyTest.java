/*
 * Copyright 2016 Mikhail Titov.
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
package org.onesec.raven.sip;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectDecoder;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class NettyTest {
    public enum State {
        ST_1, ST_2, ST_3
    }
    
    @Test
    public void test() {
        State state = State.ST_1;
        switch(state) {
            case ST_1: 
                System.out.println("ST_1");
            case ST_2: 
                System.out.println("ST_2");
                break;
            case ST_3: 
                System.out.println("ST_3");
                
        }
    }
    
//    @Test
//    public void test() throws Exception {        
//        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(8);
//        final ChannelFuture channelFuture = new Bootstrap()
//                .group(eventLoopGroup)
//                .channel(NioDatagramChannel.class)
//                .handler(new SipObjectDecoder())
//                .bind(OnesecRavenTestCase.getInterfaceAddress(), 5061);
//        
//    }
    
    private class SipObjectDecoder extends HttpObjectDecoder {

        @Override
        protected boolean isDecodingRequest() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected HttpMessage createMessage(String[] strings) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected HttpMessage createInvalidMessage() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

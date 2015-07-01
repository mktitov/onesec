/*
 * Copyright 2013 Mikhail Titov.
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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.net.NettyEventLoopGroupProvider;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class NettyNioEventLoopGroupNode extends BaseNode implements NettyEventLoopGroupProvider {
    @NotNull @Parameter(defaultValue = "4")
    private Integer threadsCount;
    
    private AtomicReference<NioEventLoopGroup> group;

    @Override
    protected void initFields() {
        super.initFields(); 
        group = new AtomicReference<NioEventLoopGroup>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        group.set(new NioEventLoopGroup(threadsCount));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        NioEventLoopGroup _group = group.getAndSet(null);
        if (_group!=null)
            _group.shutdownGracefully();
    }
    

    public Integer getThreadsCount() {
        return threadsCount;
    }

    public void setThreadsCount(Integer threadsCount) {
        this.threadsCount = threadsCount;
    }

    public EventLoopGroup getEventLoopGroup() {
        return group.get();
    }
}

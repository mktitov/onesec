/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.TestSchedulerNode;
import org.onesec.raven.ivr.BufferCache;
import org.raven.tree.Node;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class BufferCacheNodeTest extends OnesecRavenTestCase
{
    @Test
    public void initNodeTest(){
        BufferCacheNode cacheNode = new BufferCacheNode();
        tree.getRootNode().addAndSaveChildren(cacheNode);
        assertEquals(BufferCacheImpl.DEFAULT_MAX_CACHE_IDLE_TIME, cacheNode.getMaxCacheIdleTime().longValue());
    }

    @Test
    public void startNodeTest() throws Exception {
        TestSchedulerNode scheduler = new TestSchedulerNode();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);

        BufferCacheNode cacheNode = new BufferCacheNode();
        tree.getRootNode().addAndSaveChildren(cacheNode);
        cacheNode.setMaxCacheIdleTime(1l);
        cacheNode.setScheduler(scheduler);
        assertTrue(cacheNode.start());
        assertEquals(1l, registry.getService(BufferCache.class).getMaxCacheIdleTime());
    }

    @Test
    public void nodeAutoCreationTest() {
        Node node = tree.getRootNode().getChildren(SystemNode.NAME)
                .getChildren(ServicesNode.NAME).getChildren(BufferCacheNode.NAME);
        assertNotNull(node);
        assertTrue(node instanceof BufferCacheNode);
    }

}
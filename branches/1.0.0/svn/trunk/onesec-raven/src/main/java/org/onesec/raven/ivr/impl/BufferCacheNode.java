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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.BuffersCacheEntity;
import org.raven.annotations.Parameter;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class BufferCacheNode extends BaseNode implements Viewable, Schedulable {
    public static final String MAX_CACHE_IDLE_TIME_ATTR = "maxCacheIdleTime";
    public final static String NAME = "Buffers cache";

    @Service
    private static BufferCache bufferCache;

    @NotNull @Parameter
    private Long maxCacheIdleTime;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler scheduler;

    public BufferCacheNode() {
        super(NAME);
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        NodeAttribute attr = getNodeAttribute(MAX_CACHE_IDLE_TIME_ATTR);
        if (attr.getValue()==null) {
            attr.setValue(""+bufferCache.getMaxCacheIdleTime());
            attr.save();
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (maxCacheIdleTime>0)
            bufferCache.setMaxCacheIdleTime(maxCacheIdleTime);
        else
            throw new Exception("The value of the (maxCacheIdleTime) attribute must be greate than zero");
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Long getMaxCacheIdleTime() {
        return maxCacheIdleTime;
    }

    public void setMaxCacheIdleTime(Long maxCacheIdleTime) {
        this.maxCacheIdleTime = maxCacheIdleTime;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(4);

        List<String> silentBuffersKeys = bufferCache.getSilentBuffersKeys();
        Collections.sort(silentBuffersKeys);
        TableImpl table = new TableImpl(new String[]{"Silent buffer key"});
        for (String key: silentBuffersKeys)
            table.addRow(new Object[]{key});
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>Silent buffers</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        table = new TableImpl(new String[]{"Key", "Codec", "Packet size", "Checksum"
                , "Buffers count", "Usage count","Idle time (sec)", "Invalid?"});
        List<BuffersCacheEntity> entities = bufferCache.getCacheEntities();
        Collections.sort(entities, new SortByIdleTime());
        for (BuffersCacheEntity e: entities)
            table.addRow(new Object[]{e.getKey(), e.getCodec(), e.getPacketSize(), e.getChecksum()
                    , e.getBuffersCount(), e.getUsageCount(), e.getIdleTime(), e.isInvalid()});
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>Buffers caches</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        
        return vos;
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public void executeScheduledJob(Scheduler scheduler) {
        bufferCache.removeOldCaches();
    }

    private class SortByIdleTime implements Comparator<BuffersCacheEntity>{
        public int compare(BuffersCacheEntity o1, BuffersCacheEntity o2) {
            return (int)(o2.getIdleTime()-o1.getIdleTime());
        }
    }
}

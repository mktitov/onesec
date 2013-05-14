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
package org.onesec.raven.ivr.conference.impl;

import fj.data.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceException;
import org.onesec.raven.ivr.conference.ConferenceInitiator;
import org.onesec.raven.ivr.conference.ConferenceManager;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import static java.lang.Math.*;
/**
 *
 * @author Mikhail Titov
 */
public class ConferenceManagerImpl extends BaseNode implements ConferenceManager {
    private static final int LOCK_TIMEOUT = 1000;
    
    @NotNull @Parameter
    private Integer channelsCount;
    
    private Lock lock;

    @Override
    protected void initFields() {
        super.initFields();
        lock = new ReentrantLock();
    }

    public Conference createConference(String name, Date fromDate, Date toDate, int channelCount, ConferenceInitiator initiator) throws ConferenceException {
        try {
            if (lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    long curdate = System.currentTimeMillis();
                    if (fromDate.after(toDate)) 
                        throw new ConferenceException("from date can't be greater to date");
//                    if (fromDate.getTime())
                    return null;
                } finally {
                    lock.unlock();
                }
            } else throw new ConferenceException("Timeout while wating for conference creation lock");
        } catch (Throwable e) {
            throw new ConferenceException("Error creating conference. "+(e.getMessage()==null?"":e.getMessage()));
        }
    }
    
    static boolean checkInterval(final long fd, final long td, final List<Conference> conferences
            , final int maxChannels, final int reqChannels) 
    {
        if (reqChannels>maxChannels) return false;
        else if (conferences.isEmpty()) return true;
        else {
            final Conference conf = conferences.head();
            final long[] intersec = getIntersection(fd, td, conf.getStartTime().getTime(), 
                    conf.getEndTime().getTime());
            if (intersec!=null && !checkInterval(intersec[0], intersec[1], conferences.tail(), maxChannels-conf.getChannelsCount(), reqChannels))
                return false;
            return checkInterval(fd, td, conferences.tail(), maxChannels, reqChannels);
        }
    }
    
    static long[] getIntersection(long fd1, long td1, long fd2, long td2) {
        return isDatesIntersects(fd1, td1, fd2, td2)? new long[]{max(fd1,fd2), min(td1,td2)} : null;
    }
    
    static boolean isDatesIntersects(long fd1, long td1, long fd2, long td2) {
        return datesBetween(fd1, fd2, td2) || datesBetween(td1, fd2, td2) 
                || datesBetween(fd2, fd1, td1) || datesBetween(td2, fd1, td1);
    }
    
    static boolean datesBetween(long date, long fd, long td) {
        return date>=fd && date<=td;
    }

    public void removeConference(int conferenceId) throws ConferenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getChannelsCount() {
        return channelsCount;
    }

    public void setChannelsCount(Integer channelsCount) {
        this.channelsCount = channelsCount;
    }
    
}

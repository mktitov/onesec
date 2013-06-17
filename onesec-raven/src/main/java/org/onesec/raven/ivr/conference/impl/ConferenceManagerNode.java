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
import java.io.File;
import static java.lang.Math.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.Constants;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceException;
import org.onesec.raven.ivr.conference.ConferenceInitiator;
import org.onesec.raven.ivr.conference.ConferenceManager;
import org.onesec.raven.ivr.conference.ConferenceSessionListener;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.TreeException;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ConferenceManagerNode extends BaseNode implements ConferenceManager, Schedulable, DataSource {
    private static final int LOCK_TIMEOUT = 1000;
    public static final String EVENT_TYPE_FIELD = "eventType";
    public static final String CONFERENCE_FIELD = "conference";
    public static final String CONFERENCE_ARCHIVED_EVENT = "CONFERENCE_ARCHIVED";
    
    @NotNull @Parameter
    private Integer channelsCount;
    
    @NotNull @Parameter(defaultValue="60")
    private Integer maxConferenceDuration;
    
    @NotNull @Parameter(defaultValue="14")
    private Integer maxPlanDays;
    
    @NotNull @Parameter(defaultValue="10")
    private Integer minChannelsPerConference;
    
    @NotNull @Parameter(defaultValue="5")
    private Integer accessCodeLength;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="0")
    private Integer noiseLevel;
    
    @NotNull @Parameter(defaultValue="3")
    private Double maxGainCoef;
    
    @NotNull @Parameter
    private String recordingStoragePath;
    
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler archiveScheduler;
    
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE,
            defaultValue=Constants.CONFERENCE_STOP_AFTER_1MIN)
    private AudioFile oneMinuteLeftAudio;
    
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE,
            defaultValue=Constants.CONFERENCE_STOPPED)
    private AudioFile conferenceStoppedAudio;
    
    private Lock lock;
    private PlannedConferencesNode plannedNode;
    private ConferencesArchiveNode archiveNode;
    private AtomicReference<ConferenceNode> processingConference;
    private File recordingStorageFile;
    private AtomicBoolean scheduling;

    @Override
    protected void initFields() {
        super.initFields();
        lock = new ReentrantLock();
        processingConference = new AtomicReference<ConferenceNode>();
        scheduling = new AtomicBoolean();
        recordingStorageFile = null;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
        recordingStorageFile = getOrCreatePath(recordingStoragePath);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        
    }
    
    private void initNodes(boolean start) {
        archiveNode = (ConferencesArchiveNode) getNode(ConferencesArchiveNode.NAME);
        if (archiveNode==null) {
            archiveNode = new ConferencesArchiveNode();
            addAndSaveChildren(archiveNode);
        }
        if (start) archiveNode.start();
        plannedNode = (PlannedConferencesNode) getNode(PlannedConferencesNode.NAME);
        if (plannedNode==null) {
            plannedNode = new PlannedConferencesNode();
            addAndSaveChildren(plannedNode);
        }
        if (start) plannedNode.start();
    }

    public void executeScheduledJob(Scheduler scheduler) {
        if (!scheduling.compareAndSet(false, true))
            return;
        try {
            final long curTime = System.currentTimeMillis();
            for (ConferenceNode conf: NodeUtils.getChildsOfType(plannedNode, ConferenceNode.class))
                if (conf.getEndTime().getTime()<curTime && !conf.isActive())
                    archiveConference(conf);
        } finally {
            scheduling.compareAndSet(true, false);
        }
    }
    
    private void archiveConference(ConferenceNode conf) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug(String.format(
                    "Archiving conference: name=(%s); node=(%s)", conf.getConferenceName(), conf));
        String[] path = new SimpleDateFormat("yyyy MM dd").format(conf.getStartTime()).split(" ");
        Node container = getOrCreateArchivePath(archiveNode, path, 0);
        try {
            tree.move(conf, container, null);
            sendConferenceArchived(conf);
        } catch (TreeException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(String.format("Conference (%s, %s) archiving error", conf.getName(), conf), ex);
        }
    }
    
    private void sendConferenceArchived(ConferenceNode conf) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put(EVENT_TYPE_FIELD, CONFERENCE_ARCHIVED_EVENT);
        event.put(CONFERENCE_FIELD, conf);
        DataSourceHelper.sendDataToConsumers(executor, this, event, new DataContextImpl());
    }
    
    private Node getOrCreateArchivePath(final Node owner, final String[] path, final int pos) {
        if (pos==path.length) return owner;
        else {
            Node child = owner.getNode(path[pos]);
            if (child==null) {
                child = new ContainerNode(path[pos]);
                owner.addAndSaveChildren(child);
                child.start();
            }
            return getOrCreateArchivePath(child, path, pos+1);
        }
    }
    
    private File getOrCreatePath(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs())
                throw new Exception(String.format(
                        "Error creating directory (%s)", file.getAbsolutePath()));
        } else if (!file.isDirectory())
                throw new Exception(String.format(
                        "Path (%s) exists but it is not a directory", file.getAbsolutePath()));
        return file;
    }

    public File getRecordingPath(Conference conference) throws Exception {
        if (!isStarted())
            throw new Exception("Conference manager not started");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd/"+conference.getId()+"/");
        return getOrCreatePath(recordingStorageFile.getAbsolutePath()+"/"+fmt.format(conference.getStartTime()));
    }
    
    public PlannedConferencesNode getPlannedConferencesNode() {
        return plannedNode;
    }
    
    public ConferencesArchiveNode getConferencesArchiveNode() {
        return archiveNode;
    }

    public Conference createConference(final String name, final Date fromDate, final Date toDate,  
            final int channelCount, final ConferenceInitiator initiator) throws ConferenceException 
    {
        if (!isStarted()) return null;
        try {
            return executeInLock(new Ask<Conference>() {
                public Conference run() throws Exception {
                    checkConference(name, fromDate, toDate, channelCount, null);
                    return createConferenceNode(name, fromDate, toDate, channelCount, initiator);
                }
            });
        } catch (ConferenceException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(String.format(
                        "Error creating conference: name=%s; fromDate=%s; toDate=%s; channelsCount=%s; initiator=%s",
                        name, fromDate, toDate, channelCount, initiator), e);
            throw (ConferenceException)e;
        }
    }
    
    private void checkConference(String name, Date fromDate, Date toDate, int channelCount, Conference skipConf) 
            throws ConferenceException 
    {
        if (name==null || name.trim().isEmpty()) 
            throw new ConferenceException(ConferenceException.NULL_CONFERENCE_NAME);
        if (fromDate==null)
            throw new ConferenceException(ConferenceException.NULL_FROM_DATE);
        if (toDate==null)
            throw new ConferenceException(ConferenceException.NULL_TO_DATE);
        if (channelCount<minChannelsPerConference)
            throw new ConferenceException(ConferenceException.INVALID_CHANNELS_COUNT);
        checkDates(fromDate, toDate);
        List<Conference> conferences = List.list(
                NodeUtils.getChildsOfType(plannedNode, Conference.class).toArray(new Conference[]{}));
        if (!checkChannels(fromDate.getTime(), toDate.getTime(), conferences, this.channelsCount, channelCount, skipConf))
            throw new ConferenceException(ConferenceException.NOT_ENOUGH_CHANNELS);
    }
    
    public void checkConferenceNode(final Conference conf) throws ConferenceException {
        if (!isStarted()) throw new ConferenceException(ConferenceException.CONFERENCE_MANAGER_STOPPED);
        else if (processingConference.get()==conf) return;
        else executeInLock(new Tell() {
            public void run() throws Exception {
                checkConference(conf.getConferenceName(), conf.getStartTime(), conf.getEndTime(), 
                        conf.getChannelsCount(), conf);
            }
        });
    }

    public void join(final IvrEndpointConversation conversation, final String conferenceId, 
            final String accessCode, final ConferenceSessionListener listener) 
    {        
        try {
            ConferenceNode conference = (ConferenceNode) plannedNode.getNodeById(Integer.parseInt(conferenceId));
            if (conference==null) {
                sendInvalidIdOrAccessCode(listener, conferenceId);
                return;
            }
            conference.join(conversation, accessCode, listener);
        } catch (NumberFormatException e) {
            sendInvalidIdOrAccessCode(listener, conferenceId);
        }
    }
    
    private<T> T executeInLock(Task<T> task) throws ConferenceException {
        try {
            if (lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    if (task instanceof Ask) return ((Ask<T>)task).run();
                    else {
                        ((Tell)task).run();
                        return null;
                    }
                } finally {
                    lock.unlock();
                }
            } else throw new ConferenceException(ConferenceException.CONFERENCE_MANAGER_BUSY);
        } catch (Throwable e) {
            if (!(e instanceof ConferenceException))
                throw new ConferenceException("Error creating conference. "+(e.getMessage()==null?"":e.getMessage()), e);
            else throw (ConferenceException)e;
        }
    }
    
    static boolean checkChannels(final long fd, final long td, final List<Conference> conferences
            , final int maxChannels, final int reqChannels, final Conference skipConf) 
    {
        if (reqChannels>maxChannels) return false;
        else if (conferences.isEmpty()) return true;
        else {
            final Conference conf = conferences.head();
            if (conf!=skipConf) {
                final long[] intersec = getIntersection(fd, td, conf.getStartTime().getTime(), conf.getEndTime().getTime());
                if (intersec!=null && !checkChannels(intersec[0], intersec[1], conferences.tail(), maxChannels-conf.getChannelsCount(), reqChannels, skipConf))
                    return false;
            }
            return checkChannels(fd, td, conferences.tail(), maxChannels, reqChannels, skipConf);
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

    public Integer getMaxConferenceDuration() {
        return maxConferenceDuration;
    }

    public void setMaxConferenceDuration(Integer maxConferenceDuration) {
        this.maxConferenceDuration = maxConferenceDuration;
    }

    public Integer getMaxPlanDays() {
        return maxPlanDays;
    }

    public void setMaxPlanDays(Integer maxPlanDays) {
        this.maxPlanDays = maxPlanDays;
    }

    public Integer getMinChannelsPerConference() {
        return minChannelsPerConference;
    }

    public void setMinChannelsPerConference(Integer minChannelsPerConference) {
        this.minChannelsPerConference = minChannelsPerConference;
    }

    public Integer getAccessCodeLength() {
        return accessCodeLength;
    }

    public void setAccessCodeLength(Integer accessCodeLength) {
        this.accessCodeLength = accessCodeLength;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Integer getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(Integer noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Double getMaxGainCoef() {
        return maxGainCoef;
    }

    public void setMaxGainCoef(Double maxGainCoef) {
        this.maxGainCoef = maxGainCoef;
    }

    public String getRecordingStoragePath() {
        return recordingStoragePath;
    }

    public void setRecordingStoragePath(String recordingStoragePath) {
        this.recordingStoragePath = recordingStoragePath;
    }

    public Scheduler getArchiveScheduler() {
        return archiveScheduler;
    }

    public void setArchiveScheduler(Scheduler archiveScheduler) {
        this.archiveScheduler = archiveScheduler;
    }

    public AudioFile getOneMinuteLeftAudio() {
        return oneMinuteLeftAudio;
    }

    public void setOneMinuteLeftAudio(AudioFile oneMinuteLeftAudio) {
        this.oneMinuteLeftAudio = oneMinuteLeftAudio;
    }

    public AudioFile getConferenceStoppedAudio() {
        return conferenceStoppedAudio;
    }

    public void setConferenceStoppedAudio(AudioFile conferenceStoppedAudio) {
        this.conferenceStoppedAudio = conferenceStoppedAudio;
    }

    private void checkDates(Date fromDate, Date toDate) throws ConferenceException {
        final Date curDate = new Date();
        if (fromDate.after(toDate)) 
            throw new ConferenceException(ConferenceException.FROM_DATE_AFTER_TO_DATE);
        if (curDate.after(toDate))
            throw new ConferenceException(ConferenceException.DATE_AFTER_CURRENT_DATE);
        if (toDate.getTime()-fromDate.getTime() > maxConferenceDuration*60*1000)
            throw new ConferenceException(ConferenceException.CONFERENCE_TO_LONG);
        if (fromDate.getTime()-TimeUnit.DAYS.toMillis(maxPlanDays) > curDate.getTime())
            throw new ConferenceException(ConferenceException.CONFERENCE_TO_FAR_IN_FUTURE);
    }
    
    private Conference createConferenceNode(String name, Date fromDate, Date toDate, int channelsCount, 
            ConferenceInitiator initiator) 
    {
        ConferenceNode conf = new ConferenceNode();
        conf.setName(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(fromDate));
        plannedNode.addAndSaveChildren(conf);
        conf.setName(conf.getName()+" "+conf.getId());
        conf.save();
        conf.setConferenceName(name);
        conf.setStartTime(fromDate);
        conf.setEndTime(toDate);
        conf.setChannelsCount(channelsCount);
        conf.setAccessCode(generateAccessCode(accessCodeLength));
        conf.setLogLevel(getLogLevel());
        processingConference.set(conf);
        try {
            conf.start();
        } finally {
            processingConference.set(null);
        }
        return conf;
    }
    
    public static String generateAccessCode(int numOfDigits) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<numOfDigits; ++i)
            buf.append((int)(random()*10));
        return buf.toString();
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this dataSource");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    private void sendInvalidIdOrAccessCode(final ConferenceSessionListener listener, final String conferenceId) {
        executor.executeQuietly(new AbstractTask(this, "Pushing message 'invalid conference id' to conversation") {
            @Override public void doRun() throws Exception {
                listener.invalidConferenceId(conferenceId);
            }
        });
    }
    
    private interface Task<T>{}
    
    private interface Tell extends Task {
        public void run() throws Exception;
    }
    
    private interface Ask<T> extends Task<T> {
        public T run() throws Exception;
    }
    

}

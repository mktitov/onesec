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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.onesec.raven.ivr.conference.Conference;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=PlannedConferencesNode.class)
public class ConferenceNode extends BaseNode implements Conference {
    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    @NotNull @Parameter
    private String conferenceName;
    
    @NotNull @Parameter
    private String accessCode;
    
    @NotNull @Parameter
    private String startTimeStr;
    
    @NotNull @Parameter
    private String endTimeStr;
    
    @NotNull @Parameter
    private Integer channelsCount;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean callbackAllowed;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean manualJoinAllowed;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean joinUnregisteredParticipantAllowed;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean recordConference;
    
    private ConferenceManagerNode getManager() {
        return (ConferenceManagerNode) getParent().getParent();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getManager().checkConferenceNode(this);
    }

    public String getConferenceName() {
        return conferenceName;
    }

    public void setConferenceName(String conferenceName) {
        this.conferenceName = conferenceName;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getStartTimeStr() {
        return startTimeStr;
    }

    public void setStartTimeStr(String startTimeStr) {
        this.startTimeStr = startTimeStr;
    }

    public String getEndTimeStr() {
        return endTimeStr;
    }

    public void setEndTimeStr(String endTimeStr) {
        this.endTimeStr = endTimeStr;
    }

    public Date getStartTime() {
        return parseDate(startTimeStr);
    }

    public void setStartTime(Date startTime) {
        this.startTimeStr = formatDate(startTime);
    }

    public Date getEndTime() {
        return parseDate(endTimeStr);
    }

    public void setEndTime(Date endTime) {
        this.endTimeStr = formatDate(endTime);
    }

    public Integer getChannelsCount() {
        return channelsCount;
    }

    public void setChannelsCount(Integer channelsCount) {
        this.channelsCount = channelsCount;
    }

    public Boolean getCallbackAllowed() {
        return callbackAllowed;
    }

    public void setCallbackAllowed(Boolean callbackAllowed) {
        this.callbackAllowed = callbackAllowed;
    }

    public Boolean getManualJoinAllowed() {
        return manualJoinAllowed;
    }

    public void setManualJoinAllowed(Boolean manualJoinAllowed) {
        this.manualJoinAllowed = manualJoinAllowed;
    }

    public Boolean getJoinUnregisteredParticipantAllowed() {
        return joinUnregisteredParticipantAllowed;
    }

    public void setJoinUnregisteredParticipantAllowed(Boolean joinUnregisteredParticipantAllowed) {
        this.joinUnregisteredParticipantAllowed = joinUnregisteredParticipantAllowed;
    }

    public Boolean getRecordConference() {
        return recordConference;
    }

    public void setRecordConference(Boolean recordConference) {
        this.recordConference = recordConference;
    }

    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(date);
        } catch (ParseException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error parsing date from string: "+date);
            return null;
        }
    }
    
    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}

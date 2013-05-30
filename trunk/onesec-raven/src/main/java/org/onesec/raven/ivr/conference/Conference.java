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
package org.onesec.raven.ivr.conference;

import java.util.Date;

/**
 *
 * @author Mikhail Titov
 */
public interface Conference {
    int getId();
    String getAccessCode();
    String getConferenceName();
    Date getStartTime();
    Date getEndTime();
    Integer getChannelsCount();
//    List<Participant> getParticipants();
//    void addParticipant(Participant participant);
//    void removeParticipant(Participant participant);
    
    Boolean getCallbackAllowed();
    void setCallbackAllowed(Boolean value);
    
//    Boolean isInviteAllowed();
//    void setInviteAllowed(Boolean value);
    
    Boolean getManualJoinAllowed();
    void setManualJoinAllowed(Boolean value);
    
    Boolean getJoinUnregisteredParticipantAllowed();
    void setJoinUnregisteredParticipantAllowed(Boolean value);
    
    Boolean getRecordConference();
    void setRecordConference(Boolean value);    
    
    public Integer getAutoStopRecorderAfter();
    public void setAutoStopRecorderAfter(Integer autoStopRecorderAfter); 
    
    void update();    
}

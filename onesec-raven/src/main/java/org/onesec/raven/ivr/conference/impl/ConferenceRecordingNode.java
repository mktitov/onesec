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

import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceRecordingNode extends BaseNode {
    public final static String RECORDING_START_TIME_ATTR = "recordingStartTime";
    public final static String RECORDING_END_TIME_ATTR = "recordingEndTime";
    public final static String RECORDING_DURATION_ATTR = "recordingDuration";
    public final static String RECORDING_FILE_ATTR = "recordingFile";
    
    @Parameter
    private String recordingStartTime;
    @Parameter
    private String recordingEndTime;
    @Parameter
    private Integer recordingDuration;
    @Parameter
    private String recordingFile;

    public String getRecordingStartTime() {
        return recordingStartTime;
    }

    public void setRecordingStartTime(String recordingStartTime) {
        this.recordingStartTime = recordingStartTime;
    }

    public String getRecordingEndTime() {
        return recordingEndTime;
    }

    public void setRecordingEndTime(String recordingEndTime) {
        this.recordingEndTime = recordingEndTime;
    }

    public Integer getRecordingDuration() {
        return recordingDuration;
    }

    public void setRecordingDuration(Integer recordingDuration) {
        this.recordingDuration = recordingDuration;
    }

    public String getRecordingFile() {
        return recordingFile;
    }

    public void setRecordingFile(String recordingFile) {
        this.recordingFile = recordingFile;
    }
}

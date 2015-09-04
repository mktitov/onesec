/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = RecordSchemasNode.class)
public class CallCdrRecordSchemaNode extends RecordSchemaNode {
    public final static String ID = "id";
    public final static String ENDPOINT_ADDRESS = "endpointAddress";
    public final static String CALLING_NUMBER = "callingNumber";
    public final static String CALLED_NUMBER = "calledNumber";
    public final static String LAST_REDIRECTED_NUMBER = "lastRedirectedNumber";
    public final static String CALL_START_TIME = "callStartTime";
    public final static String CONNECTION_ESTABLISHED_TIME = "connectionEstablishedTime";
    public final static String CONVERSATION_START_TIME = "conversationStartTime";
    public final static String CALL_END_TIME = "callEndTime";
    public final static String COMPLETION_CODE = "completionCode";
    public final static String CALL_DURATION = "callDuration";
    public final static String CONVERSATION_DURATION = "conversationDuration";
    public final static String DUMP_FILE = "dumpFile";
    
    //audioStream stat
    public final static String AS_FORMAT = "asFormat";
    public final static String AS_START_TIME = "asStartTime";
    public final static String AS_DURATION = "asDuration";
    public final static String AS_SKEW ="asSkew";
    public final static String AS_MAX_SKEW = "asMaxSkew";
    public final static String AS_MAX_SKEW_TIME = "asMaxSkewTime";
    public final static String AS_AVG_SKEW = "asAvgSkew";
    public final static String AS_EXPECTED_PACKETS = "asExpectedPackets";
    public final static String AS_SENT_PACKETS = "asSentPackets";
    public final static String AS_MISSED_PACKETS = "asMissedPackets";
    public final static String AS_DROPPED_PACKETS = "asDroppedPackets";
    public final static String AS_SILIENCE_PACKETS ="asSiliencePackets";
    public final static String AS_AVG_TRANSFER_TIME = "asAvgTransferTime";
    public final static String AS_MAX_TRANSFER_TIME = "asMaxTransferTime";
    public final static String AS_EMPTY_BUFFER_EVENT_COUNT = "asEmptyBufferEventCount";
    
    //in rtp stat
    public final static String IN_RTP_LOCAL_ADDR = "inRtpLocalAddr";
    public final static String IN_RTP_LOCAL_PORT = "inRtpLocalPort";
    public final static String IN_RTP_REMOTE_ADDR = "inRtpRemoteAddr";
    public final static String IN_RTP_REMOTE_PORT = "inRtpRemotePort";
    public final static String IN_RTP_BAD_RTCP_PACKETS = "inRtpBadRtcpPackets";
    public final static String IN_RTP_BAD_RTP_PACKETS = "inRtpBadRtpPackets";
    public final static String IN_RTP_BYTES_RECEIVED = "inRtpBytesReceived";
    public final static String IN_RTP_PACKETS_RECEIVED = "inRtpPacketsReceived";
    public final static String IN_RTP_REMOTE_COLLISIONS = "inRtpRemoteCollisions";
    public final static String IN_RTP_LOCAL_COLLISIONS = "inRtpLocalCollisions";
    public final static String IN_RTP_TRANSMIT_FAILED = "inRtpTransmitFailed";
    
    //out rtp stat
    public final static String OUT_RTP_LOCAL_ADDR = "outRtpLocalAddr";
    public final static String OUT_RTP_LOCAL_PORT = "outRtpLocalPort";
    public final static String OUT_RTP_REMOTE_ADDR = "outRtpRemoteAddr";
    public final static String OUT_RTP_REMOTE_PORT = "outRtpRemotePort";
    public final static String OUT_RTP_BYTES_SENT = "outRtpBytesSent";
    public final static String OUT_RTP_PACKETS_SENT = "outRtpPacketsSent";
    public final static String OUT_RTP_REMOTE_COLLISIONS = "outRtpRemoteCollisions";
    public final static String OUT_RTP_LOCAL_COLLISIONS = "outRtpLocalCollisions";
    public final static String OUT_RTP_TRANSMIT_FAILED = "outRtpTransmitFailed";
    
    public final static String DATABASE_TABLE_EXTENSION_NAME = "dbTable";
    public final static String DATABASE_TABLE_NAME = "RAVEN_CALL_CDR";
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";

 
    public final static String DATE_PATTERN = "dd.MM.yyyy HH:mm:ss";
    
    @Override
    protected void doInit() throws Exception {
        super.doInit();
        generateFields();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        generateFields();
    }

    private void generateFields() {
        Node node = getRecordExtensionsNode().getNode(DATABASE_TABLE_EXTENSION_NAME);
        if (node==null) {
            DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
            dbExtension.setName(DATABASE_TABLE_EXTENSION_NAME);
            getRecordExtensionsNode().addAndSaveChildren(dbExtension);
            dbExtension.setTableName(DATABASE_TABLE_NAME);
            dbExtension.start();
        }
        createField(ID, RecordSchemaFieldType.STRING);
        createField(ENDPOINT_ADDRESS, RecordSchemaFieldType.STRING);
        createField(CALLING_NUMBER, RecordSchemaFieldType.STRING);
        createField(CALLED_NUMBER, RecordSchemaFieldType.STRING);
        createField(LAST_REDIRECTED_NUMBER, RecordSchemaFieldType.STRING);
        createField(CALL_START_TIME, RecordSchemaFieldType.TIMESTAMP);
        createField(CONNECTION_ESTABLISHED_TIME, RecordSchemaFieldType.TIMESTAMP);
        createField(CONVERSATION_START_TIME, RecordSchemaFieldType.TIMESTAMP);
        createField(CALL_END_TIME, RecordSchemaFieldType.TIMESTAMP);
        createField(COMPLETION_CODE, RecordSchemaFieldType.STRING);
        createField(CALL_DURATION, RecordSchemaFieldType.INTEGER);
        createField(CONVERSATION_DURATION, RecordSchemaFieldType.INTEGER);
        createField(DUMP_FILE, RecordSchemaFieldType.STRING);
        
        //audio stream stat fields
        createField(AS_FORMAT, RecordSchemaFieldType.STRING);
        createField(AS_START_TIME, RecordSchemaFieldType.TIMESTAMP);
        createField(AS_DURATION, RecordSchemaFieldType.INTEGER);
        createField(AS_SKEW, RecordSchemaFieldType.INTEGER);
        createField(AS_MAX_SKEW, RecordSchemaFieldType.INTEGER);
        createField(AS_MAX_SKEW_TIME, RecordSchemaFieldType.STRING);
        createField(AS_AVG_SKEW, RecordSchemaFieldType.DOUBLE);
        createField(AS_EXPECTED_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(AS_SENT_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(AS_MISSED_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(AS_DROPPED_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(AS_SILIENCE_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(AS_AVG_TRANSFER_TIME, RecordSchemaFieldType.DOUBLE);
        createField(AS_MAX_TRANSFER_TIME, RecordSchemaFieldType.INTEGER);
        createField(AS_EMPTY_BUFFER_EVENT_COUNT, RecordSchemaFieldType.INTEGER);

        //in rtp fields
        createField(IN_RTP_LOCAL_ADDR, RecordSchemaFieldType.STRING);
        createField(IN_RTP_LOCAL_PORT, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_REMOTE_ADDR, RecordSchemaFieldType.STRING);
        createField(IN_RTP_REMOTE_PORT, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_BAD_RTP_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_BAD_RTCP_PACKETS, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_BYTES_RECEIVED, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_PACKETS_RECEIVED, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_REMOTE_COLLISIONS, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_LOCAL_COLLISIONS, RecordSchemaFieldType.INTEGER);
        createField(IN_RTP_TRANSMIT_FAILED, RecordSchemaFieldType.INTEGER);
        
        //out rtp fields
        createField(OUT_RTP_LOCAL_ADDR, RecordSchemaFieldType.STRING);
        createField(OUT_RTP_LOCAL_PORT, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_REMOTE_ADDR, RecordSchemaFieldType.STRING);
        createField(OUT_RTP_REMOTE_PORT, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_BYTES_SENT, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_PACKETS_SENT, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_REMOTE_COLLISIONS, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_LOCAL_COLLISIONS, RecordSchemaFieldType.INTEGER);
        createField(OUT_RTP_TRANSMIT_FAILED, RecordSchemaFieldType.INTEGER);
    }

    protected void createField(String name, RecordSchemaFieldType fieldType) {
        if (getNode(name)!=null)
            return;
        String format = RecordSchemaFieldType.TIMESTAMP.equals(fieldType)? DATE_PATTERN : null;
        RecordSchemaFieldNode field = super.createField(name, fieldType, format);
//        field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DATABASE_COLUMN_EXTENSION_NAME, null);
        if (ID.equals(name))
            IdRecordFieldExtension.create(field, "id");
        if (RecordSchemaFieldType.TIMESTAMP.equals(fieldType))
            field.setPattern(DATE_PATTERN);
    }    
}

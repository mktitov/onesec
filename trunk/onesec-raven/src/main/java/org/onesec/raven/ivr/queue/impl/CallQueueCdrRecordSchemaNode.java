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

package org.onesec.raven.ivr.queue.impl;

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.*;
import org.raven.tree.Node;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class CallQueueCdrRecordSchemaNode extends RecordSchemaNode
{
    public final static String ID = "id";
    public final static String TARGET_QUEUE = "targetQueue";
    public final static String HANDLED_BY_QUEUE = "handledByQueue";
    public final static String PRIORITY = "priority";
    public final static String CALLING_NUMBER = "callingNumber";
    public final static String OPERATOR_ID = "operatorId";
    public final static String OPERATOR_NUMBER = "operatorNumber";
    public final static String OPERATOR_PERSON_ID = "operatorPersonId";
    public final static String OPERATOR_PERSON_DESC = "operatorPersonDesc";
    public final static String OPERATOR_BUSY_TIMER = "operatorBusyTimer";
    public final static String TRANSFERED = "transfered";
    public final static String LOG = "log";
    public final static String QUEUED_TIME = "queuedTime";
    public final static String REJECTED_TIME = "rejectedTime";
    public final static String READY_TO_COMMUTATE_TIME = "readyToCommutateTime";
    public final static String COMMUTATED_TIME = "commutatedTime";
    public final static String DISCONNECTED_TIME = "disconnectedTime";
    public final static String CONVERSATION_START_TIME = "conversationStartTime";
    public final static String CONVERSATION_DURATION = "conversationDuration";

    public final static String DATABASE_TABLE_EXTENSION_NAME = "dbTable";
    public final static String DATABASE_TABLE_NAME = "RAVEN_CALL_QUEUE_CDR";
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";

    @Message private static String datePattern;
    @Message private static String idDisplayName;
    @Message private static String targetQueueDisplayName;
    @Message private static String handledByQueueDisplayName;
    @Message private static String priorityDisplayName;
    @Message private static String callingNumberDisplayName;
    @Message private static String operatorIdDisplayName;
    @Message private static String operatorNumberDisplayName;
    @Message private static String operatorPersonIdDisplayName;
    @Message private static String operatorPersonDescDisplayName;
    @Message private static String transferedDisplayName;
    @Message private static String logDisplayName;
    @Message private static String queuedTimeDisplayName;
    @Message private static String rejectedTimeDisplayName;
    @Message private static String readyToCommutateDisplayName;
    @Message private static String commutatedTimeDisplayName;
    @Message private static String disconnectedTimeDisplayName;
    @Message private static String conversationStartTimeDisplayName;
    @Message private static String conversationDurationDisplayName;
    @Message private static String operatorBusyTimerDisplayName;

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
        Node node = getRecordExtensionsNode().getChildren(DATABASE_TABLE_EXTENSION_NAME);
        if (node==null) {
            DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
            dbExtension.setName(DATABASE_TABLE_EXTENSION_NAME);
            getRecordExtensionsNode().addAndSaveChildren(dbExtension);
            dbExtension.setTableName(DATABASE_TABLE_NAME);
            dbExtension.start();
        }
        createField(ID, idDisplayName, RecordSchemaFieldType.LONG);
        createField(TARGET_QUEUE, targetQueueDisplayName, RecordSchemaFieldType.STRING);
        createField(HANDLED_BY_QUEUE, handledByQueueDisplayName, RecordSchemaFieldType.STRING);
        createField(PRIORITY, priorityDisplayName, RecordSchemaFieldType.INTEGER);
        createField(CALLING_NUMBER, callingNumberDisplayName, RecordSchemaFieldType.STRING);
        createField(OPERATOR_ID, operatorIdDisplayName, RecordSchemaFieldType.STRING);
        createField(OPERATOR_NUMBER, operatorNumberDisplayName, RecordSchemaFieldType.STRING);
        createField(OPERATOR_PERSON_ID, operatorPersonIdDisplayName, RecordSchemaFieldType.STRING);
        createField(OPERATOR_PERSON_DESC, operatorPersonDescDisplayName, RecordSchemaFieldType.STRING);
        createField(OPERATOR_BUSY_TIMER, operatorBusyTimerDisplayName, RecordSchemaFieldType.INTEGER);
        createField(TRANSFERED, transferedDisplayName, RecordSchemaFieldType.STRING);
        createField(LOG, logDisplayName, RecordSchemaFieldType.STRING);
        createField(QUEUED_TIME, queuedTimeDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(REJECTED_TIME, rejectedTimeDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(READY_TO_COMMUTATE_TIME, readyToCommutateDisplayName
                , RecordSchemaFieldType.TIMESTAMP);
        createField(COMMUTATED_TIME, commutatedTimeDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(DISCONNECTED_TIME, disconnectedTimeDisplayName
                , RecordSchemaFieldType.TIMESTAMP);
        createField(CONVERSATION_START_TIME, conversationStartTimeDisplayName
                , RecordSchemaFieldType.TIMESTAMP);
        createField(CONVERSATION_DURATION, conversationDurationDisplayName
                , RecordSchemaFieldType.INTEGER);
    }

    protected void createField(String name, String displayName, RecordSchemaFieldType fieldType) {
        if (getChildren(name)!=null)
            return;
        String format = RecordSchemaFieldType.TIMESTAMP.equals(fieldType)? datePattern : null;
        RecordSchemaFieldNode field = super.createField(name, fieldType, format);
        field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DATABASE_COLUMN_EXTENSION_NAME, null);
        if (ID.equals(name))
            IdRecordFieldExtension.create(field, "id");
        if (RecordSchemaFieldType.TIMESTAMP.equals(fieldType))
            field.setPattern(datePattern);
    }
}

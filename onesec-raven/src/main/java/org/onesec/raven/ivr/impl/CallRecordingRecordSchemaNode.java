/*
 * Copyright 2012 Mikhail Titov.
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
import org.raven.ds.impl.*;
import org.raven.tree.Node;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class CallRecordingRecordSchemaNode extends RecordSchemaNode {
    
    public final static String ID = "id";
    public final static String CONV1_NUMA = "c1NumA";
    public final static String CONV1_NUMB = "c1NumB";
    public final static String CONV2_NUMA = "c2NumA";
    public final static String CONV2_NUMB = "c2NumB";
    public final static String RECORDING_TIME = "recordingTime";
    public final static String RECORDING_DURATION = "duration";
    public final static String FILE = "file";
    
    @Message private static String datePattern;
    @Message private static String idDisplayName;
    @Message private static String c1NumADisplayName;
    @Message private static String c1NumBDisplayName;
    @Message private static String c2NumADisplayName;
    @Message private static String c2NumBDisplayName;
    @Message private static String recordingTimeDisplayName;
    @Message private static String recordingDurationDisplayName;
    @Message private static String fileDisplayName;
    
    public final static String DATABASE_TABLE_EXTENSION_NAME = "dbTable";
    public final static String DATABASE_TABLE_NAME = "RAVEN_CALL_QUEUE_CDR";
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";
    
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
        if (node==null)
        {
            DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
            dbExtension.setName(DATABASE_TABLE_EXTENSION_NAME);
            getRecordExtensionsNode().addAndSaveChildren(dbExtension);
            dbExtension.setTableName(DATABASE_TABLE_NAME);
            dbExtension.start();
        }
        createField(ID, idDisplayName, RecordSchemaFieldType.LONG);
        createField(RECORDING_TIME, recordingTimeDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(RECORDING_DURATION, recordingDurationDisplayName, RecordSchemaFieldType.INTEGER);
        createField(CONV1_NUMA, c1NumADisplayName, RecordSchemaFieldType.STRING);
        createField(CONV1_NUMB, c1NumBDisplayName, RecordSchemaFieldType.STRING);
        createField(CONV2_NUMA, c2NumADisplayName, RecordSchemaFieldType.STRING);
        createField(CONV2_NUMB, c2NumBDisplayName, RecordSchemaFieldType.STRING);
        createField(FILE, fileDisplayName, RecordSchemaFieldType.STRING);
    }

    protected void createField(String name, String displayName, RecordSchemaFieldType fieldType)
    {
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

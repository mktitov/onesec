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
package org.onesec.raven.sms.impl;

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class, childNodes=RecordSchemaFieldNode.class)
public class IncomingSmsRecordSchemaNode extends RecordSchemaNode {
    public final static String MESSAGE_ID = "messageId";
    public final static String MESSAGE_SEG_COUNT = "messageSegmentsCount";
//    public final static String SEQUENCE_NUMBER = "sequenceNumber";
    public final static String SRC_ADDRESS = "srcAddress";
    public final static String SRC_NPI = "srcNpi";
    public final static String SRC_TON = "srcTon";
    public final static String DST_ADDRESS = "dstAddress";
    public final static String DST_NPI = "dstNpi";
    public final static String DST_TON = "dstTon";
    public final static String MESSAGE = "message";
    public final static String RECEIVE_TS = "receiveTs";
    
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";
    
    @Message private static String datePattern;
    
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
        createField(MESSAGE_ID, RecordSchemaFieldType.INTEGER);
        createField(MESSAGE_SEG_COUNT, RecordSchemaFieldType.INTEGER);
//        createField(SEQUENCE_NUMBER, RecordSchemaFieldType.INTEGER);
        createField(SRC_ADDRESS, RecordSchemaFieldType.STRING);
        createField(SRC_TON, RecordSchemaFieldType.BYTE);
        createField(SRC_NPI, RecordSchemaFieldType.BYTE);
        createField(DST_ADDRESS, RecordSchemaFieldType.STRING);
        createField(DST_TON, RecordSchemaFieldType.BYTE);
        createField(DST_NPI, RecordSchemaFieldType.BYTE);
        createField(MESSAGE, RecordSchemaFieldType.STRING);
        createField(RECEIVE_TS, RecordSchemaFieldType.TIMESTAMP);        
    }    

    protected void createField(String name, RecordSchemaFieldType fieldType) {
        if (getNode(name)!=null)
            return;
        String format = RecordSchemaFieldType.TIMESTAMP.equals(fieldType)? datePattern : null;
        RecordSchemaFieldNode field = super.createField(name, fieldType, format);
//        field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DATABASE_COLUMN_EXTENSION_NAME, null);
        if (RecordSchemaFieldType.TIMESTAMP.equals(fieldType))
            field.setPattern(datePattern);
    }
    
}

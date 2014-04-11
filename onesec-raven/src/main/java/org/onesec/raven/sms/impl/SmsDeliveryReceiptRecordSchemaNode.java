/*
 * Copyright 2014 Mikhail Titov.
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
public class SmsDeliveryReceiptRecordSchemaNode extends RecordSchemaNode {
    public final static String MESSAGE_ID = "messageId";
    public final static String SUBMIT_DATE = "submitDate";
    public final static String DONE_DATE = "doneDate";
    public final static String STATUS = "status";
    public final static String ERROR_CODE = "errorCode";
    
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";
    
    @Message private static String datePattern;
    @Message private static String messageIdDisplayName;
    @Message private static String submitDateDisplayName;
    @Message private static String doneDateDisplayName;
    @Message private static String statusDisplayName;
    @Message private static String errorCodeDisplayName;
    
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
        createField(MESSAGE_ID, messageIdDisplayName, RecordSchemaFieldType.STRING);
        createField(SUBMIT_DATE, submitDateDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(DONE_DATE, doneDateDisplayName, RecordSchemaFieldType.TIMESTAMP);
        createField(STATUS, statusDisplayName, RecordSchemaFieldType.STRING);
        createField(ERROR_CODE, errorCodeDisplayName, RecordSchemaFieldType.STRING);
    }
    
    protected void createField(String name, String displayName, RecordSchemaFieldType fieldType) {
        if (getNode(name)!=null)
            return;
        String format = RecordSchemaFieldType.TIMESTAMP.equals(fieldType)? datePattern : null;
        RecordSchemaFieldNode field = super.createField(name, fieldType, format);
        field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DATABASE_COLUMN_EXTENSION_NAME, null);
        if (RecordSchemaFieldType.TIMESTAMP.equals(fieldType))
            field.setPattern(datePattern);
    }
}

/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.CsvRecordFieldExtension;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;
import org.weda.internal.annotations.Service;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class, childNodes=RecordSchemaFieldNode.class)
public class IvrInformerRecordSchemaNode extends RecordSchemaNode
{
    public static final String CSV_COLUMN_EXTENSION_NAME = "CSV_COLUMN";
    public static final String DB_TABLE_COLUMN_EXTENSION_NAME = "DB_TABLE_COLUMN";
    public static final String DB_TABLE_EXTENSION_NAME = "DB_TABLE";
    public static final String RECORD_ID_EXTENSION_NAME = "RECORD_ID";
    public final static String TABLE_NAME = "ONESEC_INFORMER_REQUEST";
    
    public final static String ID_FIELD = "ID";
    public final static String LIST_OPERATOR_ID_FIELD = "LIST_OPERATOR_ID";
    public final static int LIST_OPERATOR_ID_CSV_COL = 1;
    public final static String ABONENT_ID_FIELD = "ABONENT_ID";
    public final static int ABONENT_ID_CSV_COL = 2;
    public final static String ABONENT_DESC_FIELD = "ABONENT_DESC";
    public final static int ABONENT_DESC_CSV_COL = 3;
    public final static String ABONENT_NUMBER_FIELD = "ABONENT_NUMBER";
    public final static int ABONENT_NUMBER_CSV_COL = 4;
    public final static String CALL_ORDER_FIELD = "CALL_ORDER";
    public final static int CALL_ORDER_CSV_COL = 5;
    public final static String COMPLETION_CODE_FIELD = "COMPLETION_CODE";
    public final static String CALL_START_TIME_FIELD = "CALL_START_TIME";
    public final static String CALL_END_TIME_FIELD = "CALL_END_TIME";
    public final static String CALL_DURATION_FIELD = "CALL_DURATION";
    public final static String CONVERSATION_START_TIME_FIELD = "CONVERSATION_START_TIME";
    public final static String CONVERSATION_DURATION_FIELD = "CONVERSATION_DURATION";

    @Service
    private static MessagesRegistry messagesRegistry;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        initializeSchema();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        initializeSchema();
    }

    private void initializeSchema()
    {
        DatabaseRecordExtension.create(
                getRecordExtensionsNode(), DB_TABLE_EXTENSION_NAME, TABLE_NAME);
        createInformerField(ID_FIELD, RecordSchemaFieldType.LONG, null, 0);
        createInformerField(
                LIST_OPERATOR_ID_FIELD, RecordSchemaFieldType.STRING, null
                , LIST_OPERATOR_ID_CSV_COL);
        createInformerField(
                ABONENT_ID_FIELD, RecordSchemaFieldType.STRING, null, ABONENT_ID_CSV_COL);
        createInformerField(
                ABONENT_DESC_FIELD, RecordSchemaFieldType.STRING, null, ABONENT_DESC_CSV_COL);
        createInformerField(
                ABONENT_NUMBER_FIELD, RecordSchemaFieldType.STRING, null, ABONENT_NUMBER_CSV_COL);
        createInformerField(
                CALL_ORDER_FIELD, RecordSchemaFieldType.SHORT, null, CALL_ORDER_CSV_COL);
        createInformerField(COMPLETION_CODE_FIELD, RecordSchemaFieldType.STRING, null, 0);
        createInformerField(
                CALL_START_TIME_FIELD, RecordSchemaFieldType.STRING, "dd.MM.yyyy HH:mm:ss", 0);
        createInformerField(
                CALL_END_TIME_FIELD, RecordSchemaFieldType.STRING, "dd.MM.yyyy HH:mm:ss", 0);
        createInformerField(CALL_DURATION_FIELD, RecordSchemaFieldType.LONG, null, 0);
        createInformerField(
                CONVERSATION_START_TIME_FIELD, RecordSchemaFieldType.STRING
                , "dd.MM.yyyy HH:mm:ss", 0);
        createInformerField(CONVERSATION_DURATION_FIELD, RecordSchemaFieldType.LONG, null, 0);
    }

    private void createInformerField(
            String name, RecordSchemaFieldType type, String pattern, int columnNumber)
    {
        RecordSchemaFieldNode field = createField(name, type, pattern);
        if (name.equals(field.getDisplayName()))
        {
            String displayName = messagesRegistry.getMessages(
                    IvrInformerRecordSchemaNode.class).get(name);
            field.setDisplayName(displayName);
        }
        if (ID_FIELD.equals(name) && field.getChildren(RECORD_ID_EXTENSION_NAME)==null)
        {
            IdRecordFieldExtension idExt = new IdRecordFieldExtension();
            idExt.setName(RECORD_ID_EXTENSION_NAME);
            field.addAndSaveChildren(idExt);
            idExt.start();
        }
        DatabaseRecordFieldExtension.create(field, DB_TABLE_COLUMN_EXTENSION_NAME, name);
        if (columnNumber>0)
            CsvRecordFieldExtension.create(field, CSV_COLUMN_EXTENSION_NAME, columnNumber);
    }
}

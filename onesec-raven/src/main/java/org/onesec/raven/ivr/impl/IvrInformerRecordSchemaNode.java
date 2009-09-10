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
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class, childNodes=RecordSchemaFieldNode.class)
public class IvrInformerRecordSchemaNode extends RecordSchemaNode
{
    public static final String DB_TABLE_COLUMN_EXTENSION_NAME = "DB_TABLE_COLUMN";
    public static final String DB_TABLE_EXTENSION_NAME = "DB_TABLE";
    public final static String TABLE_NAME = "ONESEC_INFORMER_REQUEST";
    
    public final static String ID_FIELD = "ID";
    public final static String ACTIVE_LIST = "ACTIVE_LIST";
    public final static String ABONENT_ID_FIELD = "ABONENT_ID";
    public final static String ABONENT_DESC_FIELD = "ABONENT_DESC";
    public final static String ABONENT_NUMBER_FIELD = "ABONENT_NUMBER";
    public final static String CALL_ORDER_FIELD = "CALL_ORDER";
    public final static String COMPLETION_CODE_FIELD = "COMPLETION_CODE";
    public final static String CALL_START_TIME_FIELD = "CALL_START_TIME";
    public final static String CALL_END_TIME_FIELD = "CALL_END_TIME";
    public final static String CALL_DURATION_FIELD = "CALL_DURATION";
    public final static String CONVERSATION_START_TIME_FIELD = "CONVERSATION_START_TIME";
    public final static String CONVERSATION_DURATION_FIELD = "CONVERSATION_DURATION";

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
        createInformerField(ID_FIELD, "#", RecordSchemaFieldType.LONG, null);
    }

    private void createInformerField(
            String name, String displayName, RecordSchemaFieldType type, String pattern)
    {
        RecordSchemaFieldNode field = createField(name, type, pattern);
        if (field.getDisplayName()==null)
            field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DB_TABLE_COLUMN_EXTENSION_NAME, name);
    }
}

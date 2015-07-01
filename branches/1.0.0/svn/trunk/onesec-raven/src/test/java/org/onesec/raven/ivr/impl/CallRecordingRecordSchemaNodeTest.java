/*
 * Copyright 2012 Mikhail Titov
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

import static org.junit.Assert.*;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import static org.onesec.raven.ivr.impl.CallRecordingRecordSchemaNode.*;
import org.onesec.raven.ivr.queue.impl.CallQueueCdrRecordSchemaNode;
import org.raven.RavenUtils;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.*;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class CallRecordingRecordSchemaNodeTest extends OnesecRavenTestCase {
    
    @Test
    public void test() {
        CallRecordingRecordSchemaNode schema = new CallRecordingRecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        
        checkRecordDbExtension(schema);
        checkField(schema, ID, RecordSchemaFieldType.LONG);
        checkField(schema, RECORDING_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, RECORDING_DURATION, RecordSchemaFieldType.INTEGER);
        checkField(schema, CONV1_NUMA, RecordSchemaFieldType.STRING);
        checkField(schema, CONV1_NUMB, RecordSchemaFieldType.STRING);
        checkField(schema, CONV2_NUMA, RecordSchemaFieldType.STRING);
        checkField(schema, CONV2_NUMB, RecordSchemaFieldType.STRING);
        checkField(schema, FILE, RecordSchemaFieldType.STRING);
    }
    
    private void checkRecordDbExtension(CallRecordingRecordSchemaNode schema)
    {
        DatabaseRecordExtension dbExt = schema.getRecordExtension(
                DatabaseRecordExtension.class, "dbTable");
        assertNotNull(dbExt);
        assertEquals(CallQueueCdrRecordSchemaNode.DATABASE_TABLE_NAME, dbExt.getTableName());
    }

    private void checkField(RecordSchemaNode schema, String fieldName, RecordSchemaFieldType type)
    {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) schema.getChildren(fieldName);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());

        if (ID.equals(fieldName)) {
            Node node = field.getChildren("id");
            assertNotNull(node);
            assertTrue(node instanceof IdRecordFieldExtension);
        }
        DatabaseRecordFieldExtension dbExt =
                (DatabaseRecordFieldExtension) field.getChildren("dbColumn");
        assertNotNull(dbExt);
        assertEquals(RavenUtils.nameToDbName(fieldName), dbExt.getColumnName());
    }
}

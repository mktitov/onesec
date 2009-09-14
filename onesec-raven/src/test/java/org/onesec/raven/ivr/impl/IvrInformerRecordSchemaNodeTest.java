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

import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.CsvRecordFieldExtension;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.FilterableRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class IvrInformerRecordSchemaNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test()
    {
        IvrInformerRecordSchemaNode rec = new IvrInformerRecordSchemaNode();
        rec.setName("Informer record");
        tree.getRootNode().addAndSaveChildren(rec);
        assertTrue(rec.start());

        DatabaseRecordExtension dbExt = rec.getRecordExtension(DatabaseRecordExtension.class, null);
        assertNotNull(dbExt);
        assertStarted(dbExt);
        assertEquals(IvrInformerRecordSchemaNode.TABLE_NAME, dbExt.getTableName());
        
        checkField(rec, ID_FIELD, RecordSchemaFieldType.LONG, null, 0, true);
        checkField(rec, OPERATOR_ID_FIELD, RecordSchemaFieldType.STRING, null
                , OPERATOR_ID_CSV_COL, true);
        checkField(rec, LIST_ID_FIELD, RecordSchemaFieldType.STRING, null, LIST_ID_CSV_COL, true);
        checkField(rec, ABONENT_ID_FIELD, RecordSchemaFieldType.STRING, null
                , ABONENT_ID_CSV_COL, true);
        checkField(
                rec, ABONENT_DESC_FIELD, RecordSchemaFieldType.STRING, null
                , ABONENT_DESC_CSV_COL, false);
        checkField(
                rec, ABONENT_NUMBER_FIELD, RecordSchemaFieldType.STRING, null
                , ABONENT_NUMBER_CSV_COL, false);
        checkField(rec, CALL_ORDER_FIELD, RecordSchemaFieldType.SHORT, null
                , CALL_ORDER_CSV_COL, false);
        checkField(rec, COMPLETION_CODE_FIELD, RecordSchemaFieldType.STRING, null, 0, true);
        checkField(rec, 
                CALL_START_TIME_FIELD, RecordSchemaFieldType.TIMESTAMP, "dd.MM.yyyy HH:mm:ss"
                , 0, false);
        checkField(rec,
                CALL_END_TIME_FIELD, RecordSchemaFieldType.TIMESTAMP, "dd.MM.yyyy HH:mm:ss", 0
                , false);
        checkField(rec, CALL_DURATION_FIELD, RecordSchemaFieldType.LONG, null, 0, false);
        checkField(rec,
                CONVERSATION_START_TIME_FIELD, RecordSchemaFieldType.TIMESTAMP
                , "dd.MM.yyyy HH:mm:ss", 0, false);
        checkField(rec, CONVERSATION_DURATION_FIELD, RecordSchemaFieldType.LONG, null, 0, false);

    }

    private void checkField(
            IvrInformerRecordSchemaNode rec, String fieldName
            , RecordSchemaFieldType type, String pattern, int csvColumnNumber, boolean filterable)
    {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) rec.getChildren(fieldName);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());
        assertEquals(pattern, field.getPattern());
        assertNotNull(field.getDisplayName());

        if (ID_FIELD.equals(fieldName))
        {
            IdRecordFieldExtension idExt = field.getFieldExtension(
                    IdRecordFieldExtension.class, null);
            assertNotNull(idExt);
            assertStarted(idExt);
        }

        DatabaseRecordFieldExtension colExt =
                field.getFieldExtension(DatabaseRecordFieldExtension.class, null);
        assertNotNull(colExt);
        assertStarted(colExt);
        assertEquals(fieldName, colExt.getColumnName());

        if (csvColumnNumber>0)
        {
            CsvRecordFieldExtension csvExt =
                    field.getFieldExtension(CsvRecordFieldExtension.class, null);
            assertNotNull(csvExt);
            assertStarted(csvExt);
            assertEquals(new Integer(csvColumnNumber), csvExt.getColumnNumber());
        }

        if (filterable)
        {
            FilterableRecordFieldExtension filterExt =
                    field.getFieldExtension(FilterableRecordFieldExtension.class, null);
            assertNotNull(filterExt);
            assertStarted(filterExt);
            assertFalse(filterExt.getCaseSensitive());
            assertFalse(filterExt.getFilterValueRequired());
            assertNull(filterExt.getDefaultValue());
        }
    }
}
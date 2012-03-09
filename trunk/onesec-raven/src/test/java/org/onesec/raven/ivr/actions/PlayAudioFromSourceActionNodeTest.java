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
package org.onesec.raven.ivr.actions;

import java.io.InputStream;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.BindingNames;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.PushOnDemandDataSourceListener;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioFromSourceActionNodeTest extends OnesecRavenTestCase {
    
    private PushOnDemandDataSource ds;
    private PlayAudioFromSourceActionNode actionNode;
    
    @Before
    public void prepare() throws Exception {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        NodeAttribute textAttr = new NodeAttributeImpl("text", String.class, null, null);
        textAttr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        ds.addConsumerAttribute(textAttr);
        assertTrue(ds.start());
        
        actionNode = new PlayAudioFromSourceActionNode();
        actionNode.setName("actionNode");
        tree.getRootNode().addAndSaveChildren(actionNode);
        actionNode.setDataSource(ds);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void test() throws Exception {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = new SimpleBindings();
        bindings.put("test", "test text");
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(conv.getAudioStream()).andReturn(null);
        
        replay(conv, state);
        
        actionNode.getNodeAttribute("text").setValue("test");
        ds.addDataPortion(new InputStreamSource() {
            public InputStream getInputStream() {
                return null;
            }
        });
        ds.setListener(new PushOnDemandDataSourceListener() {
            public void onGatherDataForConsumer(DataConsumer consumer, DataContext context) {
                assertNotNull(context);
                NodeAttribute attr = context.getSessionAttributes().get("text");
                assertNotNull(attr);
                assertEquals("test text", attr.getValue());
            }
        });
        PlayAudioFromSourceAction action = (PlayAudioFromSourceAction) actionNode.createAction();
        action.doExecute(conv);
        assertNotNull(ds.getLastContext());
        
        verify(conv, state);
    }
    
    @Test
    public void testWithExistingContext() throws Exception {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        InputStreamSource source = createMock(InputStreamSource.class);
        final DataContext context = new DataContextImpl();
        Bindings bindings = new SimpleBindings();
        bindings.put(BindingNames.DATA_CONTEXT_BINDING, context);
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(conv.getAudioStream()).andReturn(null);
        
        replay(conv, state, source);
        
        actionNode.getNodeAttribute("text").setValue("'test'");
        ds.addDataPortion(source);
        ds.setListener(new PushOnDemandDataSourceListener() {
            public void onGatherDataForConsumer(DataConsumer consumer, DataContext ctx) {
                assertSame(context, ctx);
            }
        });
        PlayAudioFromSourceAction action = (PlayAudioFromSourceAction) actionNode.createAction();
        action.doExecute(conv);
        assertNotNull(ds.getLastContext());
        
        verify(conv, state, source);
    }
}

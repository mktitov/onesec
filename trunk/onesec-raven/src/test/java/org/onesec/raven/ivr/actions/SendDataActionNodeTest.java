package org.onesec.raven.ivr.actions;

import org.junit.Test;
import org.junit.Before;
import static org.easymock.EasyMock.*;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.BindingNames;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;

public class SendDataActionNodeTest extends OnesecRavenTestCase
{
    private SendDataActionNode actionNode;
    private DataCollector collector;
    
    @Before
    public void prepare()
    {
        actionNode = new SendDataActionNode();
        actionNode.setName("actionNode");
        tree.getRootNode().addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(actionNode);
        assertTrue(collector.start());
    }
    
    @Test
    public void test() throws Exception
    {
        Bindings bindings = new SimpleBindings();
        bindings.put("world", "World!");
        
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        DataContext context = createMock(DataContext.class);
        DataHandler dataHandler = createMock(DataHandler.class);

        expect(conv.getConversationScenarioState()).andReturn(state).anyTimes();
        expect(state.getBindings()).andReturn(bindings).anyTimes();
        dataHandler.handleData(eq("Hello World!"), isA(DataContext.class));

        replay(conv, state, context, dataHandler);
        
        collector.setDataHandler(dataHandler);
        actionNode.setExpression("'Hello '+world");
        SendDataAction action = (SendDataAction) actionNode.createAction();
        action.doExecute(conv);

        verify(conv, state, context, dataHandler);
    }

    @Test
    public void dataContextTest() throws Exception
    {
        Bindings bindings = new SimpleBindings();
        bindings.put("world", "World!");

        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        DataContext context = createMock(DataContext.class);
        DataHandler dataHandler = createMock(DataHandler.class);
        DataContext dataContext = createMock(DataContext.class);

        expect(conv.getConversationScenarioState()).andReturn(state).anyTimes();
        expect(state.getBindings()).andReturn(bindings).anyTimes();
        dataHandler.handleData(eq("Hello World!"), same(dataContext));

        replay(conv, state, context, dataHandler);

        bindings.put(BindingNames.DATA_CONTEXT_BINDING, dataContext);
        collector.setDataHandler(dataHandler);
        actionNode.setExpression("'Hello '+world");
        SendDataAction action = (SendDataAction) actionNode.createAction();
        action.doExecute(conv);

        verify(conv, state, context, dataHandler);
    }
}

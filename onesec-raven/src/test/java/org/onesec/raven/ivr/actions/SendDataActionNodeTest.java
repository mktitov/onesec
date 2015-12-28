package org.onesec.raven.ivr.actions;

import org.junit.Test;
import org.junit.Before;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.Action;
import org.raven.BindingNames;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;

@RunWith(JMockit.class)
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
    public void test(
            @Mocked final Action.Execute execMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final DataHandler dataHandler) 
        throws Exception
    {
        final Bindings bindings = new SimpleBindings();
        bindings.put("world", "World!");
        new Expectations() {{
            state.getBindings(); result = bindings;            
        }};
        
        collector.setDataHandler(dataHandler);
        actionNode.setExpression("'Hello '+world");
        SendDataAction action = (SendDataAction) actionNode.createAction();
        Action.ActionExecuted res = action.processExecuteMessage(execMessage);
        assertSame(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, res);
        new Verifications() {{
            dataHandler.handleData("Hello World!", withInstanceOf(DataContext.class));
        }};
    }
    
    @Test
    public void dataContextTest(
            @Mocked final Action.Execute execMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final DataHandler dataHandler,
            @Mocked final DataContext dataContext) 
        throws Exception
    {
        final Bindings bindings = new SimpleBindings();
        bindings.put("world", "World!");
        bindings.put(BindingNames.DATA_CONTEXT_BINDING, dataContext);
        new Expectations() {{
            state.getBindings(); result = bindings;            
        }};
        
        collector.setDataHandler(dataHandler);
        actionNode.setExpression("'Hello '+world");
        SendDataAction action = (SendDataAction) actionNode.createAction();
        Action.ActionExecuted res = action.processExecuteMessage(execMessage);
        assertSame(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, res);
        new Verifications() {{
            dataHandler.handleData("Hello World!", withSameInstance(dataContext));
        }};
    }
}

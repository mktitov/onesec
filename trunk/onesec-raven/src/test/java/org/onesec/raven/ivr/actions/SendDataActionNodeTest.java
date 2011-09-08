package org.onesec.raven.ivr.actions;

import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;

import javax.script.Bindings;
import java.io.File;

public class SendDataActionNodeTest
{
    @Test
    public void test()
    {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        DataContext context = createMock(DataContext.class);

        expect(conv.getConversationScenarioState()).andReturn(state).anyTimes();
        expect(state.getBindings()).andReturn(bindings).anyTimes();
        expect(bindings.get)

        replay(conv, state, bindings, context);



        verify(conv, state, bindings, context);
    }

}

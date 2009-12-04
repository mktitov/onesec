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

package org.onesec.raven.ivr.actions;

import java.util.Map;
import javax.media.Manager;
import javax.media.Player;
import javax.media.protocol.FileTypeDescriptor;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.impl.ConcatDataSource;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class TestEndpointNode extends BaseNode implements IvrEndpoint
{
    @NotNull @Parameter()
    private ExecutorService executorService;

    private ConcatDataSource audioStream;
    private Player player;

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        audioStream = new ConcatDataSource(FileTypeDescriptor.WAVE, executorService, 160, this);
        player = Manager.createPlayer(audioStream);
        player.start();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        player.stop();
        audioStream.close();
    }

    public IvrEndpointState getEndpointState()
    {
        return null;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    public AudioStream getAudioStream()
    {
        return audioStream;
    }

    public void stopConversation(CompletionCode completionCode)
    {
    }

    public void invite(String opponentNumber, IvrConversationScenario conversationScenario, ConversationCompletionCallback callback, Map<String, Object> bindings) throws IvrEndpointException
    {
    }

    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
    }

    public String getObjectName()
    {
        return getName();
    }

    public String getObjectDescription()
    {
        return "test endpoint node";
    }

    public Node getOwner()
    {
        return this;
    }

}

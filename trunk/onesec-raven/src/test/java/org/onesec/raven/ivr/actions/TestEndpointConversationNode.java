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
import javax.media.Player;
import javax.media.protocol.FileTypeDescriptor;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.ConcatDataSource;
import org.raven.annotations.Parameter;
import org.raven.conv.ConversationScenarioState;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class TestEndpointConversationNode extends BaseNode implements IvrEndpointConversation
{
    @NotNull @Parameter()
    private ExecutorService executorService;

    @Service
    private static BufferCache bufferCache;
    
    @Service
    private static CodecManager codecManager;
    

    private ConcatDataSource audioStream;
    private Player player;
    private JMFHelper.OperationController operationController;
    private String fileName = "target/out.wav";
    private ConversationScenarioState conversationScenarioState;

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        audioStream = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executorService, codecManager, Codec.LINEAR, 240, 5, 5
                , this, bufferCache);
        audioStream.start();
//        player = Manager.createPlayer(audioStream);
        operationController = JMFHelper.writeToFile(audioStream, fileName);
//        player.start();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
//        player.stop();
        audioStream.close();
        operationController.stop();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getCallingNumber() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getCalledNumber() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void continueConversation(char dtmfChar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setConversationScenarioState(ConversationScenarioState conversationScenarioState) {
        this.conversationScenarioState = conversationScenarioState;
    }

    public void sendMessage(String message, String encoding, SendMessageDirection direction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ConversationScenarioState getConversationScenarioState() {
        return conversationScenarioState;
    }

    public void addConversationListener(IvrEndpointConversationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeConversationListener(IvrEndpointConversationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrEndpointConversationState getState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IncomingRtpStream getIncomingRtpStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

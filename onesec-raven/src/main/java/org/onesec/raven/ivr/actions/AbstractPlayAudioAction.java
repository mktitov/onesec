/*
 *  Copyright 2011 Mikhail Titov.
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

import java.io.InputStream;
import javax.script.Bindings;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.PlayAudioDP;
import org.raven.dp.DataProcessorFacade;
import org.raven.expr.BindingSupport;
import org.raven.tree.impl.LoggerHelper;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractPlayAudioAction extends AbstractAction
{
    private final TypeConverter converter;
    
    public AbstractPlayAudioAction(String actionName, TypeConverter converter)
    {
        super(actionName);
        this.converter = converter;
    }

    protected abstract Object getAudio(IvrEndpointConversation conversation) throws Exception;
    protected abstract BindingSupport getBindingSupport();

    @Override
    public Object processData(Object message) throws Exception {
        if (message==PlayAudioDP.PLAYED) {
            sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
            return VOID;
        } else
            return super.processData(message);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        final IvrEndpointConversation conversation = message.getConversation();
        final AudioStream audioStream = conversation.getAudioStream();
        final Object audio = doGetAudio(conversation);
        if (audio==null){
            if (getLogger().isDebugEnabled())
                getLogger().debug("Nothing to play");
            return ACTION_EXECUTED_then_EXECUTE_NEXT;
        } else {
            DataProcessorFacade player = getContext().addChild(getContext().createChild("Player", new PlayAudioDP(audioStream)));
            InputStreamSource source;
            Cacheable cacheInfo;
            if (audio instanceof AudioFile) {
                final Bindings bindings = conversation.getConversationScenarioState().getBindings();
                source = new AudioSource((AudioFile)audio, getLogger(), bindings);
                cacheInfo = (AudioFile)audio;
            } else {
                source = converter.convert(InputStreamSource.class, audio, null);
                cacheInfo = null;
            }
            getFacade().sendTo(player, new PlayAudioDP.PlayInputStreamSource(source, cacheInfo));
            return null;
        }
    }
    
    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
    
    protected Object doGetAudio(IvrEndpointConversation conversation) throws Exception {
        BindingSupport bindingSupport = prepareBindingSupport(conversation);
        try {
            return getAudio(conversation);
        } finally {
            if (bindingSupport!=null)
                bindingSupport.reset();
        }
    }
    
    private BindingSupport prepareBindingSupport(IvrEndpointConversation conversation) {
        BindingSupport bindingSupport = getBindingSupport();
        if (bindingSupport!=null) {
            final Bindings bindings = conversation.getConversationScenarioState().getBindings();
            bindingSupport.putAll(bindings);
        }
        return bindingSupport;
    }
    
    public static class AudioSource implements InputStreamSource {
        private final AudioFile audioFile;
        private final LoggerHelper logger;
        private final Bindings bindings;

        public AudioSource(AudioFile audioFile, LoggerHelper logger, Bindings bindings) {
            this.audioFile = audioFile;
            this.logger = logger;
            this.bindings = bindings;
        }

        @Override
        public InputStream getInputStream() {
            try {
                BindingSupport bindingSupport = audioFile.getBindingSupport();
                try {
                    if (bindingSupport!=null)
                        bindingSupport.putAll(bindings);
                    return audioFile.getAudioFile().getDataStream();
                } finally {
                    if (bindingSupport!=null)
                        bindingSupport.reset();
                }
            }
            catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error(String.format("Error geting audio stream from audio file node (%s) ", audioFile.getPath())
                        , ex);
                return null;
            }
        }    
    }
}

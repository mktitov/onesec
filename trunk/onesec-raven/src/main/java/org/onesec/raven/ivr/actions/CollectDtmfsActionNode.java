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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.apache.commons.lang.StringUtils;
import static org.onesec.raven.ivr.IvrEndpointConversation.*;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class CollectDtmfsActionNode extends BaseNode {
    private final static String VALIDATION_ERROR = "";
    private final static String VALIDATION_ERROR_BINDING = "VALIDATION_ERROR";
    
    @NotNull @Parameter(defaultValue="*")
    private String stopDtmf;
    
    @NotNull @Parameter(defaultValue="POINT")
    private BindingScope bindingScope;
    
    @NotNull @Parameter(defaultValue=DTMFS_BINDING)
    private String dtmfsBindingName;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean autoJoin;
    
    @Parameter
    private String joinSeparator;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object postProcess;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean usePostProcess;
    
    @Parameter
    private Integer maxDtmfsCount;
    
    @Parameter
    private Long autoStopDelay;
    
    @NotNull @Parameter(defaultValue = "SECONDS")
    private TimeUnit autoStopDelayUnit;
       
    private BindingSupportImpl bindingsSupport;
    private CollectDtmfsErrorHandler errorHandler;

    @Override
    protected void initFields() {
        super.initFields();
        bindingsSupport = new BindingSupportImpl();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
    }

    private void initNodes(boolean start) {
        errorHandler = (CollectDtmfsErrorHandler) getNode(CollectDtmfsErrorHandler.NAME);
        if (errorHandler==null) {
            errorHandler = new CollectDtmfsErrorHandler();
            addAndSaveChildren(errorHandler);
            if (start)
                errorHandler.start();
        }
    }

    public CollectDtmfsErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingsSupport.addTo(bindings);
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        if (getStatus()!=Node.Status.STARTED)
            return null;
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        String tempDtmfsKey = getTempDtmfsKey();
        ConversationScenarioState state = getConversationState(bindings);
        List<String> dtmfs = (List<String>) state.getBindings().get(tempDtmfsKey);
        if (dtmfs==null) {
            dtmfs = new LinkedList<String>();
            state.setBinding(tempDtmfsKey, dtmfs, BindingScope.POINT);
            state.setBinding(dtmfsBindingName, dtmfs, bindingScope);
        }
        String dtmf = (String) bindings.get(DTMF_BINDING);
        Integer _maxDtmfsCount = maxDtmfsCount;
        if (stopDtmf.equals(dtmf)) return processStopAction(state, dtmfs, tempDtmfsKey);
        else {
            String lastTsKey = getLastTsKey();
            long curTime = System.currentTimeMillis();
            if (!(EMPTY_DTMF+"").equals(dtmf)) {
                dtmfs.add(dtmf);
                if (_maxDtmfsCount!=null && dtmfs.size()==_maxDtmfsCount)
                    return processStopAction(state, dtmfs, tempDtmfsKey);
                else if (autoStopDelay!=null)
                    state.setBinding(lastTsKey, curTime, BindingScope.POINT);
            } else {               
                Long _autoStopDelay = autoStopDelay;
                if (_autoStopDelay!=null) {                    
                    Long lastTs = (Long) state.getBindings().get(lastTsKey);
                    if (lastTs!=null && lastTs+autoStopDelayUnit.toMillis(autoStopDelay)<=curTime) 
                        return processStopAction(state, dtmfs, tempDtmfsKey);
                }
            }
            return null;
        }
    }
    
    String getLastTsKey() {
        return RavenUtils.generateKey("lastTsKey", this);
    }
    
    String getTempDtmfsKey() {
        return RavenUtils.generateKey("dtmfs", this);
    }
    
    private boolean postProcessDtmfs(ConversationScenarioState state, List<String> dtmfs) {
        boolean success = false;
        if (!dtmfs.isEmpty()) {
            success = true;
            Object res = dtmfs;
            boolean updateBindings = false;
            if (autoJoin) {
                res = StringUtils.join(dtmfs, joinSeparator);
                updateBindings = true;
            }
            if (usePostProcess) {
                bindingsSupport.put(DTMFS_BINDING, res);
                bindingsSupport.put(VALIDATION_ERROR_BINDING, VALIDATION_ERROR);
                try {
                    res = postProcess;                    
                    updateBindings = success = res!=VALIDATION_ERROR;
                } finally {
                    bindingsSupport.reset();
                }
            }
            if (updateBindings)
                state.setBinding(dtmfsBindingName, res, bindingScope);
        }
        return success;
    }
    

    public Collection<Node> processStopAction(ConversationScenarioState state, List<String> dtmfs, 
            String tempDtmfsKey) 
    {
        state.setBinding(tempDtmfsKey, null, BindingScope.POINT);
        state.setBinding(getLastTsKey(), null, BindingScope.POINT);            
        return postProcessDtmfs(state, dtmfs)? super.getEffectiveNodes() : errorHandler.getErrorActions();
    }
    
    private ConversationScenarioState getConversationState(Bindings bindings) {
        return (ConversationScenarioState) bindings.get(CONVERSATION_STATE_BINDING);
    }

    public BindingScope getBindingScope() {
        return bindingScope;
    }

    public void setBindingScope(BindingScope bindingScope) {
        this.bindingScope = bindingScope;
    }

    public String getStopDtmf() {
        return stopDtmf;
    }

    public void setStopDtmf(String stopDtmf) {
        this.stopDtmf = stopDtmf;
    }

    public String getDtmfsBindingName() {
        return dtmfsBindingName;
    }

    public void setDtmfsBindingName(String dtmfsBindingName) {
        this.dtmfsBindingName = dtmfsBindingName;
    }

    public Boolean getAutoJoin() {
        return autoJoin;
    }

    public void setAutoJoin(Boolean autoJoin) {
        this.autoJoin = autoJoin;
    }

    public String getJoinSeparator() {
        return joinSeparator;
    }

    public void setJoinSeparator(String joinSeparator) {
        this.joinSeparator = joinSeparator;
    }

    public Object getPostProcess() {
        return postProcess;
    }

    public void setPostProcess(Object postProcess) {
        this.postProcess = postProcess;
    }

    public Boolean getUsePostProcess() {
        return usePostProcess;
    }

    public void setUsePostProcess(Boolean usePostProcess) {
        this.usePostProcess = usePostProcess;
    }

    public Integer getMaxDtmfsCount() {
        return maxDtmfsCount;
    }

    public void setMaxDtmfsCount(Integer maxDtmfsCount) {
        this.maxDtmfsCount = maxDtmfsCount;
    }

    public Long getAutoStopDelay() {
        return autoStopDelay;
    }

    public void setAutoStopDelay(Long autoStopDelay) {
        this.autoStopDelay = autoStopDelay;
    }

    public TimeUnit getAutoStopDelayUnit() {
        return autoStopDelayUnit;
    }

    public void setAutoStopDelayUnit(TimeUnit autoStopDelayUnit) {
        this.autoStopDelayUnit = autoStopDelayUnit;
    }
}

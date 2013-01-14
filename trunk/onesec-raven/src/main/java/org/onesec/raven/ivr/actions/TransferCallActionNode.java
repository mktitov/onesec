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

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class TransferCallActionNode extends BaseNode implements IvrActionNode
{
    @Parameter
    private String address;

    @NotNull @Parameter(defaultValue="false")
    private Boolean monitorTransfer;

    @NotNull @Parameter(defaultValue="10")
    private Long callStartTimeout;

    @NotNull @Parameter(defaultValue="600")
    private Long callEndTimeout;
    
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    BindingSupportImpl getBindingSupport() {
        return bindingSupport;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public Long getCallEndTimeout()
    {
        return callEndTimeout;
    }

    public void setCallEndTimeout(Long callEndTimeout)
    {
        this.callEndTimeout = callEndTimeout;
    }

    public Long getCallStartTimeout()
    {
        return callStartTimeout;
    }

    public void setCallStartTimeout(Long callStartTimeout)
    {
        this.callStartTimeout = callStartTimeout;
    }

    public Boolean getMonitorTransfer()
    {
        return monitorTransfer;
    }

    public void setMonitorTransfer(Boolean monitorTransfer)
    {
        this.monitorTransfer = monitorTransfer;
    }

    public IvrAction createAction()
    {
        return new TransferCallAction(this);
    }
}

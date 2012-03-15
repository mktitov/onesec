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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class TimeWindowsGroupActionNode extends BaseNode implements Viewable {

    @NotNull @Parameter(defaultValue="false")
    private Boolean invertResult;
    
    @Message private static String currentTimeInPeriodMessage;
    @Message private static String yesMessage;
    @Message private static String noMessage;
    @Message private static String resultInvertedMessage;
    
    private TimeWindowsNode timeWindows;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes();
    }
    
    private void initNodes() {
        timeWindows = (TimeWindowsNode) getChildren(TimeWindowsNode.NAME);
        if (timeWindows==null) {
            timeWindows = new TimeWindowsNode();
            addAndSaveChildren(timeWindows);
            timeWindows.start();
        }
    }

    public TimeWindowsNode getTimeWindows() {
        return timeWindows;
    }

    public Boolean getInvertResult() {
        return invertResult;
    }

    public void setInvertResult(Boolean invertResult) {
        this.invertResult = invertResult;
    }

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens() {
        if (getStatus()!=Node.Status.STARTED)
            return null;
        return timeWindows.isCurrentTimeInPeriod()? super.getEffectiveChildrens() : null;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        String inv = invertResult? " ("+resultInvertedMessage+")" : "";
        String mess = currentTimeInPeriodMessage+inv+(timeWindows.isCurrentTimeInPeriod()?yesMessage:noMessage);
        return Arrays.asList((ViewableObject)new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, mess));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}

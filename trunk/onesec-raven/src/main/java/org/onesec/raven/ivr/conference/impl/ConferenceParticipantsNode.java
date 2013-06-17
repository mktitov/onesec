/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.conference.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.tree.impl.BaseNode;
import static org.onesec.raven.ivr.conference.impl.ConferenceParticipantNode.*;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ChildsAsTableViewableObject;
/**
 *
 * @author Mikhail Titov
 */
public class ConferenceParticipantsNode extends BaseNode implements Viewable {
    public final static String NAME = "Participants";
    
    private final static String[] attrs = new String[]{JOIN_TIME_ATTR, DISCONNECT_TIME_ATTR};

    public ConferenceParticipantsNode() {
        super(NAME);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        return Arrays.asList((ViewableObject)new ChildsAsTableViewableObject(this, attrs, attrs));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}

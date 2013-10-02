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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.conference.Conference;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.InvisibleNode;
import static org.onesec.raven.ivr.conference.impl.ConferenceNode.*;
import org.raven.tree.Node;
import org.raven.tree.impl.AttributeValueVisualizer;
import org.raven.tree.impl.ChildsAsTableViewableObject;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class PlannedConferencesNode extends ContainerNode implements Viewable {
    public final static String NAME = "Planned";
    
    private final static String[] attrs = new String[]{CONFERENCE_NAME_ATTR, CONFERENCE_INITIATOR_ID_ATTR, 
        CONFERENCE_INITIATOR_NAME_ATTR, START_TIME_STR_ATTR, END_TIME_STR_ATTR, CHANNELS_COUNT_ATTR, 
        RECORD_CONFERENCE_ATTR, CURRENT_PARTICIPANTS_COUNT, REGISTERED_PARTICIPANTS_COUNT};
    
    private final static Comparator<Node> startTimeComparator = new Comparator<Node>() {
        public int compare(Node o1, Node o2) {
            return ((Conference)o2).getStartTime().compareTo(((Conference)o1).getStartTime());
        }
    };
    
    private final static AttributeValueVisualizer visualizer = new AttributeValueVisualizer() {
        public Object visualize(Node node, NodeAttribute attr) {
            final ConferenceNode conf = (ConferenceNode) node;
            String color = "black";
            if (conf.isActive())
                color = "green";
            else if (conf.getEndTime()!=null && conf.getEndTime().after(new Date()))
                color = "blue";
            String val = attr.getValue();
            return "<span style='color: "+color+"'>"+(val==null? "" : val)+"</span>";
        }
    };

    public PlannedConferencesNode() {
        super(NAME);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        return Arrays.asList((ViewableObject)new ChildsAsTableViewableObject(
                this, attrs, attrs, visualizer, startTimeComparator));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}

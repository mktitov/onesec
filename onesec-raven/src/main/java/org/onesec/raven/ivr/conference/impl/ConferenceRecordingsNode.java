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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ChildsAsTableViewableObject;
import static org.onesec.raven.ivr.conference.impl.ConferenceRecordingNode.*;
import org.raven.tree.Node;
import org.raven.tree.impl.AttributeValueVisualizer;
import org.raven.tree.impl.FileViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceRecordingsNode extends BaseNode implements Viewable {
    public final static String NAME = "Recordings";
    
    private final static String[] attrs = new String[]{RECORDING_START_TIME_ATTR, RECORDING_END_TIME_ATTR, 
        RECORDING_DURATION_ATTR, RECORDING_FILE_ATTR};
    
    private final static AttributeValueVisualizer VISUALIZER = new AttributeValueVisualizer() {
        public Object visualize(Node node, NodeAttribute attr) {
            return RECORDING_FILE_ATTR.equals(attr.getName())?
                new FileViewableObject(new File(attr.getValue()), node) : attr.getValue();
        }
    };
    
    public ConferenceRecordingsNode() {
        super(NAME);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        return Arrays.asList((ViewableObject)new ChildsAsTableViewableObject(this, attrs, attrs, VISUALIZER, null));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}

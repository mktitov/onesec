package org.onesec.raven.ivr;

import org.onesec.core.State;

public interface SipTerminalState extends State<SipTerminalState, SipTerminal> {
    public final static int OUT_OF_SERVICE = 1;
    public final static int IN_SERVICE = 2;
    public final static int INVITING = 3;
    public final static int ACCEPTING_CALL = 4;
    public final static int TALKING = 5;
}
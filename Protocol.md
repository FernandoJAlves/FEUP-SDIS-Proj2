# Protocol messages

All messages include an identifier string and the sender's NodeInfo.

Messages sent from A to B.

    AliveMessage: RESPONSE
    Observer: KeepAliveObserver, PermanentObserver
            message(ISALIVE)
        A response message sent from a predecessor A to a successor B that
        informs B that A is still alive and should not be forgotten.

    KeepAliveMessage: REQUEST
    Observer: AliveObserver, TimeoutObserver
            message(KEEPALIVE)
        A request message sent from a successor A to a predecessor B that incites
        B to respond with an AliveMessage or else be forgotten.

    LookupMessage: REQUEST, chained
    Observer: ResponsibleObserver(chordid), TimeoutObserver
              JoinObserver(chordid), TimeoutObserver
            message(LOOKUP)
            + chordid + source
        A request message that might get chained through the chord network
        an incites a ResponsibleMessage response to the original sender (source, A).

    PredecessorMessage: RESPONSE
    Observer: StabilizeObserver, PermanentObserver
            message(PREDECESSOR)
            + predecessor
        A response message to a StabilizeMessage request sent from B that includes
        A's predecessor's NodeInfo. The predecessor will never be null,
        as the receiver will adopt B as its predecessor if it doesn't have one.

    StabilizeMessage: REQUEST
    Observer: PredecessorObserver, PermanentObserver
            message(STABILIZE)
        A request message sent from a predecessor A to a successor B, that asks
        the successor B to send A its predecessor. The successor B should adopt A
        as its predecessor if it doesn't know one.

    ResponsibleMessage: RESPONSE
    Observer: LookupObserver, PermanentObserver
            message(RESPONSIBLE, ChordID)
        A response message to a LookupMessage request sent from node B, and
        that implies A is the node responsible for the looked up key ChordId.

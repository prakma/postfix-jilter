/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter;

import com.sendmail.jilter.internal.JilterConstants;

import java.io.IOException;

import java.nio.channels.WritableByteChannel;

/**
 * Status class for methods in {@link JilterHandler}.
 */

public abstract class JilterStatus
{
    protected JilterStatus()
    {
    }

    abstract public void sendReplyPacket(WritableByteChannel writeChannel)
        throws IOException;

    /**
     * Creates a new JilterStatus with a custom error reply code.
     *
     * @param rcode The three-digit (RFC 821/2821) SMTP reply code. rcode cannot be null, and must
     *              be a valid 4XX or 5XX reply code.
     * @param xcode The extended (RFC 1893/2034) reply code. If xcode is null, no extended code is
     *              used. Otherwise, xcode must conform to RFC 1893/2034.
     * @param messageLines An array of single lines of text which will be used as the text part of
     *                     the SMTP reply. If messageLines has zero lines, an empty message is used.
     * @return the new status 
     * @throws IllegalArgumentException if rcode or xcode is invalid
     */
    public static JilterStatus makeCustomStatus(String rcode, String xcode, String[] messageLines)
        throws IllegalArgumentException
    {
        return new CustomJilterStatus(rcode, xcode, messageLines);
    }

    /**
     * Continue processing the current connection, message, or recipient.
     */
    public static final JilterStatus SMFIS_CONTINUE = new SimpleJilterStatus(JilterConstants.SMFIR_CONTINUE);

    /**
     * Rejection.
     * For a connection-oriented routine, reject this connection; call {@link JilterHandler#close close}.
     * For a message-oriented routine (except {@link JilterHandler#eom eom} or {@link JilterHandler#abort abort}),
     * reject this message.
     * For a recipient-oriented routine, reject the current recipient (but continue processing the current message).
     */
    public static final JilterStatus SMFIS_REJECT = new SimpleJilterStatus(JilterConstants.SMFIR_REJECT);
    
    /**
     * Message discard. For a message- or recipient-oriented routine, accept this message, but silently discard it.
     * {@link #SMFIS_DISCARD SMFIS_DISCARD} should not be returned by a connection-oriented routine.
     */
    public static final JilterStatus SMFIS_DISCARD = new SimpleJilterStatus(JilterConstants.SMFIR_DISCARD);

    /**
     * Acceptance. For a connection-oriented routine, accept this connection without further filter processing;
     * call {@link JilterHandler#close close}.
     * For a message- or recipient-oriented routine, accept this message without further filtering.
     */
    public static final JilterStatus SMFIS_ACCEPT = new SimpleJilterStatus(JilterConstants.SMFIR_ACCEPT);

    /**
     * Return a temporary failure, i.e., the corresponding SMTP command will return an appropriate 4xx status code.
     * For a message-oriented routine (except {@link JilterHandler#envfrom envfrom}), fail for this message. 
     * For a connection-oriented routine, fail for this connection; call {@link JilterHandler#close close}. 
     * For a recipient-oriented routine, only fail for the current recipient; continue message processing.
     */
    public static final JilterStatus SMFIS_TEMPFAIL = new SimpleJilterStatus(JilterConstants.SMFIR_TEMPFAIL);
}

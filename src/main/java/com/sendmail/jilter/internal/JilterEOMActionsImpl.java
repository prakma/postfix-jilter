/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter.internal;

import com.sendmail.jilter.JilterEOMActions;
import com.sendmail.jilter.JilterStatus;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.WritableByteChannel;

/**
 */

public class JilterEOMActionsImpl implements JilterEOMActions
{
    WritableByteChannel writeChannel = null;

    public JilterEOMActionsImpl(WritableByteChannel writeChannel)
    {
        this.writeChannel = writeChannel;
    }

    public void addheader(String headerf, String headerv)
        throws IOException
    {
        JilterServerPacketUtil.sendAddHeaderPacket(this.writeChannel, headerf, headerv);
    }

    public void chgheader(String headerf, int hdridx, String headerv)
        throws IOException
    {
        JilterServerPacketUtil.sendChgHeaderPacket(this.writeChannel, hdridx, headerf, headerv);
    }

    public void addrcpt(String rcpt)
        throws IOException
    {
        JilterServerPacketUtil.sendAddRcptPacket(this.writeChannel, rcpt);
    }

    public void delrcpt(String rcpt)
        throws IOException
    {
        JilterServerPacketUtil.sendDelRcptPacket(this.writeChannel, rcpt);
    }

    public void replacebody(ByteBuffer bodyp)
        throws IOException
    {
        JilterServerPacketUtil.sendReplBodyPacket(this.writeChannel, bodyp);
    }

    public void progress()
        throws IOException
    {
        JilterServerPacketUtil.sendProgressPacket(this.writeChannel);
    }

    public void finish(JilterStatus status)
        throws IOException
    {
        status.sendReplyPacket(this.writeChannel);
        this.writeChannel = null;
    }
}

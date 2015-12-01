/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter;

import com.sendmail.jilter.internal.JilterServerPacketUtil;

import java.io.IOException;

import java.nio.channels.WritableByteChannel;

/**
 * Jilter status class for simple SMFIR_ status codes.
 */

class SimpleJilterStatus extends JilterStatus
{
    private int status = 0;

    protected SimpleJilterStatus(int status)
    {
        this.status = status;
    }

    public void sendReplyPacket(WritableByteChannel writeChannel) throws IOException
    {
        JilterServerPacketUtil.sendPacket(writeChannel, this.status, null);
    }
}

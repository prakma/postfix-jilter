/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter.internal;

/**
 */

public class JilterConstants
{
    public static final int SMFIC_CONNECT = 'C';
    public static final int SMFIC_MACRO = 'D';
    public static final int SMFIC_HELO = 'H';
    public static final int SMFIC_MAIL = 'M';
    public static final int SMFIC_RCPT = 'R';
    public static final int SMFIC_BODYEOB = 'E';
    public static final int SMFIC_HEADER = 'L';
    public static final int SMFIC_EOH = 'N';
    public static final int SMFIC_OPTNEG = 'O';
    public static final int SMFIC_QUIT = 'Q';
    public static final int SMFIC_BODY = 'B';
    public static final int SMFIC_DATA = 'T';
    public static final int SMFIC_ABORT = 'A';

    public static final int SMFIR_ACCEPT = 'a';
    public static final int SMFIR_ADDHEADER = 'h';
    public static final int SMFIR_ADDRCPT = '+';
    public static final int SMFIR_CHGHEADER = 'm';
    public static final int SMFIR_CONTINUE = 'c';
    public static final int SMFIR_DELRCPT = '-';
    public static final int SMFIR_DISCARD = 'd';
    public static final int SMFIR_PROGRESS = 'p';
    public static final int SMFIR_REJECT = 'r';
    public static final int SMFIR_REPLBODY = 'b';
    public static final int SMFIR_REPLYCODE = 'y';
    public static final int SMFIR_TEMPFAIL = 't';

    public static final int SMFIA_INET = '4';
    public static final int SMFIA_INET6 = '6';

    public static final int SMFIP_NOCONNECT = 0x0001;
    public static final int SMFIP_NOHELO = 0x0002;
    public static final int SMFIP_NOMAIL = 0x0004;
    public static final int SMFIP_NORCPT = 0x0008;
    public static final int SMFIP_NOBODY = 0x0010;
    public static final int SMFIP_NOHDRS = 0x0020;
    public static final int SMFIP_NOEOH = 0x0040;

    private JilterConstants()
    {
    }
}

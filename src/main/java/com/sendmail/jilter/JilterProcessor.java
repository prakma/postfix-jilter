/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import org.apache.log4j.Category;

import com.sendmail.jilter.internal.JilterConstants;
import com.sendmail.jilter.internal.JilterEOMActionsImpl;
import com.sendmail.jilter.internal.JilterPacket;
import com.sendmail.jilter.internal.JilterServerPacketUtil;

/**
 * The guts of handling the filter side of the Milter protocol. If you have your own
 * way that you like to handle communicating with the MTA side of the Milter protocol,
 * you can feed an instance of this class the bytes from the MTA, and it will handle
 * calling methods in a {@link JilterHandler}, as well as sending data back to the MTA
 * via an arbitrary {@link WritableByteChannel}.
 */

public class JilterProcessor
{
	private static Category log = Category.getInstance(JilterProcessor.class.getName());

	private JilterHandler handler = null;
	private final JilterPacket packet = new JilterPacket();
	private Properties lastProperties = null;

	/**
	 * Public constructor.
	 * 
	 * @param handler
	 *            the underlying handler that will receive calls based on the Milter conversation.
	 */
	public JilterProcessor(final JilterHandler handler)
	{
		this.handler = handler;
	}

	/**
	 * Process more data from the MTA.
	 * 
	 * @param writeChannel
	 *            the data channel for communicating back to the MTA.
	 * @param dataBuffer
	 *            the next chunk of data from the MTA.
	 * @return <code>false</code> if processing is completed.
	 */
	public boolean process(final WritableByteChannel writeChannel, final ByteBuffer dataBuffer) throws IOException
	{
		while (packet.process(dataBuffer))
		{
			if (!processCurrentPacket(writeChannel))
			{
				return false;
			}

			packet.reset();
		}

		return true;
	}

	private static boolean isBitSet(final int bit, final int position)
	{
		return (bit & position) != 0;
	}

	private boolean processCurrentPacket(final WritableByteChannel writeChannel) throws IOException
	{
		boolean returnCode = true;

		if (log.isDebugEnabled())
		{
			// log.debug(">SMFIC command is '" + ((char) this.packet.getCommand()) + "', Raw packet data:" + Util.newline() +
			// Util.hexDumpLong(this.packet.getData()));
			log.debug(">SMFIC command is '" + (char) packet.getCommand() + "'");
		}

		switch (packet.getCommand())
		{
		case JilterConstants.SMFIC_CONNECT:
			log.debug("SMFIC_CONNECT");
			processConnectPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_MACRO:
			log.debug("SMFIC_MACRO");
			processMacroPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_HELO:
			log.debug("SMFIC_HELO");
			processHeloPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_MAIL:
			log.debug("SMFIC_MAIL");
			processMailPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_RCPT:
			log.debug("SMFIC_RCPT");
			processRcptPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_BODYEOB:
			log.debug("SMFIC_BODYEOB");
			processBodyEOBPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_HEADER:
			log.debug("SMFIC_HEADER");
			processHeaderPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_EOH:
			log.debug("SMFIC_EOH");
			processEOHPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_OPTNEG:
			log.debug("SMFIC_OPTNEG");
			processOptnegPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_QUIT:
			log.debug("SMFIC_QUIT");
			returnCode = false;
			break;

		case JilterConstants.SMFIC_BODY:
			log.debug("SMFIC_BODY");
			processBodyPacket(writeChannel);
			break;

		case JilterConstants.SMFIC_ABORT:
			log.debug("SMFIC_ABORT");
			processAbortPacket(writeChannel);
			break;
		case JilterConstants.SMFIC_DATA:
			// don't proccess should be skipped
			JilterServerPacketUtil.sendPacket(writeChannel, JilterConstants.SMFIR_CONTINUE, null);
			break;
		default:
			log.error("Unhandled case [" + packet.getCommand() + "]", new Exception());
			JilterServerPacketUtil.sendPacket(writeChannel, JilterConstants.SMFIR_CONTINUE, null);
			break;
		}

		return returnCode;
	}

	private void processOptnegPacket(final WritableByteChannel writeChannel) throws IOException
	{
		final int smfif = handler.getRequiredModifications();
		int smfip = 0;
		final int supported = handler.getSupportedProcesses();

		if (!isBitSet(supported, JilterHandler.PROCESS_CONNECT))
		{
			smfip |= JilterConstants.SMFIP_NOCONNECT;
		}
		if (!isBitSet(supported, JilterHandler.PROCESS_HEADER))
		{
			smfip |= JilterConstants.SMFIP_NOHDRS;
		}
		if (!isBitSet(supported, JilterHandler.PROCESS_HELO))
		{
			smfip |= JilterConstants.SMFIP_NOHELO;
		}
		if (!isBitSet(supported, JilterHandler.PROCESS_BODY))
		{
			smfip |= JilterConstants.SMFIP_NOBODY;
		}
		if (!isBitSet(supported, JilterHandler.PROCESS_ENVRCPT))
		{
			smfip |= JilterConstants.SMFIP_NORCPT;
		}
		if (!isBitSet(supported, JilterHandler.PROCESS_ENVFROM))
		{
			smfip |= JilterConstants.SMFIP_NOMAIL;
		}

		log.debug("Supported flags " + Integer.toHexString(supported) + " maps to SMFIP_ flags "
				+ Integer.toHexString(smfip));

		final ByteBuffer optionData = ByteBuffer.wrap(new byte[] { 0x00, 0x00, 0x00, 0x02, // version
				0x00, 0x00, 0x00, (byte) smfif, // SMFIF_
				0x00, 0x00, 0x00, (byte) smfip, // SMFIP_
		});
		JilterServerPacketUtil.sendPacket(writeChannel, JilterConstants.SMFIC_OPTNEG, optionData);
	}

	private void processBodyPacket(final WritableByteChannel writeChannel) throws IOException
	{
		sendReplyPacket(writeChannel, handler.body(packet.getData()));
	}

	private void processEOHPacket(final WritableByteChannel writeChannel) throws IOException
	{
		sendReplyPacket(writeChannel, handler.eoh());
	}

	private void processHeaderPacket(final WritableByteChannel writeChannel) throws IOException
	{
		String name = null;
		String value = null;
		final ByteBuffer dataBuffer = packet.getData();

		// char name[]

		name = JilterServerPacketUtil.getZeroTerminatedString(dataBuffer);

		// char value[]

		value = JilterServerPacketUtil.getZeroTerminatedString(dataBuffer);

		sendReplyPacket(writeChannel, handler.header(name, value));
	}

	private void processBodyEOBPacket(final WritableByteChannel writeChannel) throws IOException
	{
		JilterStatus status;
		JilterEOMActions eomactions;

		eomactions = new JilterEOMActionsImpl(writeChannel);

		status = handler.eom(eomactions, lastProperties);
		if (status != null)
		{
			eomactions.finish(status);
		}
	}

	private void processRcptPacket(final WritableByteChannel writeChannel) throws IOException
	{
		String argv[] = null;
		final ByteBuffer dataBuffer = packet.getData();

		// char args[][]

		argv = JilterServerPacketUtil.getZeroTerminatedStringArray(dataBuffer);
		log.debug("Recipient is \"" + argv[0] + "\"");

		sendReplyPacket(writeChannel, handler.envrcpt(argv, lastProperties));
	}

	private void processMailPacket(final WritableByteChannel writeChannel) throws IOException
	{
		String argv[] = null;
		final ByteBuffer dataBuffer = packet.getData();

		// char args[][]

		argv = JilterServerPacketUtil.getZeroTerminatedStringArray(dataBuffer);
		log.debug("Sender is \"" + argv[0] + "\"");

		sendReplyPacket(writeChannel, handler.envfrom(argv, lastProperties));
	}

	private void processHeloPacket(final WritableByteChannel writeChannel) throws IOException
	{
		String helohost = null;
		final ByteBuffer dataBuffer = packet.getData();

		// char helo[]

		helohost = JilterServerPacketUtil.getZeroTerminatedString(dataBuffer);
		log.debug("Client identifier parsed as \"" + helohost + "\"");

		sendReplyPacket(writeChannel, handler.helo(helohost, lastProperties));
	}

	private void processMacroPacket(final WritableByteChannel writeChannel)
	{
		final ByteBuffer dataBuffer = packet.getData();
		String[] propertiesStrings = null;

		// char cmdcode

		dataBuffer.get();

		// char nameval[][]

		propertiesStrings = JilterServerPacketUtil.getZeroTerminatedStringArray(dataBuffer);
		lastProperties = new Properties();

		for (int counter = 0; counter < propertiesStrings.length; counter += 2)
		{
			log.debug("Setting property " + propertiesStrings[counter] + " = " + propertiesStrings[counter + 1]);
			lastProperties.setProperty(propertiesStrings[counter], propertiesStrings[counter + 1]);
		}

		// No reply at all...
	}

	private void processConnectPacket(final WritableByteChannel writeChannel) throws IOException
	{
		InetAddress address = null;
		final ByteBuffer dataBuffer = packet.getData();
		String hostname = null;

		// char hostname[]

		hostname = JilterServerPacketUtil.getZeroTerminatedString(dataBuffer);

		// char family

		if (dataBuffer.get() == JilterConstants.SMFIA_INET)
		{
			// uint16 port

			dataBuffer.getShort();

			// char address[]

			{
				String stringAddress = null;

				stringAddress = JilterServerPacketUtil.getZeroTerminatedString(dataBuffer);
				log.debug("Parsed IP address is " + stringAddress);
				address = InetAddress.getByName(stringAddress);
			}
		}

		sendReplyPacket(writeChannel, handler.connect(hostname, address, lastProperties));
	}

	private void sendReplyPacket(final WritableByteChannel writeChannel, final JilterStatus status) throws IOException
	{
		status.sendReplyPacket(writeChannel);
	}

	private void processAbortPacket(final WritableByteChannel writeChannel)
	{
		handler.abort();

		// No reply at all...
	}

	/**
	 * Closes this processor. Will do the right thing to communicate to the underlying handler
	 * that processing is completed.
	 */
	public void close()
	{
		packet.reset();
		handler.close();
		lastProperties = null;
	}
}

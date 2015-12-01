/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter.samples.standalone;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Category;

import com.sendmail.jilter.JilterHandler;
import com.sendmail.jilter.JilterProcessor;

/**
 * Sample implementation of a handler for a socket based Milter protocol connection.
 */

class ServerRunnable implements Runnable
{
	private static Category log = Category.getInstance(ServerRunnable.class.getName());

	private SocketChannel socket = null;
	private JilterProcessor processor = null;

	/**
	 * Constructor.
	 * 
	 * @param socket
	 *            the incoming socket from the MTA.
	 * @param handler
	 *            the handler containing callbacks for the milter protocol.
	 */
	public ServerRunnable(final SocketChannel socket, final JilterHandler handler) throws IOException
	{
		this.socket = socket;
		this.socket.configureBlocking(true);
		processor = new JilterProcessor(handler);
	}

	
	public void run()
	{
		final ByteBuffer dataBuffer = ByteBuffer.allocateDirect(4096);
		final long start = System.currentTimeMillis();
		try
		{
			while (processor.process(socket, (ByteBuffer) dataBuffer.flip()))
			{
				dataBuffer.compact();
				log.debug("Going to read [" + hashCode() + "]");
				if (socket.read(dataBuffer) == -1)
				{
					log.debug("socket reports EOF, exiting read loop [" + hashCode() + "]");
					break;
				}
				else
				{
					log.debug("Back from read [" + hashCode() + "]");
				}
			}
		}
		catch (final IOException e)
		{
			log.debug("Unexpected exception, connection will be closed [" + hashCode() + "]", e);
		}
		finally
		{
			processor.close();
			try
			{
				socket.close();
				log.info("Socket closed, work tok [" + hashCode() + "][" + (System.currentTimeMillis() - start) + "]");
			}
			catch (final IOException e)
			{
				log.debug("Unexpected exception [" + hashCode() + "]", e);
			}
		}
	}
}

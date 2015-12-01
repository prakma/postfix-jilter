/*
 * Copyright (c) 2001-2004 Sendmail, Inc. All Rights Reserved
 */

package com.sendmail.jilter.samples.standalone;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import com.sendmail.jilter.samples.standalone.IJilterHandlerFactory;

/**
 * Simple Jilter server for handling connections from an MTA.
 */

public class SimpleJilterServer implements Runnable
{
	private static Category log = Category.getInstance(SimpleJilterServer.class.getName());

	private ServerSocketChannel serverSocketChannel = null;
	private IJilterHandlerFactory factory = null;
	private boolean shutdown = false;
	/**
	 * pool for event execution
	 */
	private final Executor pool = new ThreadPoolExecutor(5, 50, 30, TimeUnit.MINUTES,
			new ArrayBlockingQueue<Runnable>(10000), new ThreadFactory()
			{
				/**
				 * group
				 */
				private final ThreadGroup group = new ThreadGroup(Thread.currentThread().getThreadGroup(),
						"Jilter ConnectionWorker");
				/**
				 * incrementor
				 */
				private int count = 0;

				
				public Thread newThread(final Runnable r)
				{
					final Thread th = new Thread(group, r);
					th.setDaemon(true);
					final String name = "Jilter ConnectionWorker-" + count++;
					th.setName(name);
					log.debug("Created thread, " + name);
					return th;
				}
			}, new ThreadPoolExecutor.CallerRunsPolicy());

	
	public void run()
	{
		while (!shutdown)
		{
			SocketChannel connection = null;
			try
			{
				log.debug("Wait for connection");
				connection = serverSocketChannel.accept();
				final ServerRunnable command = new ServerRunnable(connection, factory.newInstance());
				pool.execute(command);
				log.debug("Start connection runnable Jilter [" + connection.socket() + "][" + command.hashCode() + "]");
			}
			catch (final IOException e)
			{
				log.debug("Unexpected exception", e);
			}
		}
	}

	/**
	 * blocking call waits for server termination
	 */
	public void shutdown()
	{
		shutdown = true;
	}

	public SocketAddress getSocketAddress()
	{
		return serverSocketChannel.socket().getLocalSocketAddress();
	}

	public SimpleJilterServer(final SocketAddress endpoint, final IJilterHandlerFactory factory) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		this.factory = factory;

		// Fire up a test handler and immediately close it to make sure everything's
		// OK.

		log.debug("Opening socket");
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(true);
		log.debug("Binding to endpoint " + endpoint);
		serverSocketChannel.socket().bind(endpoint);
		log.debug("Bound to " + getSocketAddress());
	}

	private static class SimpleGetopt
	{
		private String optstring = null;
		private String[] args = null;
		private int argindex = 0;
		private String optarg = null;

		public SimpleGetopt(final String[] args, final String optstring)
		{
			this.args = args;
			this.optstring = optstring;
		}

		public int nextopt()
		{
			int argChar = -1;

			for (int counter = argindex; counter < args.length; ++counter)
			{
				if (args[counter] != null && args[counter].length() > 1 && args[counter].charAt(0) == '-')
				{
					int charIndex = 0;

					log.debug("Found apparent argument " + args[counter]);

					argChar = args[counter].charAt(1);
					charIndex = optstring.indexOf(argChar);
					optarg = null;
					if (charIndex != -1)
					{
						argindex = counter + 1;

						if (optstring.length() > charIndex + 1 && optstring.charAt(charIndex + 1) == ':')
						{
							log.debug("Argument apparently requires a parameter");
							if (args[counter].length() > 2)
							{
								optarg = args[counter].substring(2).trim();
							}
							else if (args.length > counter + 1)
							{
								optarg = args[counter + 1];
								++argindex;
							}
							log.debug("Parameter is " + optarg);
						}
					}
					break;
				}
			}

			return argChar;
		}

		public String getOptarg()
		{
			return optarg;
		}
	}

	private static SocketAddress parseSocketAddress(final String address) throws UnknownHostException
	{
		final Pattern pattern = Pattern.compile("inet\\s*:\\s*(\\d+)\\s*@\\s*(\\S+)");
		final Matcher matcher = pattern.matcher(address);

		if (!matcher.matches())
		{
			System.out.println("Unrecognized port \"" + address + "\"");
			return null;
		}

		log.debug("Successfully parsed socket address, port is " + matcher.group(1) + ", host is " + matcher.group(2));
		return new InetSocketAddress(InetAddress.getByName(matcher.group(2)), Integer.parseInt(matcher.group(1)));
	}

	private static void usage()
	{
		System.out.println("Usage: -p <port information> -c <handler class> [-v]");
		System.out.println();
		System.out.println("       -p <port information> -- the port to listen on");
		System.out.println("       -c <handler class> -- a class implementing the IJilterHandlerFactory interface");
		System.out.println("       -v -- turn on verbosity");
		System.out.println();
		System.out.println("       <port information> is of the format \"inet:port@host\"");
	}

	public static void main(final String[] args) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException
	{
		final SimpleGetopt options = new SimpleGetopt(args, "p:c:v");
		String handlerClass = null;
		SocketAddress socketAddress = null;

		if (System.getProperty("log4j.configuration") == null)
		{
			BasicConfigurator.configure();
			Category.getRoot().setLevel(Level.INFO);

			log.debug("Initialized using BasicConfigurator from main");
		}
		
		Category.getRoot().setLevel(Level.INFO);

		while (true)
		{
			final int option = options.nextopt();

			if (option == -1)
			{
				break;
			}

			switch (option)
			{
			case 'c':
				handlerClass = options.getOptarg();
				log.debug("Handler class specified is " + handlerClass);
				break;

			case 'p':
				log.debug("Socket address specified is " + options.getOptarg());
				socketAddress = parseSocketAddress(options.getOptarg());
				break;

			case 'v':
				
				log.debug("Verbosity turned on");
				break;
				
			default:
				log.debug("Command Option is "+option);
			};
			
			
			
		}

		if (socketAddress == null || handlerClass == null)
		{
			usage();
			System.exit(1);
		}
		log.debug("End-of-program. Bye !");

		new SimpleJilterServer(socketAddress, (IJilterHandlerFactory) ( (Class.forName(handlerClass)).newInstance() ) ).run();
	}
}

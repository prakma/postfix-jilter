package com.sendmail.jilter.samples.standalone;

import com.sendmail.jilter.JilterHandler;

/**
 * factory to produce filterhanlder
 *
 * @author Xyan
 *
 */
public interface IJilterHandlerFactory
{

	/**
	 * @return an new handler instance
	 */
	public JilterHandler newInstance();
}

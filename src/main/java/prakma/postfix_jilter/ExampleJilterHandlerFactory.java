package prakma.postfix_jilter;

import com.sendmail.jilter.JilterHandler;
import com.sendmail.jilter.samples.standalone.IJilterHandlerFactory;

public class ExampleJilterHandlerFactory implements IJilterHandlerFactory {

	public JilterHandler newInstance() {
		return new ExampleJilter();
	}

}

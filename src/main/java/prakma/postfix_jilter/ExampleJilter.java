package prakma.postfix_jilter;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.sendmail.jilter.JilterEOMActions;
import com.sendmail.jilter.JilterHandlerAdapter;
import com.sendmail.jilter.JilterStatus;


public class ExampleJilter extends JilterHandlerAdapter {
	
	private int stateCheck = 0;
	private static int instanceNo = 0;
	//private List<ByteBuffer> headerSequences = new ArrayList<ByteBuffer>(4);
	StringBuilder sbuilder = new StringBuilder();
	private List<ByteBuffer> rawBodySequences = new ArrayList<ByteBuffer>(16);
	private int bodySize = 0;
	
	private String [] senders;
	private String [] recipients;
	private LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
	private Date receivedDate;
	

	
	public ExampleJilter(){
		++instanceNo;

	}

	public int getSupportedProcesses() {
		// TODO Auto-generated method stub
		return PROCESS_ENVFROM | PROCESS_ENVRCPT | PROCESS_HEADER | PROCESS_BODY;
	}

	@Override
	public int getRequiredModifications()
    {
        // By default, do not modify anything
        return SMFIF_ADDHDRS | SMFIF_CHGBODY | SMFIF_ADDRCPT ;
    }
	
	public JilterStatus envfrom(String[] argv, Properties properties)
    {
//		for (String x : argv) {
//			System.out.println("envfrom "+x+",");
//			
//			//sender = x;
//		}
		senders = argv;
		receivedDate = Calendar.getInstance().getTime();
		
        return JilterStatus.SMFIS_CONTINUE;
    }

    public JilterStatus envrcpt(String[] argv, Properties properties)
    {
    	for (String x : argv) {
			System.out.println("envrcpt "+x+",");
			recipients = argv;
		}
        return JilterStatus.SMFIS_CONTINUE;
    }

    public JilterStatus header(String headerf, String headerv)
    {
//    	System.out.println("headerf-"+headerf );
//    	System.out.println("headerv-"+headerv );
    	
    	sbuilder.append(headerf)
    	.append(":")
    	.append(" ")
    	.append(headerv)
    	.append("\n");
    	
    	headerMap.put(headerf, headerv);
        return JilterStatus.SMFIS_CONTINUE;
    }

    

    public JilterStatus body(ByteBuffer bodyp)
    {
//    	System.out.println("received bytebuffer for body, hash is "+bodyp.hashCode()+", limit is "+bodyp.limit()+", position is "+bodyp.position() );
//    	System.out.println("current instance is "+instanceNo+" and current state of the handler is"+stateCheck);
    	stateCheck++;
    	bodySize += bodyp.limit();
    	rawBodySequences.add(bodyp);
        return JilterStatus.SMFIS_CONTINUE;
    }
    
    /**
     * Do your thing here. MimeMessage is ready to be used 
     */
    public JilterStatus eom(JilterEOMActions eomActions, Properties properties)
    {
    	//System.out.println("received eom" );
    	sbuilder.append("\n");
    	byte [] headersBeforeBody = sbuilder.toString().getBytes(); // headers will be all ASCII
    	ByteBuffer headersBuffer = ByteBuffer.wrap(headersBeforeBody);
    	rawBodySequences.add(0, headersBuffer);
    	
    	byte[] completeBody = new byte[headersBeforeBody.length + bodySize];
    	int offset = 0;
    	for (ByteBuffer byteBuffer : rawBodySequences) {
    		int bufferLength = byteBuffer.remaining();
			byteBuffer.get(completeBody, offset, bufferLength );
			offset = offset + bufferLength;
			
		}
    	try{
    		ByteArrayInputStream in_stream = new ByteArrayInputStream(completeBody);
        	final Session mail_session = Session.getDefaultInstance ( new Properties () );
            final MimeMessage message = new MimeMessage ( mail_session, in_stream );
            // now do what you want
            
    	}catch(Exception ex){
    		throw new RuntimeException("Could not parse body content into MimeMessage, "+ex, ex);
    	}
    	   	
        return JilterStatus.SMFIS_CONTINUE;
    }
    
    
    
    

}

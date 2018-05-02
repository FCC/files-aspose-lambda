package gov.fcc.fileservice.lampufproc;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import gov.fcc.itc.utils.Constants;
import gov.fcc.itc.utils.FileUtil;
import gov.fcc.itc.utils.PostUploadFileProcessor;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class LambdaFunctionHandler implements RequestHandler<SNSEvent, String> {

	String bucket, uuid, fileName, hook, reportBackQueue;
	JSONArray process;
	Context mainContext;
	LambdaLogger logger;
	
    @Override
    public String handleRequest(SNSEvent event, Context context) {

    	mainContext = context;
    	logger = mainContext.getLogger();    	
    	
    	GetEnvironment();
    	//logger.log("Report Back : " + reportBackQueue);
        
        String message = ParseIncomingMessage(event);        
        //logger.log("Bucket : " + bucket);
        //logger.log("UUID : " + uuid);
        //logger.log("FileName : " + fileName);
        //logger.log("Process : " + process);
        //logger.log("Hook : " + hook);        	
        
		processFile();
		
        return message;
    }
    
    private void GetEnvironment() {
    	reportBackQueue = System.getenv("REPORT_BACK_QUEUE");
    }
    
    private String ParseIncomingMessage(SNSEvent event) {
    	
        String message = event.getRecords().get(0).getSNS().getMessage();

        JSONParser parser = new JSONParser();
    	
    	try {
    		JSONObject jsonMessage = (JSONObject) parser.parse(message);

    		bucket = (String) jsonMessage.get("bucket");
            uuid = (String) jsonMessage.get("uuid");
            fileName = (String) jsonMessage.get("fileName");
            process = (JSONArray) jsonMessage.get("process");
            
            hook = (String) jsonMessage.get("hook");
    	} catch (ParseException e) {
    		logger.log("Could not parse incoming message");
    	}    	
    	return message;
    }
    
	public void processFile() {
		final long startTime = System.currentTimeMillis();

		PostUploadFileProcessor pufProc = new PostUploadFileProcessor(process, logger);
		
		JSONObject response = null;
		boolean processedFlag = true;
		String status = null;
		String statusMessage = null;

		try {
			pufProc.LoadSourceFile(bucket, fileName);
			response = pufProc.ApplyOperations();						
		    status = Constants.PROCESSING_COMPLETE;
		    statusMessage = Constants.PROCESSING_SUCCESS_MESSAGE;
		    
		} catch (Exception e) {
			// Set overall failed status 
			e.printStackTrace();
			processedFlag = false;
			statusMessage = e.getMessage();
			status = Constants.PROCESSING_ERROR;
		}		

		String sendMessage = 
				"{\"uuid\":\"" + uuid +
				"\", \"status\":\"" + status + 
				"\", \"statusMessage\":\"" + statusMessage + 
				"\", \"bucket\":\"" + bucket;
		
		if (response == null) {
			sendMessage = sendMessage + "\"}";			
		} else {
			sendMessage = sendMessage + 
					"\", \"processed\":" + response.toString() +
					"}";						
		}
		//queueSender.send(sendMessage);
		
		final long endTime = System.currentTimeMillis();

		logger.log("#############" + sendMessage);
		logger.log("Total execution time: " + (endTime - startTime));
		logger.log("############:" + fileName);
		
		if(!processedFlag)
		{
			logger.log("Unable to process the file.");
		}
	}
}

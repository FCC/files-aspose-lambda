package gov.fcc.fileservice.lampufproc;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.*;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class LambdaFunctionHandler implements RequestHandler<SNSEvent, String> {

	String bucket, uuid, fileName, hook, reportBackQueue;
	JsonArray process;
	
    @Override
    public String handleRequest(SNSEvent event, Context context) {

    	context.getLogger().log("Received event: " + event);
    	
    	GetEnvironment();
        context.getLogger().log("Report Back : " + reportBackQueue);
        
        String message = ParseIncomingMessage(event);        
        context.getLogger().log("Bucket : " + bucket);
        context.getLogger().log("UUID : " + uuid);
        context.getLogger().log("FileName : " + fileName);
        context.getLogger().log("Process : " + process);
        context.getLogger().log("Hook : " + hook);        	
        
        return message;
    }
    
    private void GetEnvironment() {
    	reportBackQueue = System.getenv("REPORT_BACK_QUEUE");
    }
    
    private String ParseIncomingMessage(SNSEvent event) {
        String message = event.getRecords().get(0).getSNS().getMessage();
        JsonObject jsonMessage = new JsonParser().parse(message).getAsJsonObject();
        
        bucket = jsonMessage.get("bucket").getAsString();
        uuid = jsonMessage.get("uuid").getAsString();
        fileName = jsonMessage.get("fileName").getAsString();
        process = jsonMessage.get("process").getAsJsonArray();

        JsonElement jhook = jsonMessage.get("hook");
        if (jhook != null) {
            hook = jhook.getAsString();
        } else {
            hook = null;
        }
    	return message;
    }
    
}

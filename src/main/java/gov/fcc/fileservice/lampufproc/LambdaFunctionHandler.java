package gov.fcc.fileservice.lampufproc;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.*;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class LambdaFunctionHandler implements RequestHandler<SNSEvent, String> {

	String bucket, uuid, fileName, hook, reportBack;
	JsonArray process;
	
    @Override
    public String handleRequest(SNSEvent event, Context context) {
    	
    	String awsRegion = System.getenv("AWS_REGION");
        context.getLogger().log("Running in : " + awsRegion);
 
        context.getLogger().log("Received event: " + event);
        String message = event.getRecords().get(0).getSNS().getMessage();
        context.getLogger().log("From SNS: " + message);
        
        ParseIncomingMessage(message);
        
        context.getLogger().log("Bucket : " + bucket);
        context.getLogger().log("UUID : " + uuid);
        context.getLogger().log("FileName : " + fileName);
        context.getLogger().log("Process : " + process);
        context.getLogger().log("Report Back : " + reportBack);
        context.getLogger().log("Hook : " + hook);        	
        
        return message;
    }
    
    private void ParseIncomingMessage(String message) {
        JsonObject jsonMessage = new JsonParser().parse(message).getAsJsonObject();
        
        bucket = jsonMessage.get("bucket").getAsString();
        uuid = jsonMessage.get("uuid").getAsString();
        fileName = jsonMessage.get("fileName").getAsString();
        reportBack = jsonMessage.get("reportBack").getAsString();
        process = jsonMessage.get("process").getAsJsonArray();

        JsonElement jhook = jsonMessage.get("hook");
        if (jhook != null) {
            hook = jhook.getAsString();
        } else {
            hook = null;
        }
    	
    }
    
}

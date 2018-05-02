package gov.fcc.itc.utils;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class AwsUtil
{
	public static final String DEFAULT_S3_REGION = "us-west-2";
	public static final String S3_ENDPOINT_REGION = "S3_ENDPOINT";
	public String S3_BUCKET ;
	public static final String S3_CREDENTIALS_PROFILE = "fcc-opif";
	
	// Loads the region name from system properties. In Tomcat they are usually
	// setup in catalina.porperties file.
	// if not defined then defaults to west -2 region
	
	public String getS3EndPointRegion()
	{
		String regionName = DEFAULT_S3_REGION;
		if (!StringUtils.isEmpty(System.getProperty(S3_ENDPOINT_REGION)))
		{
			regionName = System.getProperty(S3_ENDPOINT_REGION);
		}
		return regionName;
	}

	// Loads the bucket name from system properties. In Tomcat they are usually
	// setup in catalina.porperties file
	public String getS3BucketName()
	{
		/*if (StringUtils.isEmpty(System.getProperty(S3_BUCKET_NAME)))
		{
			throw new RuntimeException(
					"Unable to find AWS S3 bucket name. Please check the server properties");
		}
		System.out.println ("S3_BUCKET_NAME: " + S3_BUCKET_NAME);
		return System.getProperty(S3_BUCKET_NAME);*/
		return this.S3_BUCKET;
	}
	public void setS3BucketName(String bucketName)
	{
		/*if (StringUtils.isEmpty(System.getProperty(S3_BUCKET_NAME)))
		{
			throw new RuntimeException(
					"Unable to find AWS S3 bucket name. Please check the server properties");
		}
		System.out.println ("S3_BUCKET_NAME: " + S3_BUCKET_NAME);
		return System.getProperty(S3_BUCKET_NAME);*/
		this.S3_BUCKET=bucketName;
		
	}

	// Creates the connection to specified S3 bucket.
	public AmazonS3 getAWSS3Client(String regionName, String bucketName)
	{
		if (StringUtils.isEmpty(regionName))
		{
			regionName = getS3EndPointRegion();
		}

		if (StringUtils.isEmpty(bucketName))
		{
			bucketName = getS3BucketName();
		}
		
		AmazonS3 conn;
		if (StringUtils.isNotBlank(System.getProperty("AWS_ACCESS_KEY")) 
				&& StringUtils.isNotBlank(System.getProperty("AWS_SECRET_KEY"))) {
			
			BasicAWSCredentials credentials = new BasicAWSCredentials(System.getProperty("AWS_ACCESS_KEY"), 
					System.getProperty("AWS_SECRET_KEY"));
			
			System.out.println("enetred here access key" +System.getProperty("AWS_ACCESS_KEY") );
			System.out.println("enetred here access key" +System.getProperty("AWS_SECRET_KEY") );
		    conn = new AmazonS3Client(credentials);
		    System.out.println("enetred her2323e");
		} else 
			conn = new AmazonS3Client ();
		
		//System.out.println("AWS CLIENT: " + conn.getS3AccountOwner().getDisplayName());
		
		conn.setEndpoint(createEndPoint(regionName, bucketName));
		
		return conn;
	}

	// Always go through HTTPS
	private String createEndPoint(String regionName, String bucketName)
	{
		URL endpointUrl;
		try
		{
			if (regionName.equals("us-east-1"))
			{
				endpointUrl = new URL("https://s3.amazonaws.com/" + bucketName);
			} else
			{
				endpointUrl = new URL("https://s3-" + regionName
						+ ".amazonaws.com/" + bucketName);
			}
		} catch (MalformedURLException e)
		{
			throw new RuntimeException("Unable to parse service endpoint: "
					+ e.getMessage());
		}
		System.out.println("enetred he343re" + endpointUrl.toString());
		return endpointUrl.toString();
	}

	
	public void copyObject(String sourceFileName, String destinationFilename)
	{
		copyObject(getS3EndPointRegion(),  getS3BucketName(),
				sourceFileName, destinationFilename);
		
	}

	public void copyObject(String regionName, String bucketName,
			String sourceFileName, String destinationFilename)
	{
		AmazonS3 client = getAWSS3Client(regionName, bucketName);
		if (StringUtils.isEmpty(bucketName))
		{
			bucketName = getS3BucketName();
		}
		
		//writing to S3
		try
		{
			 CopyObjectRequest copyObjRequest = new CopyObjectRequest(
	            		bucketName, sourceFileName, bucketName, destinationFilename).withCannedAccessControlList(CannedAccessControlList.PublicReadWrite);
			 client.copyObject(copyObjRequest);
			//client.copyObject(new CopyObjectRequest(bucketName, sourceFileName, bucketName, destinationFilename).setCannedAccessControlList(CannedAccessControlList.PublicReadWrite));
			//client.copyObject(bucketName, sourceFileName, bucketName, destinationFilename);
		} catch (AmazonServiceException ase)
		{
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			
			throw new RuntimeException(ase);
		} catch (AmazonClientException ace)
		{
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
			throw new RuntimeException(ace);
		}

	}
	
	public void putObject(
			String destinationFilename, InputStream sourceContent,
			ObjectMetadata metaData)
	{
		putObject(getS3EndPointRegion(),  getS3BucketName(),
				 destinationFilename,  sourceContent,
				 metaData);
		
	}
	
	public void putObject(String regionName, String bucketName,
			String destinationFilename, InputStream sourceContent,
			ObjectMetadata metaData)
	{
		AmazonS3 client = getAWSS3Client(regionName, bucketName);
		if (StringUtils.isEmpty(bucketName))
		{
			bucketName = getS3BucketName();
		}
		
		//writing to S3
		try
		{
			client.putObject(new PutObjectRequest(bucketName, destinationFilename, sourceContent, metaData).withCannedAcl(CannedAccessControlList.PublicReadWrite));
			//client.putObject(bucketName, destinationFilename, sourceContent, metaData);
		} catch (AmazonServiceException ase)
		{
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			
			throw new RuntimeException(ase);
		} catch (AmazonClientException ace)
		{
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
			throw new RuntimeException(ace);
		}

	}
	
	public byte[] getObject(String sourceFileName)
		{
				return  getObject( getS3EndPointRegion(),  getS3BucketName(),
						 sourceFileName);
		}
	public byte[] getObject(String regionName, String bucketName,
			String sourceFileName)
	{
		byte[] result=null;
		AmazonS3 client = getAWSS3Client(regionName, bucketName);
		if (StringUtils.isEmpty(bucketName))
		{
			bucketName = getS3BucketName();
		}
		
		//getting from S3
		try
		{
			S3Object s3Object = client.getObject(bucketName, sourceFileName);
			if (s3Object.getObjectContent() !=null)
			{
				result= IOUtils.toByteArray(s3Object.getObjectContent());
			}
			s3Object.close();//Very important to close the connection to S3 and release the resources Otherwise memory leaks.
		} catch (AmazonServiceException ase)
		{
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			
			throw new RuntimeException(ase);
		} catch (AmazonClientException ace)
		{
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
			throw new RuntimeException(ace);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return result;
	}
	
	//AWS Doesn't have straight API to know whether a key exists in S3 or not. Instead pulling the entire file object using GetObject method or Instead of looping
	//through all the keys in S3. This is the best approach to know whether a key exists or not.
	public boolean isFileExists(String regionName, String bucketName,
			String sourceFileName)
	{
		boolean fileExists=false;
		AmazonS3Client client =(AmazonS3Client) getAWSS3Client(regionName, bucketName);
		if (StringUtils.isEmpty(bucketName))
		{
			bucketName = getS3BucketName();
		}
			try{
					ObjectMetadata metaData = client.getObjectMetadata(bucketName, sourceFileName);
					if(metaData!=null)
					{
						fileExists=true;
					}
			}catch (AmazonServiceException ase)
			{
				System.out.println("Caught an AmazonServiceException, which "
						+ "means your request made it "
						+ "to Amazon S3, but was rejected with an error response"
						+ " for some reason.");
				System.out.println("Error Message:    " + ase.getMessage());
				System.out.println("HTTP Status Code: " + ase.getStatusCode());
				System.out.println("AWS Error Code:   " + ase.getErrorCode());
				System.out.println("Error Type:       " + ase.getErrorType());
				System.out.println("Request ID:       " + ase.getRequestId());
				
				fileExists=false;
			} catch (AmazonClientException ace)
			{
				System.out.println("Caught an AmazonClientException, which "
						+ "means the client encountered "
						+ "an internal error while trying to "
						+ "communicate with S3, "
						+ "such as not being able to access the network.");
				System.out.println("Error Message: " + ace.getMessage());
				
				fileExists=false;
			}
			
		
		return fileExists;
	}
	
	public boolean isFileExists(String sourceFileName)
	{
		String regionName = getS3EndPointRegion();
		String bucketName = getS3BucketName();
				
		return isFileExists(regionName, bucketName,
				 sourceFileName);
	}
	
	
	public String getFileSize (String fileName, String regionName, String bucketName) {

		String fileSize = null;
		if (isFileExists(regionName, bucketName,fileName)) {

			if (StringUtils.isEmpty(bucketName))
			{
				bucketName = getS3BucketName();
			}

			AmazonS3Client client =(AmazonS3Client) getAWSS3Client(regionName, bucketName);
			
			try{
				ObjectMetadata metaData = client.getObjectMetadata(bucketName, fileName);
				if(metaData!=null)
				{
							//fileSize = FileUtil.formatFileSize(metaData.getContentLength(), true);
							//FileUtils.byteCountToDisplaySize(metaData.getContentLength());
					fileSize = String.valueOf(metaData.getContentLength());
						}
				}catch (AmazonServiceException ase)
				{
					System.out.println("Caught an AmazonServiceException, which "
							+ "means your request made it "
							+ "to Amazon S3, but was rejected with an error response"
							+ " for some reason.");
					System.out.println("Error Message:    " + ase.getMessage());
					System.out.println("HTTP Status Code: " + ase.getStatusCode());
					System.out.println("AWS Error Code:   " + ase.getErrorCode());
					System.out.println("Error Type:       " + ase.getErrorType());
					System.out.println("Request ID:       " + ase.getRequestId());

				} catch (AmazonClientException ace)
				{
					System.out.println("Caught an AmazonClientException, which "
							+ "means the client encountered "
							+ "an internal error while trying to "
							+ "communicate with S3, "
							+ "such as not being able to access the network.");
					System.out.println("Error Message: " + ace.getMessage());
					
				}
		}
		
		 return fileSize;
	}

	public String getFileSize (String fileName) {

		String regionName = getS3EndPointRegion();
		String bucketName = getS3BucketName();
	
		return getFileSize (fileName, regionName, bucketName);

	}

}

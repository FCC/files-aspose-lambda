package gov.fcc.itc.utils;

import com.aspose.words.License;

public class AsposeLicense {

    public static void applyLicense() throws Exception
    {
        // This line attempts to set a license from several locations relative to the executable and Aspose.Words.dll.
        // You can also use the additional overload to load a license from a stream, this is useful for instance when the
        // license is stored as an embedded resource
        try
        {
 
            String license = AsposeLicense.class.getClassLoader().getResource("Aspose.Total.Java.lic").getPath();
         	
        	System.out.println ("**********In applyLicense method. license: " + license);

            //Set Aspose Word License
            License wordLicense = new License();
            wordLicense.setLicense(license);
            
            //Set Aspose Cells License
            com.aspose.cells.License cellsLicense = new com.aspose.cells.License();
            cellsLicense.setLicense(license);

            //Set Aspose PDF License
            com.aspose.pdf.License pdfLicense = new com.aspose.pdf.License();
            pdfLicense.setLicense(license);
        
            //Set Aspose Slides License
            com.aspose.slides.License slidesLicense = new com.aspose.slides.License();
            slidesLicense.setLicense(license);

        }
        catch (Exception e)
        {
            System.out.println("Error setting the license: " + e.getMessage());
        }
    }
}
/****************************************************************
 * ElasticWarehouse - File storage based on ElasticSearch
 * ==============================================================
 * Copyright (C) 2015 by EffiSoft (http://www.effisoft.pl)
 ****************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless  required by applicable  law or agreed  to  in  writing, 
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the  License for the  specific language
 * governing permissions and limitations under the License.
 *
 ****************************************************************/
package org.elasticwarehouse.core;

import java.io.IOException;
//import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
//import java.net.URL;




import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.io.InputStream;

//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;

public class ElasticWarehouseRequestHandlerGrafana extends AbstractHandler
{
	private boolean isEmbeddedMode_ = true;
	private String graphiteHostPort_ = "";
	private int graphitePort_ = 10500;
	private String esHostPort_ = "";
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseRequestHandlerGrafana.class.getName()); 
	
	public ElasticWarehouseRequestHandlerGrafana(boolean isEmbeddedMode, String esHostPort, int graphitePort) {
		isEmbeddedMode_ = isEmbeddedMode;
		graphiteHostPort_ = "";//graphiteHostPort;
		graphitePort_ = graphitePort;
		esHostPort_ = esHostPort;
	}
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        //response.setContentType("text/html;charset=utf-8");
        //response.setStatus(HttpServletResponse.SC_OK);
        //baseRequest.setHandled(true);
        //response.getWriter().println("<h1>Hello World</h1>");
    	//request.getPathInfo()
    	LOGGER.debug("*** Grafana requesting " + request.getPathInfo());//.toString());
    	//if( isEmbeddedMode_ )
    	//{
	        String uri = request.getPathInfo();
	        String prefix = "";//app/grafana";
	        String fileToRead = uri.substring(prefix.length()); 
	        if( fileToRead.equals("/") )
	        	fileToRead = "/index.html";
	        
	        String resourceFile = "/res/"+ResourcesConfig.PATH_GRAFANA+fileToRead;
	        //System.out.println(resourceFile);
	        java.net.URL resourceId = this.getClass().getResource(resourceFile);
	        if( resourceId == null )
	        {
	        	LOGGER.warn(uri + " ; " + resourceFile + "fileResource is null");
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		        baseRequest.setHandled(true);
	        }
	        else
	        {
	        	LOGGER.debug(uri + " ; " + resourceFile + " reading... ");
	        
		        //java.nio.file.Path resPath;
		        byte fileContent[] = null;
				try {
					//resPath = java.nio.file.Paths.get(resourceId.toURI());
					//fileContent = java.nio.file.Files.readAllBytes(resPath);//, "UTF8"); 
					InputStream in = ResourceTools.class.getClass().getResourceAsStream(resourceFile);
					fileContent = IOUtils.toByteArray(in);
				} catch (IOException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} 
				
		        
				if( fileContent == null )
					LOGGER.debug("content is null");
				else
					LOGGER.debug("Read " + fileContent.length + " bytes");
		        
				if( resourceFile.endsWith(".css"))
					response.setContentType("text/css");
		        else if(resourceFile.endsWith(".js"))
		        	response.setContentType("text/javascript");
		        else if(resourceFile.endsWith(".png"))
		        {
		        	response.setContentType("image/png");
		        }
		        else
		        	response.setContentType("text/html; charset=UTF-8");
				
				
				if( resourceFile.endsWith("/config.js") )
				{
					String configcontent = new String(fileContent);
					configcontent = configcontent.replace("<<GRAPHITEHOSTWITHPORT>>", graphiteHostPort_);
					configcontent = configcontent.replace("<<GRAPHITEPORT>>", new Integer(graphitePort_).toString() );
					configcontent = configcontent.replace("<<ESHOSTWITHPORT>>", esHostPort_);
					
					
					fileContent = configcontent.getBytes(); 
				}
				//Content-Type: image/png
				//response.setContentType("text/html;charset=utf-8");
		        response.setStatus(HttpServletResponse.SC_OK);
		        baseRequest.setHandled(true);
		        //response.getw.getWriter().w .println(fileContent);
		        OutputStream os= response.getOutputStream();
		        os.write(fileContent);
		        //os.flush();
		        os.close();
	        
	        }
    	/*}else{
    		PrintWriter out = response.getWriter();
    		out.println("Grafana is available only in embedded mode. See 'mode.embedded' in elasticwarehouse.yml file. You may need to host your own Grafana instance if your configuration uses not embedded ES clutser.");
    		baseRequest.setHandled(true);
    	}*/
        //read(is); // .. read the request body
        //String mydata = convertStreamToString(is);
        //System.out.println(mydata);
    	
    };
 
    /*public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new HelloWorld());
 
        server.start();
        server.join();
    }*/
}

/*public class ElasticWarehouseRequestHandler implements HttpHandler 
{
    public void handle(HttpExchange t) throws IOException 
    {
        InputStream is = t.getRequestBody();
        System.out.println("Got request: " + t.getRequestURI().toString());
        String uri = t.getRequestURI().getPath();
        String prefix = "";//app/grafana";
        String fileToRead = uri.substring(prefix.length()); 
        if( fileToRead.equals("/") )
        	fileToRead = "/index.html";
        
        String resourceFile = "/res/"+ResourcesConfig.PATH_GRAFANA+fileToRead;
        //System.out.println(resourceFile);
        java.net.URL resourceId = this.getClass().getResource(resourceFile);
        if( resourceId == null )
			System.out.println(resourceFile + "fileResource is null");
        else
        	System.out.println(resourceFile + " reading...");
        java.nio.file.Path resPath;
        String fileContent = null;
		try {
			resPath = java.nio.file.Paths.get(resourceId.toURI());
			fileContent = new String(java.nio.file.Files.readAllBytes(resPath), "UTF8"); 
		} catch (URISyntaxException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
        
		if( fileContent == null )
			System.out.println("content is null");
		else
			System.out.println("Read " + fileContent.length() + " bytes");
        
        //read(is); // .. read the request body
        String mydata = convertStreamToString(is);
        System.out.println(mydata);
        
        
        //final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        //final String date = format.format(new Date(System.currentTimeMillis()));
        //response.setContentType("text/plain");
        //response.setContentLength(date.length());
        //response.getWriter().write(date);
        
        
        //String response = fileContent;
        //String response = "This is the response:" + fileToRead;
        t.getResponseHeaders().add("Server", "ElasticWarehouse http server");
        t.getResponseHeaders().add("Last-Modified","Tue, 02 Dec 2014 22:24:11 GMT");
        //t.getResponseHeaders().add("ETag","1e186a-4cb-509432faed8c0");
        t.getResponseHeaders().add("Accept-Ranges","bytes");
        t.getResponseHeaders().add("Content-Length",new Integer(fileContent.length()).toString());
        t.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods","GET, OPTIONS, POST");
        t.getResponseHeaders().add("Access-Control-Allow-Headers","origin, authorization, accept");
        t.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        t.getResponseHeaders().add("Connection","close");
        if( resourceFile.endsWith(".css"))
        	t.getResponseHeaders().add("Content-Type","text/css");
        else if(resourceFile.endsWith(".js"))
        	t.getResponseHeaders().add("Content-Type","text/javascript");
        else
        	t.getResponseHeaders().add("Content-Type","text/html; charset=UTF-8");
        
        OutputStream os = t.getResponseBody();
        
        t.sendResponseHeaders(200, fileContent.length());
        //t.getResponseBody().w
        
        if(resourceFile.endsWith(".js") && fileContent.length() > 45000)
        {
        	//fileContent += "dsfdsfds;lfksdfl;dsfldsfsdfdsfds;lfsdfsdfdsfd;slfldsflkds;lfdslkfklds;flsdfklsdflkdsklfd;lksf;lkds;fsdf";//fileContent.substring(0, 755920);
        	fileContent = fileContent.substring(0, 755976);
        }
        byte[] byteees = fileContent.getBytes();
        //System.out.println("After conv." + byteees.length + "bytes");
        os.write(byteees);
        //os.flush();
        os.close();
    }
    
    String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}*/

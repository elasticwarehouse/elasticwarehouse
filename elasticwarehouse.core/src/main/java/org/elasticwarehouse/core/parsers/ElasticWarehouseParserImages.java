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
package org.elasticwarehouse.core.parsers;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ElasticWarehouseParserImages implements Parser {
    private Set<MediaType> types;
    
    //private RenderingContext renderingContext;
    //private NodeRef imgFolder = null;
    private String imgFolder = "/tmp/";
    private int count = 0;
    
    public ElasticWarehouseParserImages(/*RenderingContext renderingContext*/) {
       //this.renderingContext = renderingContext;
       
       // Our expected types
       types = new HashSet<MediaType>();
       types.add(MediaType.image("bmp"));
       types.add(MediaType.image("gif"));
       types.add(MediaType.image("jpg"));
       types.add(MediaType.image("jpeg"));
       types.add(MediaType.image("png"));
       types.add(MediaType.image("tiff"));
       
       // Are images going in the same place as the HTML?
       //if( renderingContext.getParamWithDefault(PARAM_IMAGES_SAME_FOLDER, false) )
       //{
          //RenditionLocation location = resolveRenditionLocation(
           //     renderingContext.getSourceNode(), renderingContext.getDefinition(), 
           //     renderingContext.getDestinationNode()
          //);
          //imgFolder = location.getParentRef();
          //if (logger.isDebugEnabled())
          //{
          //    logger.debug("Using imgFolder: " + imgFolder);
          //}
       //}
    }
    
    //@Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
       return types;
    }

    //@Override
    public void parse(InputStream stream, ContentHandler handler,
          Metadata metadata, ParseContext context) throws IOException,
          SAXException, TikaException {
       // Is it a supported image?
       String filename = metadata.get(Metadata.RESOURCE_NAME_KEY);
       String type = metadata.get(Metadata.CONTENT_TYPE);
       boolean accept = false;
       
       if(type != null) {
          for(MediaType mt : types) {
             if(mt.toString().equals(type)) {
                accept = true;
             }
          }
       }
       if(filename != null) {
          for(MediaType mt : types) {
             String ext = "." + mt.getSubtype();
             if(filename.endsWith(ext)) {
                accept = true;
             }
          }
       }
       
       if(!accept)
          return;

       handleImage(stream, filename, type);
    }

    //@Override
    private void handleImage(InputStream stream, String filename, String type) {
       count++;
       
       // Do we already have the folder? If not, create it
       //if(imgFolder == null) {
       //   imgFolder = createImagesDirectory(renderingContext);
       //}
       
       // Give it a sensible name if needed
       if(filename == null) {
          filename = "image-" + count + ".";
          filename += type.substring(type.indexOf('/')+1);
       }
       
       // Prefix the filename if needed
       //filename = getImagesPrefixName(renderingContext) + filename; 

       // Save the image
       //createEmbeddedImage(imgFolder, (count==1), filename, type, stream, renderingContext);
    }
  }
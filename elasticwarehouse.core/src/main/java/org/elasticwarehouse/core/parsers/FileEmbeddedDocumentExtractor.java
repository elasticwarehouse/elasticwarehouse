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



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.FilenameUtils;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.elasticwarehouse.core.EWLogger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class FileEmbeddedDocumentExtractor
implements EmbeddedDocumentExtractor {

private int count = 0;
private File extractDir = new File("/tmp");
private final TikaConfig config = TikaConfig.getDefaultConfig();

public boolean shouldParseEmbedded(Metadata metadata) {
return true;
}

public void parseEmbedded(InputStream inputStream, ContentHandler contentHandler, Metadata metadata, boolean outputHtml) throws SAXException, IOException {
String name = metadata.get(Metadata.RESOURCE_NAME_KEY);

if (name == null) {
    name = "file" + count++;
}
DefaultDetector detector = new DefaultDetector();
MediaType contentType = detector.detect(inputStream, metadata);

if (name.indexOf('.')==-1 && contentType!=null) {
    try {
        name += config.getMimeRepository().forName(
                contentType.toString()).getExtension();
    } catch (MimeTypeException e) {
    	EWLogger.logerror(e);
        e.printStackTrace();
    }
}

String relID = metadata.get(Metadata.EMBEDDED_RELATIONSHIP_ID);
if (relID != null && !name.startsWith(relID)) {
    name = relID + "_" + name;
}

File outputFile = new File(extractDir, FilenameUtils.normalize(name));
File parent = outputFile.getParentFile();
if (!parent.exists()) {
    if (!parent.mkdirs()) {
        throw new IOException("unable to create directory \"" + parent + "\"");
    }
}
System.out.println("Extracting '"+name+"' ("+contentType+") to " + outputFile);

FileOutputStream os = null;

try {
    os = new FileOutputStream(outputFile);

    if (inputStream instanceof TikaInputStream) {
        TikaInputStream tin = (TikaInputStream) inputStream;

        if (tin.getOpenContainer() != null && tin.getOpenContainer() instanceof DirectoryEntry) {
            POIFSFileSystem fs = new POIFSFileSystem();
            copy((DirectoryEntry) tin.getOpenContainer(), fs.getRoot());
            fs.writeFilesystem(os);
        } else {
            IOUtils.copy(inputStream, os);
        }
    } else {
        IOUtils.copy(inputStream, os);
    }
} catch (Exception e) {
    //
    // being a CLI program messages should go to the stderr too
    //
    String msg = String.format(
        Locale.ROOT,
        "Ignoring unexpected exception trying to save embedded file %s (%s)",
        name,
        e.getMessage()
    );
    EWLogger.logerror(e);
    System.err.println(msg);
    //logger.warn(msg, e);
} finally {
    if (os != null) {
        os.close();
    }
}
}

protected void copy(DirectoryEntry sourceDir, DirectoryEntry destDir)
    throws IOException {
for (org.apache.poi.poifs.filesystem.Entry entry : sourceDir) {
    if (entry instanceof DirectoryEntry) {
        // Need to recurse
        DirectoryEntry newDir = destDir.createDirectory(entry.getName());
        copy((DirectoryEntry) entry, newDir);
    } else {
        // Copy entry
        InputStream contents = new DocumentInputStream((DocumentEntry) entry);
        try {
            destDir.createDocument(entry.getName(), contents);
        } finally {
            contents.close();
        }
    }
}
}
}

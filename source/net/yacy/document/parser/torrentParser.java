/**
 *  torrentParser.java
 *  Copyright 2010 by Michael Peter Christen, mc@yacy.net, Frankfurt am Main, Germany
 *  First released 03.01.2010 at http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.document.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import net.yacy.cora.document.MultiProtocolURI;
import net.yacy.document.AbstractParser;
import net.yacy.document.Condenser;
import net.yacy.document.Document;
import net.yacy.document.Parser;
import net.yacy.kelondro.data.word.Word;
import net.yacy.kelondro.util.BDecoder;
import net.yacy.kelondro.util.FileUtils;
import net.yacy.kelondro.util.BDecoder.BObject;
import net.yacy.kelondro.util.BDecoder.BType;

// a BT parser according to http://wiki.theory.org/BitTorrentSpecification
public class torrentParser extends AbstractParser implements Parser {

    public torrentParser() {
        super("Torrent Metadata Parser");
        SUPPORTED_EXTENSIONS.add("torrent");
        SUPPORTED_MIME_TYPES.add("application/x-bittorrent");
    }
    
    public Document[] parse(MultiProtocolURI location, String mimeType, String charset, InputStream source) throws Parser.Failure, InterruptedException {
        byte[] b = null;
        try {
            b = FileUtils.read(source);
        } catch (IOException e1) {
            throw new Parser.Failure(e1.toString(), location);
        }
        BDecoder bd = new BDecoder(b);
        BObject bo = bd.parse();
        if (bo == null) throw new Parser.Failure("BDecoder.parse returned null", location);
        if (bo.getType() != BType.dictionary) throw new Parser.Failure("BDecoder object is not a dictionary", location);
        Map<String, BObject> map = bo.getMap();
        BObject commento = map.get("comment");
        String comment = (commento == null) ? "" : new String(commento.getString());
        //Date creation = new Date(map.get("creation date").getInteger());
        BObject infoo = map.get("info");
        StringBuilder filenames = new StringBuilder();
        String name = "";
        if (infoo != null) {
            Map<String, BObject> info = infoo.getMap();
            BObject fileso = info.get("files");
            if (fileso != null) {
                List<BObject> filelist = fileso.getList();
                for (BObject fo: filelist) {
                    BObject patho = fo.getMap().get("path");
                    if (patho != null) {
                        List<BObject> l = patho.getList(); // one file may have several names
                        for (BObject fl: l) filenames.append(fl.toString()).append(" ");
                    }
                }
            }
            BObject nameo = info.get("name");
            if (nameo != null) name = new String(nameo.getString());
        }
        try {
            return new Document[]{new Document(
                    location,
                    mimeType,
                    charset,
                    null,
                    null,
                    name, // title
                    comment, // author 
                    location.getHost(),
                    null,
                    null,
                    filenames.toString().getBytes(charset),
                    null,
                    null,
                    null,
                    false)};
        } catch (UnsupportedEncodingException e) {
            throw new Parser.Failure("error in torrentParser, getBytes: " + e.getMessage(), location);
        }
    }
    
    public static void main(String[] args) {
        try {
            byte[] b = FileUtils.read(new File(args[0]));
            torrentParser parser = new torrentParser();
            Document[] d = parser.parse(new MultiProtocolURI("http://localhost/test.torrent"), null, "utf-8", new ByteArrayInputStream(b));
            Condenser c = new Condenser(d[0], true, true);
            Map<String, Word> w = c.words();
            for (Map.Entry<String, Word> e: w.entrySet()) System.out.println("Word: " + e.getKey() + " - " + e.getValue().posInText);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Parser.Failure e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}

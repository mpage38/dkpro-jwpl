/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.tudarmstadt.ukp.wikipedia.api;

import de.tudarmstadt.ukp.wikipedia.api.exception.WikiTitleParsingException;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An iterator over category objects.
 *
 */
public class TitleIterator implements Iterator<Title> {

//    private final static Logger logger = Logger.getLogger(TitleIterator.class);

    private TitleBuffer buffer;

    public TitleIterator(Wikipedia wiki, int bufferSize) {
        buffer = new TitleBuffer(bufferSize, wiki);
    }

    public boolean hasNext(){
        return buffer.hasNext();
    }

    public Title next(){
        return buffer.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Buffers titles in a list.
     *
     *
     */
    class TitleBuffer {

        private Wikipedia wiki;

        private List<String> titleStringBuffer;
        private int maxBufferSize;  // the number of pages to be buffered after a query to the database.
        private int bufferFillSize; // even a 500 slot buffer can be filled with only 5 elements
        private int bufferOffset;   // the offset in the buffer
        private int dataOffset;     // the overall offset in the data

        public TitleBuffer(int bufferSize, Wikipedia wiki){
            this.maxBufferSize = bufferSize;
            this.wiki = wiki;
            this.titleStringBuffer = new ArrayList<String>();
            this.bufferFillSize = 0;
            this.bufferOffset = 0;
            this.dataOffset = 0;
        }

        /**
         * If there are elements in the buffer left, then return true.
         * If the end of the filled buffer is reached, then try to load new buffer.
         * @return True, if there are pages left. False otherwise.
         */
        public boolean hasNext(){
            if (bufferOffset < bufferFillSize) {
                return true;
            }
            else {
                return this.fillBuffer();
            }
        }

        /**
         *
         * @return The next Title or null if no more categories are available.
         */
        public Title next(){
            // if there are still elements in the buffer, just retrieve the next one
            if (bufferOffset < bufferFillSize) {
                return this.getBufferElement();
            }
            // if there are no more elements => try to fill a new buffer
            else if (this.fillBuffer()) {
                return this.getBufferElement();
            }
            else {
                // if it cannot be filled => return null
                return null;
            }
        }

        private Title getBufferElement() {
            String titleString = titleStringBuffer.get(bufferOffset);
            Title title = null;
            try {
                title = new Title(titleString);
            } catch (WikiTitleParsingException e) {
                e.printStackTrace();
            }
            bufferOffset++;
            dataOffset++;
            return title;
        }

        private boolean fillBuffer() {

            Session session = this.wiki.__getHibernateSession();
            session.beginTransaction();
            List returnList = session.createNativeQuery(
            "select p.name from PageMapLine as p")
                .setFirstResult(dataOffset)
                .setMaxResults(maxBufferSize)
                .setFetchSize(maxBufferSize)
                .list();
            session.getTransaction().commit();

            // clear the old buffer and all variables regarding the state of the buffer
            titleStringBuffer.clear();
            bufferOffset = 0;
            bufferFillSize = 0;

            titleStringBuffer.addAll(returnList);

            if (titleStringBuffer.size() > 0) {
                bufferFillSize = titleStringBuffer.size();
                return true;
            }
            else {
                return false;
            }
        }

    }
}

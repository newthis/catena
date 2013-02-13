package syndeticlogic.catena.store;

import static org.junit.Assert.*;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import syndeticlogic.catena.type.Type;
import syndeticlogic.catena.utility.Codec;
import syndeticlogic.catena.utility.VariableLengthArrayGenerator;

public class SegmentWriterTest {
    PageFactory pageFactory;
    PageManager pageManager;
    FileChannel fileChannel;
    SerializedObjectChannel channel;
    SegmentHeader header;
    String fileName = "target"+System.getProperty("file.separator")+"SegmentWriterTest";
    int pages = 101;
    int pageSize = 4096;
    int retryLimit = 2;
    VariableLengthArrayGenerator generator;
    HashMap<Integer, Long> pageOffsets;
    List<byte[]> pageValues;
    
    public List<byte[]> readPages(List<Page> pageVector) {
        List<byte[]> ret = new ArrayList<byte[]>(pageVector.size());
        for(int i = 0; i < pageVector.size(); i++) {
            int pageSize = pageVector.get(i).limit();
            byte[] newValue = new byte[pageSize];
            pageVector.get(i).read(newValue, 0, 0, pageSize);
            ret.add(newValue);
        }
        return ret;
    }
    
    @Before
    public void setUp() throws Exception {
        fileChannel = new RandomAccessFile(fileName, "rw").getChannel();
        channel = new SerializedObjectChannel(fileChannel);
        pageFactory = new PageFactory(PageFactory.BufferPoolMemoryType.Java,
                PageFactory.CachingPolicy.PinnableLru,
                PageFactory.PageDescriptorType.Unsynchronized, retryLimit);
        pageManager = pageFactory.createPageManager(null, pageSize, 3 * pages);
        pageManager.createPageSequence(fileName);
        
        Codec.configureCodec(null);
        
        
        SegmentManager.configureSegmentManager(SegmentManager.CompressionType.Null,
                pageFactory.createPageManager(null, pageSize, pages));

        header = new SegmentHeader(fileChannel, pageManager, bufferPool);
        header.type(Type.BINARY);
        header.pages(50);
        //header.store();
        
        pageOffsets = new HashMap<Integer, Long>(header.pages());
        long offset = header.headerSize();
        
        generator = new VariableLengthArrayGenerator(37, 13);
        generator.setMaxSize(pageSize);
        
        
        /*List<byte[]>*/ pageValues = generator.generateMemoryArray(header.pages());        
        List<Page> pageVector = pageManager.getPageSequence(fileName);
        
        for(int i = 0; i < pageValues.size(); i++) {
            Page page = pageManager.page(fileName); 
            page.write(pageValues.get(i), 0, 0, pageValues.get(i).length);
            page.setLimit(pageValues.get(i).length);
            pageVector.add(page);
            pageOffsets.put(i, offset);
            offset = offset + page.limit();
        }
        System.out.println(" size = " +pageManager.getPageSequence(fileName).size()+" page vector .size = "+pageVector.size());
        List<byte[]> readBackPageValues = readPages(pageVector);
        
        compareArrays(pageValues, readBackPageValues);
        assertEquals(pageOffsets.size(), pageValues.size());

        
    }
    
    public void compareArrays(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSegmentWriter() throws Exception {
        SegmentWriter writer = new SegmentWriter(channel, pageManager, pageOffsets, fileName);
        writer.write();
        
        channel.position(0L);
        header = new SegmentHeader(fileChannel, pageManager, bufferPool);
        header.load();
        assertEquals(pageValues.size(), header.pages());
        assertEquals(Type.BINARY, header.type());
        fileChannel.position(header.headerSize());
        ByteBuffer length = ByteBuffer.allocate(Type.INTEGER.length());
        for(int i = 0; i < pageValues.size() ; i++) {
            
            int bytesRead = fileChannel.read(length);
            length.rewind();
            assertEquals(Type.INTEGER.length(), bytesRead);
            int size = Codec.getCodec().decodeInteger(length);
            length.rewind();
            ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.limit(size);
            bytesRead = fileChannel.read(buffer);
            buffer.rewind();
            assertArrayEquals(pageValues.get(i), buffer.array());
        }
    }
}

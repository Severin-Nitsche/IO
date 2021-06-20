package com.github.severinnitsche.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
* Threadsave manager for I/O operations
*/
public class SyncedIOStream {
  private volatile AtomicBuffer buffer;
  private volatile InputStream input;
  private volatile OutputStream output;
  private volatile ConcurrentLinkedQueue<IStream> iqueue;
  private volatile ConcurrentLinkedQueue<OStream> oqueue;
  private volatile ArrayList<Ghost> ghosts;
  private volatile long offset;
  private final ReentrantLock read;

  /**
  * Creates a new SyncedIOStream on the basis of existing sources
  * @param input The input source
  * @param output The output source
  */
  public SyncedIOStream(InputStream input, OutputStream output) {
    this.buffer = AtomicBuffer.allocateDirect(131072,() -> ghosts.stream().mapToLong(g -> g.offset).min().orElse(offset)); //1-Mebibyte
    this.iqueue = new ConcurrentLinkedQueue<>();
    this.oqueue = new ConcurrentLinkedQueue<>();
    this.ghosts = new ArrayList<>();
    this.output = output;
    this.input = input;
    this.offset = 0;
    this.read = new ReentrantLock();
  }

  /**
  * Non blocking method to queue a new {@link com.github.severinnitsche.io.IOStream}
  * @return a new IOStream
  */
  public IOStream entry() {
    var io = new IOStream(this);
    iqueue.offer(io);
    oqueue.offer(io);
    return io;
  }

  /**
  * Flush the underlying stream
  * @throws IOException when the underlying stream throws an exception
  */
  public void flush() throws IOException {
    output.flush();
  }

  int ghost(long real) throws IOException {
    if(buffer.size() > real) //The content has been buffered
      return buffer.readByte(real); //Use the blocking method to avoid receiving -1, when the position was updated by another Thread but is not yet marked as readable
    read.lock(); //Request exclusive read-access to guarantee order
    try {
      if(buffer.size() > real) //The content has been buffered in the mean time
        return buffer.readByte(real); //Use the blocking method to avoid receiving -1, when the position was updated by another Thread but is not yet marked as readable
      int i = 0;
      while(buffer.size() <= real) buffer.write((byte)(i = input.read()));
      return i;
    } finally {
      read.unlock(); //Unlock the read
    }
    //return -1; //Fallback
  }

  int read(IOStream stream, long position) throws IOException {
    while(iqueue.peek() != stream || oqueue.peek() != stream); //Block
    long real = position + offset; //The address in the buffer
    return ghost(real);
  }

  void write(IOStream stream, byte b) throws IOException {
    while(iqueue.peek() != stream || oqueue.peek() != stream); // Block
    output.write(b);
  }

  synchronized void release(IOStream stream, long position) {
    while(iqueue.peek() != stream || oqueue.peek() != stream); // Block
    offset = position; //Update bounds
    //queue.poll(); //Remove stream
    iqueue.poll();
    oqueue.poll();
    ghosts.stream().forEach(g -> {
      if(g.stream == iqueue.peek() && g.stream == oqueue.peek()) {
        g.offset = offset;
        g.active.set(true);
      }
    });
  }

  Ghost ghost(IOStream stream) {
    var ghost = new Ghost(stream,iqueue.peek() == stream && oqueue.peek() == stream,offset,this);
    ghosts.add(ghost);
    return ghost;
  }

  /**
  * Writes a String regardless of other activities
  * @param s The string
  * @throws IOException when the underlying stream throws an exception
  */
  public void interrupt(String s) throws IOException {
    synchronized(output) {
      for(byte b : s.getBytes())
        output.write(b);
      output.flush();
    }
  }

  synchronized void release(Ghost ghost) {
    ghosts.remove(ghost);
  }
}

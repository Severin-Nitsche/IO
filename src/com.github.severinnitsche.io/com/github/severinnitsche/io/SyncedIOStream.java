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
  * 🚫 Non ✋ blocking method to queue a ✨ new ⛲️ {@link com.github.severinnitsche.io.OSpring}
  * @return 👉 a ✨ new ⛲️ OSpring
  */
  public OSpring oentry() {
    var o = new OSpring(this);
    oqueue.offer(o);
    return o;
  }

  /**
  * 🚫 Non ✋ blocking method to queue a ✨ new 🌊 {@link com.github.severinnitsche.io.IFlow}
  * @return 👉 a ✨ new 🌊 IFlow
  */
  public IFlow ientry() {
    var i = new IFlow(this);
    iqueue.offer(i);
    return i;
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

  private boolean turn(java.io.Closeable entity) {
    if(entity instanceof IOStream)
      return iqueue.peek() == entity && oqueue.peek() == entity;
    else if(entity instanceof IFlow)
      return iqueue.peek() == entity;
    else if(entity instanceof OSpring)
      return oqueue.peek() == entity;
    return false;
  }

  int read(IStream stream, long position) throws IOException {
    while(!turn(stream)); //Block
    long real = position + offset; //The address in the buffer
    return ghost(real);
  }

  void write(OStream stream, byte b) throws IOException {
    while(!turn(stream)); // Block
    output.write(b);
  }

  synchronized void release(IStream stream, long position) {
    while(iqueue.peek() != stream); // Block
    offset += position; //Update bounds
    //queue.poll(); //Remove stream
    iqueue.poll();
    ghosts.stream().forEach(g -> {
      if(g.stream == iqueue.peek()) {
        g.offset = offset;
        g.active.set(true);
      }
    });
  }

  synchronized void release(OStream stream) {
    while(oqueue.peek() != stream); //Block
    oqueue.poll();
  }

  Ghost ghost(IStream stream) {
    var ghost = new Ghost(stream,turn(stream),offset,this);
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

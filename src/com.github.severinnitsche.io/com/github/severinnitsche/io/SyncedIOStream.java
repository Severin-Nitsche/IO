package com.github.severinnitsche.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class SyncedIOStream {
  private volatile AtomicBuffer buffer;
  private volatile InputStream input;
  private volatile OutputStream output;
  private volatile ConcurrentLinkedQueue<IOStream> queue;
  private volatile ArrayList<Ghost> ghosts;
  private volatile long offset;
  private final ReentrantLock read;

  public SyncedIOStream(InputStream input, OutputStream output) {
    this.buffer = AtomicBuffer.allocateDirect(131072,() -> ghosts.stream().mapToLong(g -> g.offset).min().orElse(offset)); //1-Mebibyte
    this.queue = new ConcurrentLinkedQueue<>();
    this.ghosts = new ArrayList<>();
    this.output = output;
    this.input = input;
    this.offset = 0;
    this.read = new ReentrantLock();
  }

  public IOStream entry() {
    var io = new IOStream(this);
    queue.offer(io);
    return io;
  }

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
    while(queue.peek() != stream); //Block
    long real = position + offset; //The address in the buffer
    return ghost(real);
  }

  void write(IOStream stream, byte b) throws IOException {
    while(queue.peek() != stream); //block until stream is at front of the queue
    output.write(b);
  }

  synchronized void release(IOStream stream, long position) {
    while(queue.peek() != stream); //Block
    offset = buffer.size(); //Update bounds
    queue.poll(); //Remove stream
    //TODO: Add ghost Activation
    ghosts.stream().forEach(g -> {
      if(g.stream == queue.peek()) {
        g.offset = offset;
        g.active.set(true);
      }
    });
  }

  Ghost ghost(IOStream stream) {
    var ghost = new Ghost(stream,queue.peek() == stream,offset,this);
    ghosts.add(ghost);
    return ghost;
  }

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
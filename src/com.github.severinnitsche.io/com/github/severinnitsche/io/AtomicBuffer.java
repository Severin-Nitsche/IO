package com.github.severinnitsche.io;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
* <p>Atomic Wrapper for a dynamically sized Buffer.</p>
* <h2>Memory Layout</h2>
* <p>The buffer is layed out in groups of 9 Bytes each, whereas the first byte of each group contains flags, on whether the following 8 Bytes are readable / written.</p>
* <table>
*	<thead>
*    <tr><th>MemoryAddress</th><th>Flags</th><th></th><th></th><th></th><th></th><th></th><th></th><th></th><th></th><th>Text</th></tr>
*	</thead>
*	<tbody>
*	<tr>
*		<td>0x00000000</td><td>0x11111111</td><td>0x48</td><td>0x65</td><td>0x6c</td><td>0x6c</td><td>0x6f</td><td>0x20</td><td>0x57</td><td>0x6f</td><td>Hello.Wo</td>
*	</tr>
*	<tr>
*		<td>0x00000008</td><td>0x11100000</td><td>0x72</td><td>0x6c</td><td>0x64</td><td>0x00</td><td>0x00</td><td>0x00</td><td>0x00</td><td>0x00</td><td>rld........</td>
*	</tr>
*	</tbody>
* </table>
*/
public class AtomicBuffer {
  private AtomicReference<ByteBuffer> buffer;
  private AtomicInteger capacity;
  private AtomicLong position;
  private AtomicLong offset;
  private final ReentrantReadWriteLock lock;
  private final ReentrantLock flagLock;
  private final boolean direct;
  private final LongSupplier cleaner;

  private static ByteBuffer _allocate(final int capacity, final boolean direct) {
    if(direct)
      return ByteBuffer.allocateDirect(capacity*9);
    else
      return ByteBuffer.allocate(capacity*9);
  }

  /**
  * @param capacity The initial capacity in 8-Byte Words
  * @param direct Flag, whether thea unerlying Buffer is direct
  */
  private AtomicBuffer(final int capacity, final boolean direct, final LongSupplier cleaner) {
    buffer = new AtomicReference<>(_allocate(capacity,direct));
    position = new AtomicLong(0);
    offset = new AtomicLong(0);
    lock = new ReentrantReadWriteLock();
    flagLock = new ReentrantLock();
    this.capacity = new AtomicInteger(capacity);
    this.direct = direct;
    this.cleaner = cleaner;
  }

  public static AtomicBuffer allocate(final int capacity) {
    return new AtomicBuffer(capacity,false,() -> 0);
  }

  public static AtomicBuffer allocateDirect(final int capacity) {
    return new AtomicBuffer(capacity,true,() -> 0);
  }

  public static AtomicBuffer allocate(final int capacity, final LongSupplier cleaner) {
    return new AtomicBuffer(capacity,false,cleaner);
  }

  public static AtomicBuffer allocateDirect(final int capacity, final LongSupplier cleaner) {
    return new AtomicBuffer(capacity,true,cleaner);
  }

  public long size() {
    return position.get();
  }

  private synchronized void enlarge(final long target) {
    discardBefore(cleaner.getAsLong());
    System.out.print("❗️");
    //Lock
    lock.writeLock().lock();
    try {
      //Check if the buffer is large enough
      ByteBuffer nextBuffer = buffer.get().duplicate();
      while(nextBuffer.capacity() <= target - offset.get()) {
        nextBuffer = _allocate(nextBuffer.capacity() / 9 + capacity.get(), direct);
      }
      var pos = buffer.get().position();
      buffer.get().position(0);
      nextBuffer.put(buffer.get());
      nextBuffer.position(pos);
      buffer.set(nextBuffer);
    } finally {
      System.out.print("✅");
      //Unlock
      lock.writeLock().unlock();
    }
  }

  public void write(final byte data) {
    //Lock
    lock.readLock().lock();
    try {
      //Save this sequential-write position
      final long pos = position.getAndIncrement();
      //Get the current flags & real position
      final long flags = pos / 8 * 9 - offset.get();
      final long real = pos + pos/8 + 1 - offset.get();
      //Check if the buffer is large enough
      if(buffer.get().capacity() <= real) {
        //Unlock (prevent Deadlock if multiple Threads need to enlarge)
        lock.readLock().unlock();
        try {
          //Enlarge the buffer to match the target size
          enlarge(real);
        } finally {
          //Relock
          lock.readLock().lock();
        }
      }
      //Write to the position
      buffer.get().put((int)real,data);
      //Lock flags
      flagLock.lock(); //Technically a waste because only this flag-pos needs to be locked
      try {
        //Update Flags
        byte flag = buffer.get().get((int)flags);
        flag |= 1 << (pos % 8);
        buffer.get().put((int)flags,flag);
      } finally {
        //Unlock Flags
        flagLock.unlock();
      }
    } finally {
      //Unlock
      lock.readLock().unlock();
    }
  }

  public int read(long pos) {
    final long flags = pos / 8 * 9 - offset.get();
    final long real = pos + pos/8 + 1 - offset.get();
    if(buffer.get().capacity() <= real)
      return -1;
    if((buffer.get().get((int)flags) & (1 << (pos % 8))) != 0)
      return buffer.get().get((int)real);
    return -1;
  }

  public byte readByte(long pos) {
    final long flags = pos / 8 * 9 - offset.get();
    final long real = pos + pos/8 + 1 - offset.get();
    while(buffer.get().capacity() <= real);
    while((buffer.get().get((int)flags) & (1 << (pos % 8))) == 0);
    return buffer.get().get((int)real);
  }

  public int get(long pos) {
    final long real = pos + pos/8 + 1 - offset.get();
    if(buffer.get().capacity() <= real)
      return -1;
    return buffer.get().get((int)real);
  }

  public byte getByte(long pos) {
    final long real = pos + pos/8 + 1 - offset.get();
    while(buffer.get().capacity() <= real);
    return buffer.get().get((int)real);
  }

  public void discardBefore(long pos) {
    //lock
    lock.writeLock().lock();
    try {
      pos = pos / 8 * 9;
      if(pos <= buffer.get().capacity()) {
        var newSize = ((buffer.get().capacity() - pos) / capacity.get() + 1) * capacity.get();
        ByteBuffer newBuffer;
        if(newSize < buffer.get().capacity()) {
          newBuffer = _allocate((int)newSize,direct);
        } else {
          newBuffer = buffer.get().duplicate();
          newBuffer.position(0);
        }
        buffer.get().position((int)pos);
        newBuffer.put(buffer.get());
        buffer.set(newBuffer);
      } else {
        var newBuffer = _allocate(capacity.get(),direct);
        buffer.set(newBuffer);
      }
      offset.addAndGet(pos);
    } finally {
      //unlock
      lock.writeLock().unlock();
    }
  }

  @Override
  public String toString() {
    lock.readLock().lock();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append(buffer.get());
      sb.append(String.format("%n%11s %10s %23s %8s","Address","Flags","Data","Text"));
      byte[] buf = new byte[8];
      byte flags = 0;
      for(long i = offset.get(); i < Math.ceil(position.get()*1.125); i++) {
        byte current = buffer.get().get((int)(i - offset.get()));
        if(i%9 == 0) {
          flags = current;
          sb.append(String.format("%n0x%08x:",i/9*8));
          sb.append(" 0b");
          for(int j = 0; j < 8; j++) {
            sb.append((current >>> j) & 1);
          }
        } else {
          sb.append(String.format(" %02x",current).toUpperCase());
          buf[(int)(i%9-1)] = current;
          if((flags & (1 << (i%9-1))) == 0) {
            buf[(int)(i%9-1)] = '.';
          }
        }
        if(i%9 == 8) {
          try {
            sb.append(String.format(" %s",new String(buf,"utf-8").replaceAll("[^\\p{Graph}]",".")));
          } catch(UnsupportedEncodingException e) {}
        }
      }
      sb.append(String.format("%n"));
      return sb.toString();
    } finally {
      lock.readLock().unlock();
    }
  }

}

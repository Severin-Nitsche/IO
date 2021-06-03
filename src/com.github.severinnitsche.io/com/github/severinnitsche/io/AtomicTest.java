package com.github.severinnitsche.io;

public class AtomicTest {

  public static void main(String[] args) {
    AtomicBuffer buffer = AtomicBuffer.allocate(131072);
    buffer.discardBefore(0);
    for(int i = 0; i < 100; i++) {
      buffer.write((byte)(Math.random()*256));
    }
    System.out.println(buffer);
  }

}

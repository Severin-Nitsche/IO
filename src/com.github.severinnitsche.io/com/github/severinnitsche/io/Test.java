package com.github.severinnitsche.io;

import java.util.concurrent.Executors;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class Test {

  public static void main(String[] args) throws IOException {
    //--------Stream Content----------
    var input = "Lorem Ipsum \n Dolor Sit amet";
    //-------Streams------------------
    var out = new ByteArrayOutputStream();
    var in = new ByteArrayInputStream(input.getBytes());
    //-------Synced IO----------------
    var sio = new SyncedIOStream(in,out);
    //-------IO-----------------------
    var io_1 = sio.entry();
    var io_2 = sio.entry();
    var gh_1 = io_1.ghost();
    var gh_2 = io_2.ghost();
    //-------Output-------------------
    var pool = Executors.newFixedThreadPool(4);
    pool.execute(() -> {
      try {
        System.out.printf("Ghost: %s << %s%n",gh_1,gh_1.ghostLine());
        System.out.printf("Ghost: %s << %s%n",gh_1,gh_1.ghostLine());
        gh_1.release();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    pool.execute(() -> {
      try {
        System.out.printf("Ghost: %s << %s%n",gh_2,gh_2.ghostLine());
        gh_2.release();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    pool.execute(() -> {
      try {
        io_2.write("\nio_2:\n");
        io_2.write(io_2.readLine());
        io_2.release();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    pool.execute(() -> {
      try {
        io_1.write("\nio_1\n");
        io_1.write(io_1.readLine());
        io_1.release();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    pool.shutdown();
    while(!pool.isTerminated());
    System.out.println(out);
  }

}

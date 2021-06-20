package com.github.severinnitsche.io;

import java.util.concurrent.TimeUnit;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

//--------JUnit imports----------
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@DisplayName("IO")
@Execution(ExecutionMode.CONCURRENT)
public class IOTest {

  //--------Stream Content----------
  static String input;
  static String[] lines;
  //-------Streams------------------
  static ByteArrayOutputStream out;
  static ByteArrayInputStream in;
  //-------Synced IO----------------
  static SyncedIOStream sio;
  //-------IO-----------------------
  static IOStream io_1;
  static IOStream io_2;
  static IFlow in_1;
  static Ghost gh_1;
  static Ghost gh_2;

  @BeforeAll
  public static void setUp() throws Exception {
    //--------Stream Content----------
    input = "Lorem Ipsum \n Dolor Sit amet \n Neo de victum";
    lines = input.split("\n");
    //-------Streams------------------
    out = new ByteArrayOutputStream();
    in = new ByteArrayInputStream(input.getBytes());
    //-------Synced IO----------------
    sio = new SyncedIOStream(in,out);
    //-------IO-----------------------
    io_1 = sio.entry();
    io_2 = sio.entry();
    in_1 = sio.ientry();
    gh_1 = io_1.ghost();
    gh_2 = io_2.ghost();
  }

  @Test
  @DisplayName("😱 #1")
  public void ghost_1() throws Exception {
    assertEquals(gh_1.ghostLine(),lines[0]);
    assertEquals(gh_1.ghostLine(),lines[1]);
    gh_1.release();
  }

  @Test
  @DisplayName("😱 #2")
  public void ghost_2() throws Exception {
    assertEquals(gh_2.ghostLine(),lines[1]);
    gh_2.release();
  }

  @Test
  @DisplayName("I/O #1")
  public void io_1() throws Exception {
    io_1.write(io_1.readLine());
    io_1.release();
  }

  @Test
  @DisplayName("I/O #2")
  public void io_2() throws Exception {
    io_2.write(io_2.readLine());
    io_2.release();
    assertEquals(lines[0]+lines[1],out.toString());
  }

  @Test
  @DisplayName("in #1")
  public void in_1() throws Exception {
    assertEquals(in_1.readLine(),lines[2]);
    in_1.close();
  }

}

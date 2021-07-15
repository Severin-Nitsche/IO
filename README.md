![Logo](Logo.svg)

[![License](https://img.shields.io/github/license/Severin-Nitsche/IO?color=ff69b4)](https://github.com/Severin-Nitsche/IO/blob/main/LICENSE)
![GitHub repo size](https://img.shields.io/github/repo-size/Severin-Nitsche/IO?color=blue&label=size)
[![Build Status](https://img.shields.io/github/workflow/status/Severin-Nitsche/IO/Build?logo=github)](https://github.com/Severin-Nitsche/IO/actions)
[![Test Status](https://img.shields.io/github/workflow/status/Severin-Nitsche/IO/Test?label=Tests)](https://github.com/Severin-Nitsche/IO/actions)
# IO
IO is a ♨️ java library that enables secure multi-thread access to an I/O-source useful in the development of web, native and parallel computing communicating applications. This access is strongly cached and high-performance, yet also wrapped in a thread-save and user-friendly layer, appropriate for the java ecosystem.

IO is an enabling technology and provides high-level abstraction for handling thread-save I/O-accesses. It is not a framework and remains minimal in functionality so there is little overhead to consider.

IO is open source software and freely available at no charge

## ❤️‍🔥 Motivation

When wrapping I/O-sources in application layers one often finds it hard to do so in a thread-save manner ie. reading or writing from multiple Threads leads to a jumbled up mess. So one has to resort to other means or bother with java's thread monitoring system which for most casual developers is vastly out of scope in simple projects.

This project aims to lift the latter responsibility out of your 🤝 hands by providing a queue-based wrapper for I/O-sources while 🤞 hopefully also remaining at a 〽️ peak performance.

![Terminal Example](demo.gif)

## ⚡️ Installation

You can either download the latest release. Or build from 😎 source.
```
$ git clone https://github.com/Severin-Nitsche/IO.git
$ cd IO
$ mvn
```
Alternatively you may use `make build` if you don't fancy using [Maven](https://maven.apache.org).

## Quick Start
Let's 🧐 see how you may use IO to wrap simple I/O-operations.
### 🔓 Locking Mechanism
```java
public class Service {
  private SyncedIOStream sio;

  public Service() {
    ...
    sio = new SyncedIOStream(in,out); // initialize with your I/O-sources
  }

  public int serve() {
    var io = sio.entry(); // get exclusive access to your I/O
    ...// some logic
    var info = io.readLine();
    io.writeLine(process(info));
    ...//some more logic
    io.release(); //free the resource (locally)
    return importantResult;
  }
}
```
Of course one could achieve such simple example easily with the use of a `synchronized` block. However, regardless of code-style preference IO allows for more Operations such as parallel reading. So whenever you create a locally exclusive IO element you are able to fork a 😱 Ghost that outlives it's parents release and picks up reading at their entry-point.
### 🔥 Parallel Reading
```java
var io = sio.entry();
...
var ghost = io.ghost(); // Fork a 😱
...
io.release();
...
var info = ghost.ghostLine(); // Works even though io is released
```
### ⚠️ Dangerous Capabilities
There are times, when though generally in need for queued parallelization, one finds themselves in the need for dropping all cares and access the stream directly. Especially when a reading operation is blocking other threads from writing (and a 😱  ghost is unreasonable because the characters need to be consumed in order for other queued Operations to work properly).
```java
sio.interrupt("Some Token");
```
❗️ Note that this feature is ought to be ❌ removed, as separate queueing for I & O has been ➕ added.

### Separate Queueing
To allow for more fine grained control of one's I/O-sources, there has been ➕ added a 🎗 support for separate I/O-queueing.
```java
try (var i = sio.ientry()) {
  ...
  var info = i.readLine();
  ...
}
```
```java
try (var o = sio.oentry()) {
  ...
  o.write(SOME_BYTE);
  ...
}
```
As seen here, there also has been added the implementation of the [`Closeable`](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/io/Closeable.html) interface, which allows for a 😅 save usage - even with 💩 faulty code - and seeks to be a 👍 preferred alternative to the `release` method. Of course this is also true for combined I/O.

## Notice
While all entry methods are 🚫 ✋ non-blocking, read and write methods are. Consider this when using IO, because you are 😞 not save from creating 🔥 dead-locks.

## Social
Consider ✨ starring this repository if you enjoy it.

[![built-with-love](http://ForTheBadge.com/images/badges/built-with-love.svg)](https://GitHub.com/Severin-Nitsche/)

package main

import (
  "net"
  "fmt"
  "time"
)

const (
  workers = 10
  connections = 300
)

func main() {
  tasks := make(chan int)

  for i := 0; i < workers; i++ {
    go func(i int) {
      for {
        j, more := <-tasks
        if more {
          conn, err := net.Dial("tcp", "127.0.0.1:9998")
          if err != nil {
            fmt.Println("error while connecting:", err)
            return
          }
          conn.Write([]byte("<?xml version=\"1.0\"?>"))
          conn.Write([]byte("<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">"))
          fmt.Println("writing", j, "to proxy from worker", i)
          data := make([]byte, 512)
          _, err = conn.Read(data)
          if err != nil {
            fmt.Println("error while reading:", err)
            return
          }
          conn.Close()
        } else {
          return
        }
      }
    }(i)
  }

  for i := 0; i < connections; i++ {
    tasks <- i
  }
  time.Sleep(1 * time.Second)
  return
}

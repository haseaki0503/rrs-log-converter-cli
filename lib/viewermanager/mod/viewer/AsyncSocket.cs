using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;


using System.Net;
using System.Net.Sockets;

using Tasks;
using Viewer;

using MsgPack;
using MsgPack.Serialization;

namespace Viewer
{

    public class AsyncMsgSocket {
        
        private TcpClient client;
        private NetworkStream stream;
        
        private Task<byte[]> taskReceive;
        private Task<int> taskSend;

        private volatile bool alive;

        public AsyncMsgSocket() {
            client = new TcpClient();
            stream = null;
            taskReceive = null;
            taskSend = null;
            alive = false;

        }

        public AsyncMsgSocket(string host, int port) : this() {
            Connect(host, port);
        }

        public void Close() {
            if(client != null) {
                client.Close();
                client = null;
                stream = null;
            }
        }

        public void Connect(string host, int port) {
            client.Connect(host, port);
            stream = client.GetStream();
            alive = true;
        }

        private int getSizeRemain(byte[] data) 
        {
            if(data == null || data.Length <= 0) return 0;
            if((data[0] & 0x80) == 0) {
                return 0;
            }
            else if(data[0] == 0xcc) {
                return 1;
            }
            else if(data[0] == 0xcd) {
                return 2;
            }
            else if(data[0] == 0xce) {
                return 4;
            }
            else {
                return -1;
            }
        }

        private byte[] receiveAsync() {
            if(!alive) {
                return null;
            }

            byte[] buffer = new byte[1024];
            MemoryStream ms = new MemoryStream();
            int read = 0;
            uint res_size = 0;
            int require = 64;
            int total = 0;

            do {
                if(res_size == 0 || ms.Position < res_size) {
                    read = stream.Read(buffer, 0, require); // Blocking
                    if(read < 0) {
                        // Communication Ended.
                        alive = false;
                        break;
                    }
                    total += read;

                    if(res_size == 0) {
                        var unpack = Unpacking.UnpackUInt32(buffer);
                        res_size = unpack.Value;

                        ms.Write(buffer, unpack.ReadCount, read - unpack.ReadCount);
                    }
                    else {
                        ms.Write(buffer, 0, read);
                    }

                    require = (int)Math.Min(buffer.Length, res_size - ms.Position);
                }

            } while(read > 0 && require > 0 && total < res_size);

            byte[] result = ms.ToArray();
            ms.Close();
            return result;
        }

        private int sendAsync(byte[] data) {
            if(!alive || stream == null) {
                return 0;
            }

            stream.Write(data, 0, data.Length);

            return data.Length;
        }

        public Task<int> Send(byte[] data) {
            if(taskSend != null && !taskSend.Done) {
                // If task working, return it.
                return taskSend;
            }
            else if (stream == null) {
                // no valid connection working
                return null;
            }

            MemoryStream ms = new MemoryStream();
            Packer packer = Packer.Create(ms);
            ms.Position = 0;
            packer.Pack(data.Length);
            
            ms.Write(data, 0, data.Length);

            // create new task
            taskSend = Task.Run<byte[], int>(sendAsync, ms.ToArray());

            return taskSend;
        }

        public Task<byte[]> Receive() {
            if(taskReceive != null && !taskReceive.Done)
             {
                // If task working, return it.
                return taskReceive;
            }
            else if(stream == null) {
                // no connection working
                return null;
            }

            // create new task
            taskReceive = Task.Run<byte[]>(receiveAsync);

            return taskReceive;
        }

    };

    // Test
    // public class TestMain {
    //     public static void Main(string[] args) {
    //         Console.WriteLine("Hello");

    //         AsyncMsgSocket socket = new AsyncMsgSocket("localhost", 17777);
    //         byte[] msg = System.Text.Encoding.Unicode.GetBytes("HelloWorld-1234567890");
    //         var send = socket.Send(msg);
    //         Console.WriteLine("Send Msg");
    //         send.Wait();


    //         var recv = socket.Receive();
    //         recv.Wait();
    //         string recved = System.Text.Encoding.Unicode.GetString(recv.Result);
    //         Console.WriteLine("Recv Msg {0} bytes", recv.Result.Length);
    //         Console.WriteLine(recved);
    //     }
    // }

}


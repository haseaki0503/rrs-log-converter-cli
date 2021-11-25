using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;


using System.Net;
using System.Net.Sockets;

using Tasks;
using Viewer;

using System.Reflection;

using MsgPack;
using MsgPack.Serialization;

namespace Viewer
{
    public class TestMain {
        public static void Main(string[] args)
        {
            Console.WriteLine("START");
            SerializationContext context = new SerializationContext();
            context.SerializationMethod = SerializationMethod.Map;
            MessagePackSerializer<Result> unpacker = MessagePackSerializer.Get<Result>();

            MemoryStream ms = new MemoryStream();
            Packer packer = Packer.Create(ms);
            Request req = new Request();
            req.request = Request.OPEN;
            packer.Pack<Request>(req, context);
            byte[] data = ms.ToArray();

            AsyncMsgSocket socket = new AsyncMsgSocket("localhost", 17777);
            socket.Send(data).Wait();
            var taskReceive = socket.Receive();

            byte[] recv = null;
            World world = new EntityWorld();
            PropertyInfo[] propertyInfo = typeof(World).GetProperties();
            while(taskReceive != null) {
                
                taskReceive.Wait();
                if(taskReceive.IsSuccess){
                    recv = taskReceive.Result;
                    Console.WriteLine("> {0}", recv.Length);

                    ms = new MemoryStream();
                    ms.Write(recv, 0, recv.Length);
                    ms.Position = 0;
                    Result res = unpacker.Unpack(ms);
                    Console.WriteLine(res);

                    world.Update(res.record);
                    Console.WriteLine(world);
                    foreach (PropertyInfo info in propertyInfo)
                    {
                        Console.WriteLine("{0}: {1}", info.Name, info.GetValue(world));
                    }
                }
                else {
                    break;
                }


                taskReceive = socket.Receive();
            }

            Console.WriteLine("END");
        }
    }
}



using System;
using System.Collections.Generic;
using System.Text;
using System.IO;

using MsgPack;
using MsgPack.Serialization;

using System.Net;
using System.Net.Sockets;

using System.Diagnostics;

namespace MsgViewer
{
    public class OEntity
    {
        public string name { get; set; }
        public int value { get; set; }
    }


    public class Request
    {
        public int request { get; set; }
        public int? time { get; set; }

        public static int OPEN = (1 << 0);
        public static int CLOSE = (1 << 1);
        public static int ACTION = (1 << 2);
        public static int UPDATE = (1 << 3);
        public static int WORLD = (1 << 4);
        public static int PERCEPTION = (1 << 5);
        public static int CONFIG = (1 << 6);
        public static int MAP = (1 << 7);

        public Request()
        {
            request = OPEN;
            time = null;
        }
    }


    public class Entity
    {
        public int id { get; set; }
        public string type { get; set; }
        public bool? deleted { get; set; }

        public int? x { get; set; }
        public int? y { get; set; }

        public int? position { get; set; }

        public int? damage { get; set; }
        public int? buried { get; set; }
        public int? hp { get; set; }

        public Point[] history { get; set; }
        public int? travel { get; set; }

        public int? board { get; set; }
        public int? water { get; set; }

        public int[] blockades { get; set; }

        public int? temp { get; set; }
        public int? broken { get; set; }
        public int? fiery { get; set; }

        public int? repairCost { get; set; }
        public Point[] apexes { get; set; }
    }

    public class Point
    {
        public int x { get; set; }
        public int y { get; set; }
    }
    public class Edge
    {
        public Point start { get; set; }
        public Point end { get; set; }
    }

    public class AreaInfo
    {
        public int id { get; set; }
        public string type { get; set; }
        public int? x { get; set; }
        public int? y { get; set; }
        public Edge[] edges { get; set; }
        public int[] neighbours { get; set; }
    }

    public class Action
    {
        public int id { get; set; }
        public string type { get; set; }
        public int[] path { get; set; }
        public int? x { get; set; }
        public int? y { get; set; }
        public int? channel { get; set; }
        public int? target { get; set; }
        public int? water { get; set; }
    }

    public class Record
    {
        public int? time { get; set; }
        public AreaInfo[] map { get; set; }
        public Entity[] world { get; set; }
        public Action[] commands { get; set; }
        public Entity[] changes { get; set; }
        public Dictionary<string, string> config { get; set; }

        override
        public string ToString()
        {
            return string.Format("{{time: {0}, map: {1}, world: {2}, commands: {3}, changes: {4}, config: {5}}}"
                , time.HasValue ? time.Value.ToString() : "null"
                , map != null ? map.Length.ToString() : "0"
                , world != null ? world.Length.ToString() : "0"
                , commands != null ? commands.Length.ToString() : "0"
                , changes != null ? changes.Length.ToString() : "0"
                , config != null ? config.Count.ToString() : "0"
                );

        }
    };


    public class Result
    {
        public int request { get; set; }
        public string message { get; set; }
        public bool? result { get; set; }
        public int? time { get; set; }
        public Record record { get; set; }

        override
        public string ToString() {
            return string.Format("{{request: {0}, message: {1}, result: {2}, time: {3}, record: {4}}}"
                , request
                , message
                , result.HasValue ? result.ToString() : "null"
                , time.HasValue ? time.ToString() : "null"
                , record != null ? record.ToString() : "null"
                );
        }
    };

    class Program
    {
        static void Test1()
        {
            SerializationContext context = new SerializationContext();
            context.SerializationMethod = SerializationMethod.Map;
            MessagePackSerializer packer = MessagePackSerializer.Get<OEntity>(context);

            OEntity entity = new MsgViewer.OEntity();
            entity.name = "initial";
            entity.value = 10;

            // Pack Object
            byte[] data = null;
            using (MemoryStream stream = new MemoryStream())
            {
                Packer.Create(stream).Pack(10).PackObject(entity, context);
                data = stream.ToArray();
            }

            // Check Data
            foreach (byte b in data)
            {
                Debug.Write(string.Format("{0:x} ", b));
            }
            Debug.WriteLine("");

            // Unpack Object
            MemoryStream sstream = new MemoryStream(data);
            Unpacker unpacker = Unpacker.Create(sstream);

            while (unpacker.Read())
            {
                if (!unpacker.IsCollectionHeader)
                {
                    Debug.WriteLine("Int: {0}", unpacker.Unpack<int>());
                }
                else if (unpacker.IsMapHeader)
                {
                    OEntity e = unpacker.Unpack<OEntity>();
                    Debug.WriteLine("Dictionary: {0}", e.ToString());
                }
            }
            
        }

        //-----------------------------------------------------------------------------------
        static void Test2()
        {
            SerializationContext context = new SerializationContext();
            context.SerializationMethod = SerializationMethod.Map;

            TcpClient socket = new TcpClient("172.16.101.173", 17777);
            NetworkStream stream = socket.GetStream();

            {
                MemoryStream ms = new MemoryStream();
                Packer packer = Packer.Create(ms);

                Request req = new Request();

                packer.Pack<Request>(req, context);
                byte[] data = ms.ToArray();
                ms.Position = 0;
                packer.Pack(data.Length);
                ms.Write(data, 0, data.Length);

                byte[] send = ms.ToArray();
                stream.Write(send, 0, send.Length);

                ms.Close();
            }

            {
                byte[] buffer = new byte[1024 * 4];
                MemoryStream ms = new MemoryStream();
                MessagePackSerializer<Result> unpacker = MessagePackSerializer.Get<Result>();
                uint res_size = 0;
                int require = buffer.Length;
                long total = 0;
                int read = 0;
                do
                {
                    if (res_size == 0 || ms.Position < res_size)
                    {
                        read = stream.Read(buffer, 0, require);
                        if (read <= 0) break;

                        total += read;
                        Debug.WriteLine("{0} bytes read - {1} bytes total", read, total);

                        if (res_size == 0)
                        {
                            Debug.WriteLine("Stream {0} / {1}", ms.Length, ms.Capacity);
                            ms.Position = 0;
                            var unpack = Unpacking.UnpackUInt32(buffer);

                            res_size = unpack.Value;
                            ms.Position = 0;
                            ms.Write(buffer, unpack.ReadCount, read - unpack.ReadCount);

                            Debug.WriteLine("Next Message is {0} bytes", res_size);
                        }
                        else {
                            ms.Write(buffer, 0, read);

                            // Next
                            require = (int)Math.Min(buffer.Length, res_size - ms.Position);
                            Debug.WriteLine("Next Request is {0} bytes", require);
                            if (require == 0) require = buffer.Length;
                        }
                    }
                    else
                    {
                        // Try to unpacks
                        Result result = null;
                        ms.Position = 0;
                        if (ms.Length == 0) continue;
                        try
                        {
                            result = (Result)unpacker.Unpack(ms);
                        }
                        catch (Exception ex)
                        {
                            Debug.WriteLine(ex.Message);
                        }

                        if (result != null)
                            Debug.WriteLine("{0} bytes -> {1}", ms.Position, result.ToString());
                        else {
                            Debug.WriteLine("Consume {0} bytes", ms.Position);
                        }

                        // re-initialize
                        res_size = 0;
                        ms = new MemoryStream();
                    }

                } while (read > 0);
                ms.Close();
            }

            socket.Close();
        }


        //-----------------------------------------------------------------------------------
        static void Main(String[] args)
        {
            Test1();
            Test2();
        }
    }
}

import std.stdio, std.conv, std.json, std.getopt, std.socket;
import std.array, std.algorithm, std.ascii;
import std.concurrency, std.datetime;

import msgpack, std.json;

enum CallMsg {
    UNKNOWN = 0
    , EXIT = 1
    , ERRORS = 2
    , VIEWERINFO = 3
    , MANAGERINFO = 4
    , OPEN = 101
    , CLOSE = 102
    , PROVIDERINFO = 103
    , CONNECT = 201
    , DISCONNECT = 202
    , REFRESH = 203
    , SESSIONINFO = 204
};

CallMsg[] callList = [
    CallMsg.UNKNOWN
    , CallMsg.EXIT
    , CallMsg.ERRORS
    , CallMsg.VIEWERINFO
    , CallMsg.MANAGERINFO
    , CallMsg.OPEN
    , CallMsg.CLOSE
    , CallMsg.PROVIDERINFO
    , CallMsg.CONNECT
    , CallMsg.DISCONNECT
    , CallMsg.REFRESH
    , CallMsg.SESSIONINFO
];

struct Address{ string host; int port; };
struct Connection { int provider; int viewer; };

struct Msg{
    Address address;
    string logfilename;
    int[] provider;
    int[] session;
    int[] viewer;
    Connection connection;
};

class Request
{
public:
    uint request;
    int time;

    static const uint OPEN = (1 << 0);
    static const uint CLOSE = (1 << 1);
    static const uint ACTION = (1 << 2);
    static const uint UPDATE = (1 << 3);
    static const uint WORLD = (1 << 4);
    static const uint PERCEPTION = (1 << 5);
    static const uint CONFIG = (1 << 6);
    static const uint MAP = (1 << 7);

    this() {
        request = OPEN;
        time = 0;
    }
};


class ViewerManagerSocket
{
public:
    enum Mode {
        Control = 1,
        Viewer = 2,
    };

protected:
    TcpSocket socket;
    Mode mode;

public:
    this(string host, ushort port, Mode mode) {
        this.socket = new TcpSocket(getAddress(host, port)[0]);
        this.mode = mode;

        stderr.writefln("connection created");
    }

    ~this() {
        close();
    }

    void commu(ubyte[] data) {
        ubyte[] car = pack(data.length) ~ data;
        stderr.writefln("send data %d bytes", car.length);
        socket.send(car);

        ubyte[] buffer = new ubyte[1024];
        long size, total = 0;
        auto unpacker = StreamingUnpacker([]);
        int msgSize;

        stderr.writeln("wait for reply...");
        unpacker.purge();
        while(socket.isAlive) {
            size = socket.receive(buffer);
            stderr.writefln("received %d", size);
            if(size > 0) {
                total += size;

                foreach(b; buffer[0..size]) {
                    stderr.writef("%x ", b);
                }
                stderr.writefln("");
                ubyte[] d = buffer[0..size];
                unpacker.feed(d);

                stderr.writefln("feed %d bytes", unpacker.size);
                foreach(r; unpacker)
                {
                    stderr.writefln("%s", r.type);
                    if (r.type == Value.Type.array) {
                        r.value.toJSONValue.toString.writeln;
                        socket.close();
                        socket = null;
                    }
                    else if(r.type == Value.Type.map) {
                        r.value.toJSONValue.toString.writeln;
                    }
                    else {
                        stderr.writefln("%s: %s", r.type, r.toJSONValue.toString);
                    }
                } // rof
            } // fi
            else if(size == 0) {
                break;
            }
        } // rof

        socket.close();
        socket = null;
    }

    void commu(Request request) {
        if(mode == Mode.Control) {
            return ;
        }

        ubyte[] req = pack!true(request);
        stderr.writefln("packed request size == %d", req.length);

        foreach(b; req) {
            stderr.writef("%x ", b);
        }
        stderr.writefln("");

        return commu(req);
    }

     void commu(CallMsg callMsg, Msg msg) {
        if(mode == Mode.Viewer) {
            return;
        }
        
        
        return commu(pack("1") ~ pack(callMsg) ~ pack!true(msg));
    }

    void close() {
        if(!(socket is null)) {
            if(mode is Mode.Viewer) {
                Request request;
                request.request = Request.CLOSE;
                commu(request);
            }

            socket.close();
        }

        socket = null;
    }


};


void main(string[] args)
{
    string call = null;
    int callID = 0;
    string host = null;
    ushort port = 0;
    string list = null;
    bool viewer = false;

    auto helper = getopt(
            args
            , "list|l",  "comma separated list of arguments: (ex) 1,2,3", &list
            , "msgid|i", "request message id", &callID
            , "msg|m",   "request mesasge name", &call
            , "viewer|v", "connect as Viewer", &viewer
            , std.getopt.config.required
            , "host|h", "host name to send request", &host
            , std.getopt.config.required
            , "port|p", "host's port to connect", &port
            );
    if(helper.helpWanted || (!viewer && (call is null && callID == 0))) {
        defaultGetoptPrinter("Usage: <app> <arguments>", helper.options);
        return;
    }

    CallMsg callMsg = to!CallMsg(callID);
    if(callMsg == 0 && call !is null)
    {
        auto req = call.map!toUpper;
        foreach(CallMsg msgid; callList) {
            if(equal(req, msgid.to!string)) {
                callMsg = msgid;
            }
        }
        // rof
    }


    int[] idList = null;
    if(list !is null) {
        idList = list.split(",").to!(int[]);
    }

    ViewerManagerSocket socket = null;
    if(viewer) {
        socket = new ViewerManagerSocket(host, port, ViewerManagerSocket.Mode.Viewer);
        
        stderr.writefln("move as Viewer");
        Request req = new Request();

        stderr.writefln("send message");
        socket.commu(req);
    }
    else{
        stderr.writefln("You want to send %s message", callMsg);
        if(list !is null) stderr.writefln("> with list %s", idList);

        stderr.writefln("Connect to (%s, %d)", host, port);

        Msg msg;
        if(callMsg is CallMsg.VIEWERINFO) {
            msg.viewer = idList;
        }
        else if(callMsg is CallMsg.SESSIONINFO) {
            msg.session = idList;
        }
        else if(callMsg is CallMsg.PROVIDERINFO) {
            msg.provider = idList;
        }

        socket = new ViewerManagerSocket(host, port, ViewerManagerSocket.Mode.Control);
        socket.commu(callMsg, msg);
    }

    socket.close();
    stderr.writefln("exit");

    return;
}



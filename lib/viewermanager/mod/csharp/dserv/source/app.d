import std.stdio;
import std.socket;
import std.algorithm, std.conv, std.string;

import msgpack;

int main()
{
	TcpSocket serv = new TcpSocket(AddressFamily.INET);
	serv.bind(new InternetAddress(17777));
	serv.listen(1);

	Socket client = serv.accept();
	ubyte[] buffer = new ubyte[1024 * 4];
	long read = 0;
	do {
		read = client.receive(buffer);
		if(read < 0) {
			break;
		}
		"> Receive %d bytes".writefln(read);


		ubyte[] data = buffer[0..read];
		"> Send %d bytes".writefln(data.length);
		client.send(data);
	} while(read < 0);

	client.close();
	serv.close();

	return 0;
}

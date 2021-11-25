import std.stdio, std.file, std.conv, std.algorithm;

import msgpack, std.json;
import std.container.dlist;
import std.array, std.string;

import std.regex;
import std.typecons;

import std.getopt;
import std.functional;

bool compareTo(string s1, string s2)
{
    auto re = regex(`(\d+)`);
    auto m1 = matchFirst(s1, re);
    auto m2 = matchFirst(s2, re);

    // string N header
    auto s1h = !m1.empty && m1.pre.empty;
    auto s2h = !m1.empty && m1.pre.empty;

    if(s1h && s2h) {
        // s1 and s2 has header
        int n1 = m1.hit.to!int;
        int n2= m2.hit.to!int;
        return n1 < n2;
    }
    else if(s1h) {
        // s1 has a header, but s2 has no header
        return false;
    }
    else if(s2h) {
        // s2 has a header, but s1 has no header
        return true;
    }

    // s1 and s2 has no number header
    return s1 < s2;
}

class CommandLineParser {
public:
    string readln()
    {
        return std.stdio.readln();
    }
};


class CommandParser : CommandLineParser {
protected:
    string[] commands;
    int index;

public:
    this(string line, Regex!char delimiter)
    {
        commands = null;
        parse(line, delimiter);
    }

    string[] parse(string line, Regex!char delimiter)
    {
        if (line is null)
        {
            return null;
        }

        commands = line.split(delimiter);
        index = 0;

        debug (parser){
            stderr.writefln("CommandLineParser: input \"%s\", delim \"%s\"", line, delimiter);
            stderr.writefln("\tcommands: %s", commands);
        }

        return commands;
    }

    override
    string readln()
    {
        string res = null;

        if(index < commands.length) {
            res = commands[index];
            ++index;
        }

        return res;
    }
}

int main(string[] args)
{
    string logfilename = null;
    string command = null;
    string delim = r"/";

    auto helpInformation = getopt(
            args
            , "cmd|c", "command to apply (ex) -c \":list/log/1/world/:pp/:q\"", &command
            , "delimiter|delim|d", "demiliter of command (ex) -c \":list$log$1$world$:pp$:q\" -d \"$\"", &delim
            );


    if(args.length < 2) {
        defaultGetoptPrinter("[USAGE]: vlog [options] <filename>"
                , helpInformation.options
                );

        return 0;
    }
    else if(!exists(args[1]) || !isFile(args[1])) {
        writefln("File \"%s\" is not found", args[1]);
        return 1;
    }

    ubyte[] data = cast(ubyte[])read(args[1]);
    JSONValue json = data.unpack.toJSONValue;
    JSONValue cur = json;
    DList!JSONValue stack = make!(DList!JSONValue)();
    DList!string stack_str = make!(DList!string)();

    if(json.type != JSON_TYPE.ARRAY && json.type != JSON_TYPE.OBJECT) {
        // NOT A OBJECT or ARRAY
        writefln("%s", json.toString);
        return 0;
    }

    string prefix = ":";
    auto r = regex(prefix);
    string line = prefix ~ "h";
    string key;
    alias Descripter = Tuple!(string[], "keys", string, "message");
    Descripter[string] desc =
        [
            "help": Descripter(["h", "help"], "show this help")
            , "quit": Descripter(["q", "quit", "exit"], "exit this program")
            , "root" : Descripter(["r", "root"], "back to root")
            , "back" : Descripter(["b", "back", "..", "parent"], "back to parent")
            , "stack": Descripter(["s", "show", "stack"], "show parent stack")
            , "list": Descripter(["l", "list", "member"], "show current object member list")
            , "print": Descripter(["p", "print"], "print current object/array as string")
            , "pprint": Descripter(["pp", "pprint"], "print current object/array as pretty string")
        ];


    bool outputPrompt = false;
    CommandLineParser parser = null;
    if (command is null)
    {
        parser = new CommandLineParser();
        outputPrompt = true;
    }
    else
    {
        parser = new CommandParser(command, regex(delim));
        outputPrompt = false;
    }
    
    do {
        auto match = line.strip.matchFirst(r);
        
        if(match.empty) {
            // Control of vlog
            key = line.strip;
            if(!key.empty) {
                try {
                    JSONValue c;
                    if(cur.type == JSON_TYPE.ARRAY) {
                        c = cur[to!size_t(key)];
                    }
                    else {
                        c = cur[key];
                    }
                    if(c.type == JSON_TYPE.ARRAY || c.type == JSON_TYPE.OBJECT) {
                        stack.insertBack(cur);
                        cur = c;
                        stack_str.insertBack(key.to!string);
                    }
                    else {
                        writefln("%s", c.toString());
                    }
                }
                catch(JSONException ex) {
                   stderr.writefln("key \"%s\" is not found", key);
                }
                catch(ConvException ex) {
                    stderr.writefln("unknown command or key specified");
                }
            }
        }
        else if(!match.pre.empty) {
            stderr.writefln("unknown operation/command %s", line);
        }
        else if(!find(desc["help"].keys, match.post).empty) {
            if (!outputPrompt) {
                continue;
            }

            // SHOW HELP
            writeln("USAGE: ");
            writeln("\t<name>\topen object <name>");
            writeln("\t<index>\topen object <index>");

            foreach(v; desc) {
                write("\t");
                foreach(cmd; v.keys){
                    writef(":%s ", cmd);
                }
                write("\t");
                writef("%s\t", v.message);
                writeln();
            }
        }
        else if(!find(desc["quit"].keys, match.post).empty) {
            // EXIT PROGRAM
            break;
        }
        else if(!find(desc["print"].keys, match.post).empty) {
            // PRINT CURRENT
            writefln("%s", cur.toString());
        }
        else if(!find(desc["pprint"].keys, match.post).empty) {
            // PRETTY PRINT CURRENT
            writefln("%s", cur.toPrettyString());
        }
        else if(!find(desc["back"].keys, match.post).empty) {
            // BACK TO PARENT
            if (stack.empty) {
                stderr.writeln("stack is empty");
            }
            else {
                cur = stack.back;
                stack.removeBack;
                stack_str.removeBack;
            }
        }
        else if(!find(desc["root"].keys, match.post).empty) {
            // BACK TO ROOT
            cur = json;
            stack.clear();
            stack_str.clear();
        }
        else if(!find(desc["stack"].keys, match.post).empty) {
            // SHOW STACK
            if(stack_str.empty) {
                writefln("stack is empty.");
            }
            else {
                writefln("%s", join(stack_str[], " > "));
            }
        }
        else if(!find(desc["list"].keys, match.post).empty) {
            // SHOW MEMBER
            if(cur.type == JSON_TYPE.ARRAY) {
                writefln("array: [@%d]", cur.array().length);
            }
            else if(cur.type == JSON_TYPE.OBJECT) {
                write("object: {");
                string[] keys = cur.object().keys;
                write(join(sort!(compareTo)(keys), ", "));
                writeln("}");
            }
        }
        else {
            stderr.writefln("unknown command %s", match.post);
        }

        
        if(outputPrompt)
        {
            if(cur.type == JSON_TYPE.ARRAY) {
                writefln("$ array: [@%d]", cur.array().length);
            }
            else if(cur.type == JSON_TYPE.OBJECT) {
                write("$ object: {");
                string[] keys = cur.object().keys;
                write(join(sort!(compareTo)(keys), ", "));
                writeln("}");
            }

            {
                string path = join(stack_str[], "/");
                if(path.empty) { write("> "); }
                else { writef("%s > ", path); }
            }
        }
    }
    while((line = parser.readln()) !is null);

    return 0;
}






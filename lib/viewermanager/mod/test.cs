

using System;
using System.IO;
using System.Reflection;

using Viewer;

class TestObj
{
    public int Value1 { get; set; }
    public string Value2 { get; set; }
    public int[] Value3 { get; set; }

    public int a;
    public static readonly int b = 1502;

    public TestObj() {
        Value1 = 0;
        Value2 = "Hello";
        Value3 = new int[2];
        Value3[0] = 10;
        Value3[1] = 100;

        a = 1010;
    }

    override
    public string ToString()
    {
        return Utilities.ToMsg(this);
    }

    public TestObj Update(TestObj e)
    {
        Utilities.Update(this, e);
        return this;
    }
}

public class Test
{
    public static void Main(string[] args) {
        TestObj obj1 = new TestObj();
        TestObj obj2 = new TestObj();

        obj1.Value1 = 10;
        obj2.Value2 = "world";

        Console.WriteLine(obj1);
        Console.WriteLine(obj2);

        obj2.Update(obj1);

        FieldInfo info = typeof(TestObj).GetField("a");
        Console.WriteLine("a: {0}", info != null);
        info = typeof(TestObj).GetField("b");
        Console.WriteLine("b: {0}", info != null);
        Console.WriteLine("a: {0}", Utilities.GetFieldFrom<int, TestObj>("a", obj1));
        Console.WriteLine("b: {0}", Utilities.GetStaticFieldFrom<int, TestObj>("b"));

        string key = "Road";
        Console.WriteLine("{0}, {1}", key, ViewerKey.GetFromName<ViewerKey.Entity>(key));

        key = "Clear";
        Console.WriteLine("{0}, {1}", key, ViewerKey.Action.Get(key));

        Console.WriteLine(obj1);
        Console.WriteLine(obj2);

    }
}


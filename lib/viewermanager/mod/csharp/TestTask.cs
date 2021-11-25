using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;

using Tasks;

namespace Tasks {
    public class MainClass {

        public static void Main(string[] args)
        {
            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Baic Test>>>>>>>>>>>>>");
            Task<string> task = Task.Run<string>(() => {
                System.Threading.Thread.Sleep(1000);
                return "Test";
            });
            Task<string> task1 = Task.Run<string>(() => {
                System.Threading.Thread.Sleep(700);
                return "Test1";
            });

            Func<string> f1 = () => { System.Threading.Thread.Sleep(400); return "Test2"; };

            List<Task<string>> tasks = new List<Task<string>>();
            tasks.Add(task);
            tasks.Add(task1);
            tasks.Add(Task.Run<string>(f1));
            tasks.Add(Task.Run<string>(() => { System.Threading.Thread.Sleep(300); return "Test3"; }));

            System.DateTime start = System.DateTime.Now;

            int finish = 0;
            do {
                foreach (var t in tasks) {
                    if (t.Done) {
                        Console.WriteLine("{0}", t.Result);
                    }
                }
                tasks.RemoveAll(a => a.Done);
                System.Threading.Thread.Sleep(1);
            } while (tasks.Count > 0);

            System.DateTime end = System.DateTime.Now;
            Console.WriteLine("Start: {0}.{1}¥nEnd: {2}.{3}", start.Second, start.Millisecond, end.Second, end.Millisecond);
            
            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Argument Pass Test>>>>>>>>>>>>>");
            try
            {
                var structTask = Task.Run<int, int>((int a) => { return a; }, 10);
                structTask.Wait();
                Console.WriteLine("正しい. {0}", structTask.Result);
            }
            catch (Exception e)
            {
                Console.WriteLine("無理やった");
            }

            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Struct Type Test>>>>>>>>>>>>>");
            try
            {
                Task<int> structTask = Task.Run<int>(() => { return 0; });
                structTask.Wait();
                Console.WriteLine("正しい. {0}", structTask.Result);
            }
            catch (Exception e)
            {
                Console.WriteLine("無理やった");
            }

            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Exception Test>>>>>>>>>>>>>");
            try
            {
                Task<string> exc = Task.Run<string>(() => { throw new Exception("Test");; });
                exc.Wait();
                Console.WriteLine(exc.Result);
            }
            catch (Exception e) {
                Console.WriteLine("正しい．");
            }

            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Abort Test>>>>>>>>>>>>>");
            try
            {
                Task<string> exc = Task.Run<string>(() => { System.Threading.Thread.Sleep(1000); return "false"; });
                exc.WaitFor(200);
                exc.Abort();
                string res = exc.Result;
                Console.WriteLine("来ちゃダメな奴 {0}", res);
            }
            catch (Exception e)
            {
                Console.WriteLine("正しい例外: {0}", e.Message);
            }

            //-----------------------------------------------------------------------------------------------
            Console.WriteLine(">>>>>>>>>>>>>Success Test>>>>>>>>>>>>>");
            try
            {
                Task<string> exc = Task.Run<string>(() => { System.Threading.Thread.Sleep(1000); return "false"; });
                exc.WaitFor(200);
                exc.Abort();
                if (!exc.IsSuccess)
                {
                    Console.WriteLine("来なきゃいけないやつ");
                }
                else {
                    string res = exc.Result;
                    Console.WriteLine("ホントに来たらあかんやつ");
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("これ出たらアカン", e.Message);
            }

        }
    }
}

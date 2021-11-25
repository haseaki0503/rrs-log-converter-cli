using System;
using System.IO;
using System.Net.Sockets;


using System.Threading;

namespace Tasks {

    class AsyncWorker<TResult>
    {
        protected volatile object monitor;
        protected Thread thread;
        protected Exception exception;
        protected Func<TResult> func;
        protected TResult result;
        
        private void Run()
        {
            TResult res = default(TResult);
            Exception exc = null;

            try
            {
                res = func();
            }
            catch (Exception ex)
            {
                exc = ex;
            }
            finally {
                lock (monitor)
                {
                    this.result = res;
                    this.exception = exc;
                }

            }
        }

        public AsyncWorker(Func<TResult> func) {
            result = default(TResult);
            this.func = func;
            monitor = new object();
            thread = new Thread(new ThreadStart(Run));
            exception = null;
        }

        public void Start() {
            thread.Start();
        }

        public void Abort()
        {
            thread.Abort();
        }

        public bool IsSuccess
        {
            get
            {
                lock (monitor) {
                    return thread.ThreadState != ThreadState.Aborted && exception == null;
                }
            }
        }

        public void Wait() {
            if (thread.IsAlive)
            {
                thread.Join();
            }
        }

        public bool WaitFor(int millisec) {
            if (thread.IsAlive) {
                return thread.Join(millisec);
            }
            return true;
        }

        public bool Done {
            get {
                lock (monitor) {
                    return !thread.IsAlive;
                }
            }
        }
        
        public TResult Result
        {
            get {
                Wait();
                lock (monitor)
                {
                    if (thread.ThreadState == ThreadState.Aborted)
                    {
                        throw new AggregateException("Thread Aborted");
                    }
                    else if (exception != null)
                    {
                        throw exception;
                    }
                    else
                    {
                        return result;
                    } // fi
                } 
            } // teg
        }
    }

    
    public class Task {
        public static Task<TResult> Run<TResult>(Func<TResult> func)
        {
            return new Task<TResult>(new AsyncWorker<TResult>(func));
        }

        public static Task<TResult> Run<T,TResult >(Func<T, TResult> func, T obj)
        {
            return new Task<TResult>(new AsyncWorker<TResult>(() => { return func(obj); }));
        }
    }

    public class Task<TResult>
    {
        private AsyncWorker<TResult> asyncWoker;

        internal Task(AsyncWorker<TResult> worker)
        {
            asyncWoker = worker;
            worker.Start();
        }

        public void Abort()
        {
            asyncWoker.Abort();
        }

        public bool IsSuccess {
            get {
                return asyncWoker.IsSuccess;
            }
        }

        public void Wait()
        {
            asyncWoker.Wait();
        }

        public bool WaitFor(int milliseconds)
        {
            return asyncWoker.WaitFor(milliseconds);
        }

        public bool Done {
            get {
                return asyncWoker.Done;
            }
        }

        public TResult Result {
            get {
                return asyncWoker.Result;
            }
        }
    }
}

using System;
using System.Collections.Generic;

using System.Reflection;

namespace Viewer
{
    public class Utilities
    {
        public static string ToMsg<T>(T obj) {
            List<string> list = new List<string>();
            PropertyInfo[] propertyInfo = typeof(T).GetProperties();
            foreach (PropertyInfo info in propertyInfo)
            {
                if (info.PropertyType == typeof(string))
                {
                    list.Add(string.Format("\"{0}\": \"{1}\""
                        , info.Name, info.GetValue(obj)));
                }
                else if (info.PropertyType.GetInterface("IEnumerable") != null)
                {
                    list.Add(string.Format("\"{0}\": [{1}]"
                        , info.Name, info.GetValue(obj)));
                }
                else
                {
                    list.Add(string.Format("\"{0}\": {1}"
                        , info.Name, info.GetValue(obj)));
                }
            }

            return string.Format("{{{0}}}", string.Join(", ", list));
        }

        public static void Update<T>(T dst, T src)
        {
            PropertyInfo[] propertyInfo = typeof(T).GetProperties();
            foreach (PropertyInfo info in propertyInfo)
            {
                if (info.GetValue(src) != null)
                {
                    info.SetValue(dst, info.GetValue(src));
                }
            }
        }


        public static TResult GetFieldFrom<TResult, T>(string name, T obj)
        {
            FieldInfo fieldInfo = typeof(T).GetField(name);
            return (fieldInfo != null)
                ? (TResult)fieldInfo.GetValue(obj)
                : default(TResult);
        }

        public static TResult GetStaticFieldFrom<TResult, T>(string name)
        {
            FieldInfo fieldInfo = typeof(T).GetField(name);
            return (fieldInfo != null && fieldInfo.IsStatic)
                ? (TResult)fieldInfo.GetValue(null)
                : default(TResult);
        }
    }
}
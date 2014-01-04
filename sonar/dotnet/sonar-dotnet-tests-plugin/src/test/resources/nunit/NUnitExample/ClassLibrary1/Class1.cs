using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ClassLibrary1
{
    public class Class1
    {

        public void ASimpleMethod()
        {
            Console.WriteLine("A simple method");
        }

        public bool ThisMethodReturnsTheInput(bool input)
        {
            return input;
        }

        public void ThrowException()
        {
            throw new Exception("something bad happened");
        }
    }
}

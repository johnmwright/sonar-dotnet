using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using NUnit.Framework;

namespace ClassLibrary1.Test
{
    [TestFixture]
    public class Class1Tests
    {
        [Test]
        public void SimpleTest_NoAsserts()
        {
            var class1 = new Class1();
            class1.ASimpleMethod();
            
            //Does't actually do anything, just passes with some code coverage
        }

        [Test]
        [TestCase(true, true)]
        [TestCase(false, true, Description = "Expecting this to fail")]
        public void TestWithInput(Boolean input, bool expectedOutput)
        {
            var class1 = new Class1();
            var output = class1.ThisMethodReturnsTheInput(input);
            Assert.That(output, Is.EqualTo(expectedOutput), string.Format("output did not meet expectations (input {0}, expectedOutput {1})", input, expectedOutput));
        }


        //[Test]
        //[TestCase(true, true)]
        //[TestCase(false, true, Description = "Expecting this to fail")]
        public void TestWithInput(string foo)
        {
            Assert.That(foo, Is.Not.Null);
        }

    }
}

using System;
using NUnit.Framework;

namespace ClassLibrary1.Test.Foo.Bar
{
    [TestFixture]
    class Class1Tests
    {
        [Test]
        public void TestNestedNamespaceClass()
        {
            var class1 = new Class1();
            Assert.True(true);
        }

        [Test]
        public void TestWillThrowExeption()
        {
            var class1 = new Class1();
            class1.ThrowException();
        }

        [Test]
        [Ignore("Test is ignored")]
        public void IgnoredTest()
        {
            Assert.Fail();
        }
    }
}

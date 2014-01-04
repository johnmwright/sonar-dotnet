using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using NUnit.Framework;

namespace ClassLibrary1.Test
{
    [TestFixture]
    public class NestedClassesOutter
    {
        [Test]
        public void UnnestedTest()
        {
            Assert.True(true);
        }

        [TestFixture]
        public class NestedClassesInner
        {
            [Test]
            public void InnerNestedTest()
            {
                Assert.True(true);
            }

            [TestFixture]
            public class DoubleNestedClassesInner
            {

                [Test]
                public void DoubleNestedTest()
                {
                    Assert.True(true);
                }
            }
        }

    }
}

package naksha.base

import kotlin.test.*

class Foo : P_Object() {
    companion object {
        private val NAME = NotNullProperty<Any, Foo, String>(String::class, "Bernd")
        private val AGE = NotNullProperty<Any, Foo, Int>(Int::class, 0)
    }
    var name: String by NAME
    var age: Int by AGE
}

class Bar : P_Object() {
    companion object {
        private val FOO = NotNullProperty<Any, Bar, Foo>(Foo::class)
        private val FOO2 = NullableProperty<Any, Bar, Foo>(Foo::class)
    }
    var foo: Foo by FOO
    var foo2 : Foo? by FOO2
}

class ObjectProxyTest {
    @Test
    fun testSingleton() {
        val foo1 = Foo()
        foo1.name = "a"
        val foo2 = Foo()
        foo2.name = "a"
        // Set a breakpoint here.
        // There should be no delegation, the compiler should inline the setter and getter.
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testNotNullable() {
        val bar = Bar()
        val foo = bar.foo
        assertNotNull(foo)
        assertSame(foo, bar.foo)
        bar.foo.age = 12
        assertEquals(12, bar.foo.age)
        assertFalse(bar.foo.hasRaw("name"))
        assertEquals("Bernd", bar.foo.name)
        assertTrue(bar.foo.hasRaw("name"))
        assertEquals("Bernd", bar.foo.getRaw("name"))
        bar.foo.name = "Hello World"
        assertEquals("Hello World", bar.foo.name)
    }

    @Test
    fun testNullable() {
        val bar = Bar()
        assertNull(bar.foo2)
        bar.foo2 = Foo()
        val foo2 = bar.foo2
        assertNotNull(foo2)
        assertSame(foo2, bar.foo2)
    }
}
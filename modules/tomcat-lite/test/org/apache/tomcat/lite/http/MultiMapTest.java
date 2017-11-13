/*
 */
package org.apache.tomcat.lite.http;

import org.apache.tomcat.lite.http.MultiMap.Entry;

import junit.framework.TestCase;

public class MultiMapTest extends TestCase {

    MultiMap map = new MultiMap();
    MultiMap lmap = new MultiMap().insensitive();

    public void testAdd() {
        map.add("foo", "bar");
        Assert.assertEquals("bar", map.get("foo").toString());
    }

    public void testRemove() {
        map.add("foo", "bar");
        map.add("foo", "bar");
        map.add("foo1", "bar");
        Assert.assertEquals(3, map.count);
        map.remove("foo");
        Assert.assertEquals(1, map.count);
    }

    public void testRemove1() {
        map.add("foo", "bar");
        map.add("foo1", "bar");
        map.add("foo", "bar");
        Assert.assertEquals(3, map.count);
        map.remove("foo");
        Assert.assertEquals(1, map.count);
        map.remove("foo1");
        Assert.assertEquals(0, map.count);
    }

    public void testCase() {
        lmap.add("foo", "bar1");
        lmap.add("Foo", "bar2");
        lmap.add("a", "bar3");
        lmap.add("B", "bar4");
        Assert.assertEquals(4, lmap.count);
        Assert.assertEquals(3, lmap.map.size());

        Assert.assertEquals("bar3", lmap.getString("a"));
        Assert.assertEquals("bar3", lmap.getString("A"));
        Assert.assertEquals("bar1", lmap.getString("Foo"));
        Entry entry = lmap.getEntry("FOO");
        Assert.assertEquals(2, entry.values.size());
    }

}

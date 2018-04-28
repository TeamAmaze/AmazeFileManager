package com.amaze.filemanager.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ComputerParcelableTest {

    //hashcode test isn't existed, because hashcode is encrypted.

    /**
     * Purpose: when two string parameter is not null parameter, come out in the decided format
     * Input: ComputerParcelable(str1, str2) / toString()
     * Expected:
     * string format "%str1 [%str2]"
     */
    @Test
    public void testToStringNotNullTwoString() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");

        assertEquals(computerParcelable.toString(), "com1 [1]");
    }

    /**
     * Purpose: when address string parameter is null parameter, come out in the decided format
     * Input: ComputerParcelable(str1, null) / toString()
     * Expected:
     * string format "%str1 []" .. but result "%str1 [null]" => failure
     */
    @Test
    public void testToStringAddrNullString() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", null);

        assertEquals(computerParcelable.toString(), "com1 []");
    }

    /**
     * Purpose: when name string parameter is null parameter, come out in the decided format
     * Input: ComputerParcelable(null, str2) / toString()
     * Expected:
     * string format " [%str2]" .. but result "null [%str2]" => failure
     */
    @Test
    public void testToStringNameNullString() {
        ComputerParcelable computerParcelable = new ComputerParcelable(null, "1");

        assertEquals(computerParcelable.toString(), " [1]");
    }

    /**
     * Purpose: the function is working well after the constructor is created.
     * Input: ComputerParcelable(str1, str2) / describeContents()
     * Expected:
     * result is zero
     */
    @Test
    public void testDescribeContents() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        assertTrue(computerParcelable.describeContents() == 0);
    }

    /**
     * Purpose: check computerParcelable and object are the equal.
     * Input: computerParcelable.equals(object) ComputerParcelable == Object
     * Expected:
     * result is true
     */
    @Test
    public void testObjectEquals() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        Object object = new ComputerParcelable("com1", "1");

        assertTrue(computerParcelable.equals(object) == true);
    }

    /**
     * Purpose: when computerParcelable's name and object's name are not the same, confirm that the two are different.
     * Input: computerParcelable.equals(object) only ComputerParcelable.addr == Object.addr
     * Expected:
     * result is false
     */
    @Test
    public void testObjectNotEqualsName() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        Object object = new ComputerParcelable("com2", "1");

        assertTrue(computerParcelable.equals(object) == false);
    }

    /**
     * Purpose: when computerParcelable's address and object's address are not the same, confirm that the two are different.
     * Input: computerParcelable.equals(object) only ComputerParcelable.name == Object.name
     * Expected:
     * result is false
     */
    @Test
    public void testObjectNotEqualsAddr() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        Object object = new ComputerParcelable("com1", "2");

        assertTrue(computerParcelable.equals(object) == false);
    }

    /**
     * Purpose: when computerParcelable's name/address and object's name/address are not the same, confirm that the two are different.
     * Input: computerParcelable.equals(object) ComputerParcelable and Object not same(name, address)
     * Expected:
     * result is false
     */
    @Test
    public void testObjectNotEqualsTwo() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        Object object = new ComputerParcelable("com2", "2");

        assertTrue(computerParcelable.equals(object) == false);
    }

    /**
     * Purpose: when computerParcelable and not computerParcelable are compared, they should be different.
     * Input: computerParcelable.equals(object) Object is not ComputerParcelable
     * Expected:
     * result is false
     */
    @Test
    public void testObjectIsNotComputerParcelable() {
        ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
        Object object = new tempObject("com1", "1");

        assertTrue(computerParcelable.equals(object) == false);
    }

    class tempObject {
        private String name;
        private String addr;

        public tempObject(String str, String str2) {
            this.name = str;
            this.addr = str2;
        }
    }

}
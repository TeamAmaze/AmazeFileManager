package com.amaze.filemanager.filesystem.usb;

import android.hardware.usb.UsbInterface;
import android.support.annotation.Nullable;

import com.amaze.filemanager.utils.Utils;

/**
 * This class replesents a usb device.
 *
 * @see UsbOtgRepresentation#equals(Object)
 */
public class UsbOtgRepresentation {

    public final int productID, vendorID;
    public final @Nullable String serialNumber;

    public UsbOtgRepresentation(int productID, int vendorID, @Nullable String serialNumber) {
        this.productID = productID;
        this.vendorID = vendorID;
        this.serialNumber = serialNumber;
    }

    /**
     * This does not ensure a device is equal to another!
     * This tests parameters to know to a certain degree of certanty that a device is "similar enough"
     * to another one to be the same one.
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof UsbOtgRepresentation)) return false;

        UsbOtgRepresentation other = (UsbOtgRepresentation) obj;
        return productID == other.productID && vendorID == other.vendorID
                && ((serialNumber == null && other.serialNumber == null) || serialNumber.equals(other.serialNumber));
    }

    @Override
    public int hashCode() {
        int result = productID;
        result = 37 * result + vendorID;
        result = 37 * result + (serialNumber != null? serialNumber.hashCode():0);
        return result;
    }

}

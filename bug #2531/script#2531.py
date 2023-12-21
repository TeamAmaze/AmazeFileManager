# coding: utf-8

#
import uiautomator2 as u2
import time
d = u2.connect('emulator-5554')
d(resourceId="com.amaze.filemanager.debug:id/sd_main_fab").click()
d.click(0.886, 0.696)
d.send_keys("1")
d.click(0.821, 0.53)
time.sleep(3)
d.xpath('//*[@resource-id="com.amaze.filemanager.debug:id/listView"]/android.widget.RelativeLayout[2]/android.widget.RelativeLayout[2]').click()
d.xpath('//*[@resource-id="com.amaze.filemanager.debug:id/listView"]/android.widget.RelativeLayout[2]/android.widget.RelativeLayout[1]/android.widget.FrameLayout[1]').click()
d(resourceId="com.amaze.filemanager.debug:id/sd_main_fab").click()
d.click(0.886, 0.754)
d.send_keys("1")
d.click(0.818, 0.529)
d.click(0.224, 0.975)
d.xpath('//*[@resource-id="com.amaze.filemanager.debug:id/listView"]/android.widget.RelativeLayout[2]/android.widget.RelativeLayout[1]/android.widget.FrameLayout[1]').click()



d(description="More options").click()
time.sleep(3)
d.click(0.647, 0.113)
d.send_keys("1")
d.click(0.818, 0.528)
d.xpath('//*[@resource-id="com.amaze.filemanager.debug:id/listView"]/android.widget.RelativeLayout[4]/android.widget.RelativeLayout[2]').click()
time.sleep(3)
d.click(0.608, 0.555)
time.sleep(3)
d.click(0.468, 0.202)
d.click(0.229, 0.975)
time.sleep(3)
d(description="More options").click()
d.click(0.693, 0.113)
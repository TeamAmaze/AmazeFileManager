package com.amaze.filemanager.utils.cloud;

import android.content.Context;
import android.database.Cursor;

import com.amaze.filemanager.R;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.models.CloudEntry;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.CloudRail;
import com.cloudrail.si.exceptions.AuthenticationException;
import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;

/*
 * Copyright (C) 2017 Sanzhar Zholdiyarov <zholdiyarov@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class CloudSyncTask implements Runnable {
	private Cursor data;
	private Delegate delegate;
	private Context context;
	private CloudHandler cloudHandler;
	private static final String CLOUD_AUTHENTICATOR_REDIRECT_URI = "com.amaze.filemanager:/oauth2redirect";
	private boolean failedDueToException = false;
	
	
	public CloudSyncTask(Cursor data, Context context, CloudHandler cloudHandler, Delegate delegate) {
		this.data = data;
		this.delegate = delegate;
		this.cloudHandler = cloudHandler;
		this.context = context;
	}
	
	
	@Override
	public void run() {
		if (data.getCount() > 0 && data.moveToFirst()) {
			do {
				
				switch (data.getInt(0)) {
					case 1:
						try {
							CloudRail.setAppKey(data.getString(1));
						} catch (Exception e) {
							// any other exception due to network conditions or other error
							e.printStackTrace();
							showToastInfo(context.getString(R.string.failed_cloud_api_key));
						}
						break;
					case 2:
						// DRIVE
						try {
							
							CloudEntry cloudEntryGdrive = null;
							CloudEntry savedCloudEntryGdrive;
							
							
							GoogleDrive cloudStorageDrive = new GoogleDrive(context.getApplicationContext(),
									data.getString(1), "", CLOUD_AUTHENTICATOR_REDIRECT_URI, data.getString(2));
							cloudStorageDrive.useAdvancedAuthentication();
							
							if ((savedCloudEntryGdrive = cloudHandler.findEntry(OpenMode.GDRIVE)) != null) {
								// we already have the entry and saved state, get it
								
								try {
									cloudStorageDrive.loadAsString(savedCloudEntryGdrive.getPersistData());
								} catch (ParseException e) {
									e.printStackTrace();
									// we need to update the persist string as existing one is been compromised
									
									cloudStorageDrive.login();
									cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
									cloudHandler.updateEntry(OpenMode.GDRIVE, cloudEntryGdrive);
								}
								
							} else {
								
								cloudStorageDrive.login();
								cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
								cloudHandler.addEntry(cloudEntryGdrive);
							}
							
							delegate.onAddAccount(cloudStorageDrive);
						} catch (CloudPluginException e) {
							e.printStackTrace();
							deleteConnection(OpenMode.GDRIVE, context.getResources().getString(R.string.cloud_error_plugin));
						} catch (AuthenticationException e) {
							e.printStackTrace();
							showToastInfo(context.getString(R.string.cloud_fail_authenticate));
							deleteConnection(OpenMode.GDRIVE, context.getResources().getString(R.string.cloud_fail_authenticate));
						} catch (Exception e) {
							// any other exception due to network conditions or other error
							e.printStackTrace();
							deleteConnection(OpenMode.GDRIVE, context.getResources().getString(R.string.failed_cloud_new_connection));
						}
						break;
					case 3:
						// DROPBOX
						try {
							
							CloudEntry cloudEntryDropbox = null;
							CloudEntry savedCloudEntryDropbox;
							
							CloudStorage cloudStorageDropbox = new Dropbox(context.getApplicationContext(),
									data.getString(1), data.getString(2));
							
							if ((savedCloudEntryDropbox = cloudHandler.findEntry(OpenMode.DROPBOX)) != null) {
								// we already have the entry and saved state, get it
								
								try {
									cloudStorageDropbox.loadAsString(savedCloudEntryDropbox.getPersistData());
								} catch (ParseException e) {
									e.printStackTrace();
									// we need to persist data again
									
									cloudStorageDropbox.login();
									cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
									cloudHandler.updateEntry(OpenMode.DROPBOX, cloudEntryDropbox);
								}
								
							} else {
								
								cloudStorageDropbox.login();
								cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
								cloudHandler.addEntry(cloudEntryDropbox);
							}
							delegate.onAddAccount(cloudStorageDropbox);
						} catch (CloudPluginException e) {
							e.printStackTrace();
							deleteConnection(OpenMode.DROPBOX, context.getResources().getString(R.string.cloud_error_plugin));
						} catch (AuthenticationException e) {
							e.printStackTrace();
							deleteConnection(OpenMode.DROPBOX, context.getResources().getString(R.string.cloud_fail_authenticate));
						} catch (Exception e) {
							// any other exception due to network conditions or other error
							e.printStackTrace();
							deleteConnection(OpenMode.DROPBOX, context.getResources().getString(R.string.failed_cloud_new_connection));
						}
						break;
					case 4:
						// BOX
						try {
							
							CloudEntry cloudEntryBox = null;
							CloudEntry savedCloudEntryBox;
							
							CloudStorage cloudStorageBox = new Box(context.getApplicationContext(),
									data.getString(1), data.getString(2));
							
							if ((savedCloudEntryBox = cloudHandler.findEntry(OpenMode.BOX)) != null) {
								// we already have the entry and saved state, get it
								
								try {
									cloudStorageBox.loadAsString(savedCloudEntryBox.getPersistData());
								} catch (ParseException e) {
									e.printStackTrace();
									// we need to persist data again
									
									cloudStorageBox.login();
									cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
									cloudHandler.updateEntry(OpenMode.BOX, cloudEntryBox);
								}
								
							} else {
								
								cloudStorageBox.login();
								cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
								cloudHandler.addEntry(cloudEntryBox);
							}
							delegate.onAddAccount(cloudStorageBox);
						} catch (CloudPluginException e) {
							
							e.printStackTrace();
							deleteConnection(OpenMode.BOX, context.getResources().getString(R.string.cloud_error_plugin));
						} catch (AuthenticationException e) {
							e.printStackTrace();
							deleteConnection(OpenMode.BOX, context.getResources().getString(R.string.cloud_fail_authenticate));
						} catch (Exception e) {
							// any other exception due to network conditions or other error
							e.printStackTrace();
							deleteConnection(OpenMode.BOX, context.getResources().getString(R.string.failed_cloud_new_connection));
						}
						break;
					case 5:
						// ONEDRIVE
						try {
							
							CloudEntry cloudEntryOnedrive = null;
							CloudEntry savedCloudEntryOnedrive;
							
							CloudStorage cloudStorageOnedrive = new OneDrive(context.getApplicationContext(),
									data.getString(1), data.getString(2));
							
							if ((savedCloudEntryOnedrive = cloudHandler.findEntry(OpenMode.ONEDRIVE)) != null) {
								// we already have the entry and saved state, get it
								
								try {
									cloudStorageOnedrive.loadAsString(savedCloudEntryOnedrive.getPersistData());
								} catch (ParseException e) {
									e.printStackTrace();
									// we need to persist data again
									
									cloudStorageOnedrive.login();
									cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
									cloudHandler.updateEntry(OpenMode.ONEDRIVE, cloudEntryOnedrive);
								}
								
							} else {
								cloudStorageOnedrive.login();
								cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
								cloudHandler.addEntry(cloudEntryOnedrive);
							}
							
							delegate.onAddAccount(cloudStorageOnedrive);
						} catch (CloudPluginException e) {
							
							e.printStackTrace();
							deleteConnection(OpenMode.ONEDRIVE, context.getResources().getString(R.string.cloud_error_plugin));
						} catch (AuthenticationException e) {
							e.printStackTrace();
							deleteConnection(OpenMode.ONEDRIVE, context.getResources().getString(R.string.cloud_fail_authenticate));
						} catch (Exception e) {
							// any other exception due to network conditions or other error
							e.printStackTrace();
							deleteConnection(OpenMode.ONEDRIVE, context.getResources().getString(R.string.failed_cloud_new_connection));
						}
						break;
					default:
						deleteConnection(OpenMode.ONEDRIVE, context.getResources().getString(R.string.cloud_error_failed_restart));
				}
			} while (data.moveToNext());
		}
		
		delegate.onComplete(failedDueToException);
	}
	
	private void showToastInfo(String text) {
		failedDueToException = true;
		delegate.showToastInfo(text);
	}
	
	private void deleteConnection(OpenMode mode, String errorToDisplay) {
		failedDueToException = true;
		delegate.onDeleteConnection(mode, errorToDisplay);
	}
	
	public interface Delegate {
		void showToastInfo(String text);
		
		void onDeleteConnection(OpenMode mode, String errorToDisplay);
		
		void onAddAccount(CloudStorage cloudStorage);
		
		void onComplete(Boolean showDrawer);
	}
}


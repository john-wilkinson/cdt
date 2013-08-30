/*******************************************************************************
 * Copyright (c) 2009, 2012 Andrew Gvozdev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Gvozdev - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.core.language.settings.providers;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestSuite;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.testplugin.CModelMock;
import org.eclipse.cdt.core.testplugin.ResourceHelper;
import org.eclipse.cdt.core.testplugin.util.BaseTestCase;
import org.eclipse.cdt.internal.core.settings.model.CProjectDescriptionManager;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.internal.filesystem.local.LocalFileNativesManager;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

/**
 * Test cases testing LanguageSettingsProvider functionality related to persistence.
 */
public class LanguageSettingsPersistenceProjectTests extends BaseTestCase {
	// These should match extension points defined in plugin.xml
	private static final String EXTENSION_BASE_PROVIDER_ID = LanguageSettingsExtensionsTests.EXTENSION_BASE_PROVIDER_ID;
	private static final String EXTENSION_BASE_PROVIDER_NAME = LanguageSettingsExtensionsTests.EXTENSION_BASE_PROVIDER_NAME;
	private static final String EXTENSION_SERIALIZABLE_PROVIDER_ID = LanguageSettingsExtensionsTests.EXTENSION_SERIALIZABLE_PROVIDER_ID;
	private static final String EXTENSION_EDITABLE_PROVIDER_ID = LanguageSettingsExtensionsTests.EXTENSION_EDITABLE_PROVIDER_ID;
	private static final ICLanguageSettingEntry EXTENSION_SERIALIZABLE_PROVIDER_ENTRY = LanguageSettingsExtensionsTests.EXTENSION_SERIALIZABLE_PROVIDER_ENTRY;

	// Constants from LanguageSettingsProvidersSerializer
	public static final String LANGUAGE_SETTINGS_PROJECT_XML = ".settings/language.settings.xml";
	public static final String LANGUAGE_SETTINGS_WORKSPACE_XML = "language.settings.xml";

	// Arbitrary sample parameters used by the test
	private static final String CFG_ID = "test.configuration.id.0";
	private static final String CFG_ID_2 = "test.configuration.id.2";
	private static final String PROVIDER_0 = "test.provider.0.id";
	private static final String PROVIDER_1 = "test.provider.1.id";
	private static final String PROVIDER_2 = "test.provider.2.id";
	private static final String PROVIDER_NAME_0 = "test.provider.0.name";
	private static final String PROVIDER_NAME_1 = "test.provider.1.name";
	private static final String PROVIDER_NAME_2 = "test.provider.2.name";
	private static final String ATTR_PARAMETER = "parameter";
	private static final String CUSTOM_PARAMETER = "custom parameter";
	private static final String ELEM_TEST = "test";
	private static final String ELEM_PROVIDER = "provider "; // keep space for more reliable comparison
	private static final String ELEM_PROVIDER_REFERENCE = "provider-reference";

	/**
	 * Mock configuration description.
	 */
	class MockConfigurationDescription extends CModelMock.DummyCConfigurationDescription implements ILanguageSettingsProvidersKeeper {
		List<ILanguageSettingsProvider> providers;
		public MockConfigurationDescription(String id) {
			super(id);
		}
		@Override
		public void setLanguageSettingProviders(List<? extends ILanguageSettingsProvider> providers) {
			this.providers = new ArrayList<ILanguageSettingsProvider>(providers);
		}
		@Override
		public List<ILanguageSettingsProvider> getLanguageSettingProviders() {
			return providers;
		}
		@Override
		public void setDefaultLanguageSettingsProvidersIds(String[] ids) {
		}
		@Override
		public String[] getDefaultLanguageSettingsProvidersIds() {
			return null;
		}
	}

	/**
	 * Mock project description.
	 */
	class MockProjectDescription extends CModelMock.DummyCProjectDescription {
		ICConfigurationDescription[] cfgDescriptions;
		public MockProjectDescription(ICConfigurationDescription[] cfgDescriptions) {
			this.cfgDescriptions = cfgDescriptions;
		}
		public MockProjectDescription(ICConfigurationDescription cfgDescription) {
			this.cfgDescriptions = new ICConfigurationDescription[] { cfgDescription };
		}
		@Override
		public ICConfigurationDescription[] getConfigurations() {
			return cfgDescriptions;

		}
		@Override
		public ICConfigurationDescription getConfigurationById(String id) {
			for (ICConfigurationDescription cfgDescription : cfgDescriptions) {
				if (cfgDescription.getId().equals(id))
					return cfgDescription;
			}
			return null;
		}
	}

	/**
	 * Constructor.
	 * @param name - name of the test.
	 */
	public LanguageSettingsPersistenceProjectTests(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		LanguageSettingsManager.setWorkspaceProviders(null);
		super.tearDown(); // includes ResourceHelper cleanup
	}

	/**
	 * @return - new TestSuite.
	 */
	public static TestSuite suite() {
		return new TestSuite(LanguageSettingsPersistenceProjectTests.class);
	}

	/**
	 * main function of the class.
	 *
	 * @param args - arguments
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Get read-only configuration descriptions.
	 */
	private ICConfigurationDescription[] getConfigurationDescriptions(IProject project) {
		// project description
		ICProjectDescription projectDescription = CProjectDescriptionManager.getInstance().getProjectDescription(project, false);
		assertNotNull(projectDescription);
		assertEquals(1, projectDescription.getConfigurations().length);
		// configuration description
		ICConfigurationDescription[] cfgDescriptions = projectDescription.getConfigurations();
		assertNotNull(cfgDescriptions);
		return cfgDescriptions;
	}

	/**
	 * Get first read-only configuration description.
	 */
	private ICConfigurationDescription getFirstConfigurationDescription(IProject project) {
		ICConfigurationDescription[] cfgDescriptions = getConfigurationDescriptions(project);

		ICConfigurationDescription cfgDescription = cfgDescriptions[0];
		assertNotNull(cfgDescription);

		return cfgDescription;
	}


	/**
	 * TODO: refactor with ErrorParserManager
	 *
	 * @param store - name of the store
	 * @return location of the store in the plug-in state area
	 */
	public static String getStoreLocationInWorkspaceArea(String store) {
		IPath location = CCorePlugin.getDefault().getStateLocation().append(store);
		return location.toString();
	}

	private boolean enableAction = false;
	private Exception ex = null;
	private Thread mainThread = null;
	private Thread listenerThread = null;

	IResourceChangeListener listener = new IResourceChangeListener() {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (enableAction) {
				listenerThread = Thread.currentThread();
//				if (listenerThread != mainThread) {
					Exception e = new Exception("thread=[" + listenerThread.getName() + "]");
					ex = e;
					enableAction = false;
//					StackTraceElement[] stack = e.getStackTrace();
//					for (StackTraceElement element : stack) {
//						if (element.getMethodName().equals("refreshLocal")) {
//							ex = e;
//							enableAction =false;
//							break;
//						}
//					}
//				}
			}
		}
	};

	/**
	 * Test split storage in a real project 100 times.
	 */
	public void testProjectPersistence_RealProjectSplitStorage_100() throws Exception {
		// TODO
		mainThread = Thread.currentThread();
		listenerThread = mainThread;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);

		
		
		
		String usingNatives = "LocalFileNativesManager.DEFAULT";
		if (LocalFileNativesManager.isUsingNatives()) {
			usingNatives = "LocalFileNativesManager.isUsingNatives";
//			if (UnixFileNatives.isUsingNatives()) {
//				DELEGATE = new UnixFileHandler();
			{
				final String LIBRARY_NAME = "unixfile_1_0_0";
				boolean _usingNatives = false;
//				int _libattr = 0;
				try {
					System.loadLibrary(LIBRARY_NAME);
					_usingNatives = true;
					usingNatives = "UnixFileNatives";
//					_libattr = libattr();
				} catch (UnsatisfiedLinkError e) {
//					if (isLibraryPresent())
//						logMissingNativeLibrary(e);
//				} finally {
//					usingNatives = _usingNatives;
//					libattr = _libattr;
				}
			}
//			} else if (LocalFileNatives.isUsingNatives()) {
//				DELEGATE = new LocalFileHandler();
			try {
				boolean hasNatives = false;
				final String LIBRARY_NAME = "localfile_1_0_0";
				System.loadLibrary(LIBRARY_NAME);
				hasNatives = true;
				usingNatives = "LocalFileNatives";
//				isUnicode = internalIsUnicode();
//				try {
//					nativeAttributes = nativeAttributes();
//				} catch (UnsatisfiedLinkError e) {
//					// older native implementations did not support this
//					// call, so we need to handle the error silently
//				}
			} catch (UnsatisfiedLinkError e) {
//				if (isLibraryPresent())
//					logMissingNativeLibrary(e);
			}
//		} else {
			try {
				Class c = LocalFileNativesManager.class.getClassLoader().loadClass("org.eclipse.core.internal.filesystem.jdk7.Java7Handler"); //$NON-NLS-1$
//				DELEGATE = (NativeHandler) c.newInstance();
				usingNatives = "Java7Handler";
			} catch (ClassNotFoundException e) {
				// Class was missing?
				// Leave the delegate as default
			} catch (LinkageError e) {
				// Maybe the bundle was somehow loaded, the class was there but the bytecodes were the wrong version?
				// Leave the delegate as default
//			} catch (IllegalAccessException e) {
//				// We could not instantiate the object because we have no access
//				// Leave delegate as default
//			} catch (InstantiationException e) {
//				// We could not instantiate the object because of something unexpected
//				// Leave delegate as default
			} catch (ClassCastException e) {
				// The handler does not inherit from the correct class
				// Leave delegate as default
			}
//			}
		}

		for (int i=1; i<=100; i++) {
			IProject project = ResourceHelper.createCDTProjectWithConfig(this.getName() + '_' + i);
			IFile xmlStorageFilePrj;
			String xmlPrjOutOfTheWay;
			String xmlStorageFileWspLocation;
			String xmlWspOutOfTheWay;

			List<ICLanguageSettingEntry> entries = new ArrayList<ICLanguageSettingEntry>();
			entries.add(new CIncludePathEntry("path0", 0));

			{
				// get project descriptions
				ICProjectDescription prjDescriptionWritable = CProjectDescriptionManager.getInstance().getProjectDescription(project, true);
				assertNotNull(prjDescriptionWritable);
				ICConfigurationDescription[] cfgDescriptions = prjDescriptionWritable.getConfigurations();
				assertEquals(1, cfgDescriptions.length);
				ICConfigurationDescription cfgDescriptionWritable = cfgDescriptions[0];
				assertNotNull(cfgDescriptionWritable);
				assertTrue(cfgDescriptionWritable instanceof ILanguageSettingsProvidersKeeper);

				// create a provider
				LanguageSettingsSerializableProvider mockProvider = new LanguageSettingsSerializableProvider(PROVIDER_0, PROVIDER_NAME_0);
				LanguageSettingsManager.setStoringEntriesInProjectArea(mockProvider, false);
				mockProvider.setSettingEntries(cfgDescriptionWritable, null, null, entries);
				List<ILanguageSettingsProvider> providers = new ArrayList<ILanguageSettingsProvider>();
				providers.add(mockProvider);
				((ILanguageSettingsProvidersKeeper) cfgDescriptionWritable).setLanguageSettingProviders(providers);
				List<ILanguageSettingsProvider> storedProviders = ((ILanguageSettingsProvidersKeeper) cfgDescriptionWritable).getLanguageSettingProviders();
				assertEquals(1, storedProviders.size());

				// write to project description
				CProjectDescriptionManager.getInstance().setProjectDescription(project, prjDescriptionWritable);
				xmlStorageFilePrj = project.getFile(LANGUAGE_SETTINGS_PROJECT_XML);
				assertTrue(xmlStorageFilePrj.exists());
				xmlStorageFileWspLocation = getStoreLocationInWorkspaceArea(project.getName()+'.'+LANGUAGE_SETTINGS_WORKSPACE_XML);
				java.io.File xmlStorageFileWsp = new java.io.File(xmlStorageFileWspLocation);
				assertTrue(xmlStorageFileWsp.exists());
			}
			{
				ICConfigurationDescription cfgDescription = getFirstConfigurationDescription(project);
				assertNotNull(cfgDescription);
				assertTrue(cfgDescription instanceof ILanguageSettingsProvidersKeeper);

				List<ILanguageSettingsProvider> providers = ((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders();
				assertEquals(1, providers.size());
				ILanguageSettingsProvider provider = providers.get(0);
				assertTrue(provider instanceof LanguageSettingsSerializableProvider);
				assertEquals(PROVIDER_0, provider.getId());
				assertEquals(PROVIDER_NAME_0, provider.getName());

				List<ICLanguageSettingEntry> actual = provider.getSettingEntries(cfgDescription, null, null);
				assertEquals(entries.get(0), actual.get(0));
				assertEquals(entries.size(), actual.size());
			}
			{
				// Move storages out of the way
				// project storage
				String xmlStorageFilePrjLocation = xmlStorageFilePrj.getLocation().toOSString();
				java.io.File xmlFile = new java.io.File(xmlStorageFilePrjLocation);
				xmlPrjOutOfTheWay = xmlStorageFilePrjLocation+".out-of-the-way";
				java.io.File xmlFileOut = new java.io.File(xmlPrjOutOfTheWay);
				xmlFile.renameTo(xmlFileOut);
				assertFalse(xmlFile.exists());
				assertTrue(xmlFileOut.exists());

				// workspace storage
				java.io.File xmlStorageFileWsp = new java.io.File(xmlStorageFileWspLocation);
				assertTrue(xmlStorageFileWsp.exists());
				xmlWspOutOfTheWay = xmlStorageFileWspLocation+".out-of-the-way";
				java.io.File xmlWspFileOut = new java.io.File(xmlWspOutOfTheWay);
				boolean result = xmlStorageFileWsp.renameTo(xmlWspFileOut);
				assertTrue(result);
				assertFalse(xmlStorageFileWsp.exists());
				assertTrue(xmlWspFileOut.exists());
			}

			{
				// clear configuration
				ICProjectDescription prjDescriptionWritable = CProjectDescriptionManager.getInstance().getProjectDescription(project, true);
				ICConfigurationDescription[] cfgDescriptions = prjDescriptionWritable.getConfigurations();
				assertEquals(1, cfgDescriptions.length);
				ICConfigurationDescription cfgDescriptionWritable = cfgDescriptions[0];
				assertNotNull(cfgDescriptionWritable);
				assertTrue(cfgDescriptionWritable instanceof ILanguageSettingsProvidersKeeper);

				((ILanguageSettingsProvidersKeeper) cfgDescriptionWritable).setLanguageSettingProviders(new ArrayList<ILanguageSettingsProvider>());
				CProjectDescriptionManager.getInstance().setProjectDescription(project, prjDescriptionWritable);
				List<ILanguageSettingsProvider> providers = ((ILanguageSettingsProvidersKeeper) cfgDescriptionWritable).getLanguageSettingProviders();
				assertEquals(0, providers.size());
			}
			{
				// re-check if it really took it
				ICConfigurationDescription cfgDescription = getFirstConfigurationDescription(project);
				assertNotNull(cfgDescription);
				assertTrue(cfgDescription instanceof ILanguageSettingsProvidersKeeper);

				List<ILanguageSettingsProvider> providers = ((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders();
				assertEquals(0, providers.size());
			}
			{
				// close the project
				project.close(null);
			}
			{
				// open to double-check the data is not kept in some other kind of cache
				project.open(null);

				// check that list of providers is empty
				ICConfigurationDescription cfgDescription = getFirstConfigurationDescription(project);
				assertNotNull(cfgDescription);
				assertTrue(cfgDescription instanceof ILanguageSettingsProvidersKeeper);
				List<ILanguageSettingsProvider> providers = ((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders();
				assertEquals(0, providers.size());

				// Move project storage back
				project.open(null);
				enableAction =true;
				String xmlStorageFilePrjLocation = xmlStorageFilePrj.getLocation().toOSString();
				java.io.File xmlFile = new java.io.File(xmlStorageFilePrjLocation);
				xmlFile.delete();
				assertFalse("File "+xmlFile+ " still exist", xmlFile.exists());
				java.io.File xmlFileOut = new java.io.File(xmlPrjOutOfTheWay);
				xmlFileOut.renameTo(xmlFile);
				assertTrue("File "+xmlFile+ " does not exist", xmlFile.exists());
				assertFalse("File "+xmlFileOut+ " still exist", xmlFileOut.exists());

//				// TODO
//				boolean isSynchronized_0/* = fastIsSynchronized((File) xmlStorageFilePrj)*/;
//				File target = (File) xmlStorageFilePrj;
//				String point = "";
//				{
//					boolean result = false;
//					ResourceInfo info = target.getResourceInfo(false, false);
//					if (target.exists(target.getFlags(info), true)) {
//						LocalFile store = (LocalFile) target.getLocalManager().getStore(target);
////						IFileInfo fileInfo = store.fetchInfo();
////						IFileInfo fileInfo =  (FileInfo) store.fetchInfo(EFS.NONE, null);
////						IFileInfo fileInfo = store.fetchInfo(EFS.NONE, null);
//						IFileInfo fileInfo;
//						java.io.File file = store.toLocalFile(0, null);
//						String filePath = file.getAbsolutePath();
//						{
//							if (LocalFileNativesManager.isUsingNatives()) {
//								FileInfo info_1 = LocalFileNativesManager.fetchFileInfo(filePath);
//								//natives don't set the file name on all platforms
//								if (info_1.getName().length() == 0) {
//									String name = file.getName();
//									//Bug 294429: make sure that substring baggage is removed
//									info_1.setName(new String(name.toCharArray()));
//								}
////								return info_1;
//								fileInfo = info_1;
//								point += "E";
//							} else {
//								//in-lined non-native implementation
//								FileInfo info_1 = new FileInfo(file.getName());
//								final long lastModified = file.lastModified();
//								if (lastModified <= 0) {
//									//if the file doesn't exist, all other attributes should be default values
//									info_1.setExists(false);
////									return info_1;
//									fileInfo = info_1;
//									point += "K";
//								} else {
//									info_1.setLastModified(lastModified);
//									info_1.setExists(true);
//									info_1.setLength(file.length());
//									info_1.setDirectory(file.isDirectory());
//									info_1.setAttribute(EFS.ATTRIBUTE_READ_ONLY, file.exists() && !file.canWrite());
//									info_1.setAttribute(EFS.ATTRIBUTE_HIDDEN, file.isHidden());
////									return info_1;
//									fileInfo = info_1;
//									point += "P";
//								}
//							}
//						}
//						if (/*!fileInfo.isDirectory() && */info.getLocalSyncInfo() == fileInfo.getLastModified())
//							result = true;
//					} else {
//						point += "X";
//					}
////					return result;
//					isSynchronized_0 = result;
//				}

				// TODO
				waitForIndexer(CCorePlugin.getDefault().getCoreModel().create(project));
				// Refresh storage in workspace
				enableAction =true;
				xmlStorageFilePrj.refreshLocal(IResource.DEPTH_ZERO, null);
				boolean exists = xmlStorageFilePrj.exists();
				enableAction = !exists;
				if (enableAction) {
					Thread.sleep(5000);
					if (ex != null) {
						throw ex;
					} else {
						fail("Listener was not activated");
					}
				}

//				boolean isSynchronized = fastIsSynchronized((File) xmlStorageFilePrj);
//				boolean isSynchronized_1 = fastIsSynchronized((File) xmlStorageFilePrj);
//				boolean exists = xmlStorageFilePrj.exists();
//				boolean isSynchronized_2 = fastIsSynchronized((File) xmlStorageFilePrj);
//				assertTrue("i=" + i + ", point=" + point + ", " + usingNatives + ", sync=" + isSynchronized_0 + "," + isSynchronized + "," + isSynchronized_1 + "," + isSynchronized_2 + ": File "+xmlStorageFilePrj+ " does not exist", exists);


				// and close
				project.close(null);
			}

			{
				// Move workspace storage back
				java.io.File xmlWspFile = new java.io.File(xmlStorageFileWspLocation);
				xmlWspFile.delete();
				assertFalse("File "+xmlWspFile+ " still exist", xmlWspFile.exists());
				java.io.File xmlWspFileOut = new java.io.File(xmlWspOutOfTheWay);
				xmlWspFileOut.renameTo(xmlWspFile);
				assertTrue("File "+xmlWspFile+ " does not exist", xmlWspFile.exists());
				assertFalse("File "+xmlWspFileOut+ " still exist", xmlWspFileOut.exists());
			}

			{
				// Remove project from internal cache
				CProjectDescriptionManager.getInstance().projectClosedRemove(project);
				// open project and check if providers are loaded
				project.open(null);
				ICConfigurationDescription cfgDescription = getFirstConfigurationDescription(project);
				assertNotNull(cfgDescription);
				assertTrue(cfgDescription instanceof ILanguageSettingsProvidersKeeper);

				List<ILanguageSettingsProvider> providers = ((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders();
				assertEquals(1, providers.size());
				ILanguageSettingsProvider loadedProvider = providers.get(0);
				assertTrue(loadedProvider instanceof LanguageSettingsSerializableProvider);
				assertEquals(PROVIDER_0, loadedProvider.getId());
				assertEquals(PROVIDER_NAME_0, loadedProvider.getName());

				List<ICLanguageSettingEntry> actual = loadedProvider.getSettingEntries(cfgDescription, null, null);
				assertEquals(entries.get(0), actual.get(0));
				assertEquals(entries.size(), actual.size());
			}
		}
	}

	public boolean fastIsSynchronized(File target) {
		boolean result = false;
		ResourceInfo info = target.getResourceInfo(false, false);
		if (target.exists(target.getFlags(info), true)) {
			IFileInfo fileInfo = target.getLocalManager().getStore(target).fetchInfo();
			if (!fileInfo.isDirectory() && info.getLocalSyncInfo() == fileInfo.getLastModified())
				result = true;
		}
		return result;
	}


}

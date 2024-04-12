/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.testwebgui.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="DownloadBean")
@SessionScoped
@SuppressWarnings("serial")
public class DownloadBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(DownloadBean.class);

	private static final String ZIP_NAME = "files.zip";

	private List<FileAdapter> files;

	private String curDirPath;

	private FileAdapter[] selectedFiles = new FileAdapter[0];

	private boolean upNotAvailable;

    private boolean showHiddenFiles;

    private String loadedPageOfFile;

    private transient PagedFileViewer pagedFileViewer;

    private transient String searchPhrase;

    //restore transient fields after deserialization
    private Object readResolve()  {
        init();
        return this;
    }

	@PostConstruct
	public void init() {
		changeDirectory(".");
	}

	public void preRenderView() {
		if(SFLocalContext.getDefault() == null) {
			BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
		}
	}

	private void fillFiles() throws FileNotFoundException, WorkspaceSecurityException {

		IWorkspaceDispatcher workspaceDispatcher = BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class);

		ArrayList<String> children = new ArrayList<String>(workspaceDispatcher.listFiles(null, FolderType.ROOT, curDirPath));
		this.files = new ArrayList<FileAdapter>();

		for (String file : children) {
			try {
				FileAdapter adapter = new FileAdapter(
						workspaceDispatcher.getFile(FolderType.ROOT, curDirPath, file),
						curDirPath + File.separator + file);
                files.add(adapter);
			} catch (FileNotFoundException | WorkspaceSecurityException e) {
				// skip file
				logger.info("Failed to get file from workspace", e );
			}
		}

        try {
            files.sort(FileNameComparator.INSTANCE);
        } catch(IllegalArgumentException e) {
            String fileNames = files.stream()
                    .map(a -> a.getFile().toString())
                    .collect(Collectors.joining(", "));
            logger.error("Can't sort files in directory: " + curDirPath, e);
            logger.info("List of files: [{}]", fileNames);
        }

        if (!upNotAvailable) {
            File currentDir = workspaceDispatcher.getFile(FolderType.ROOT, curDirPath);
			FileAdapter parent = new FileAdapter(
					currentDir.getParentFile(),
					new File(curDirPath).getParent());
			parent.setName("..");
            files.add(0, parent);
		}
	}

	public StreamedContent getStrContent() {
		logger.info("getStrContent invoked {}", getUser());
        if(selectedFiles.length != 1) {
            return null; // Message?
        }
        if(selectedFiles[0].isDirectory()) {
            return null; // Message?
        }

        StreamedContent content = selectedFiles[0].getStrContent();
        return content == null ? null : content;
    }

	public StreamedContent getZipContent() {
		logger.info("getZipContent invoked {}", getUser());

        try {

			String absoluteWebPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/");
			File zipFile = new File(absoluteWebPath, ZIP_NAME);

			if (zipFile.exists()) {
				zipFile.delete();
			}

			AppZip appZip = new AppZip();

            for(FileAdapter fa : selectedFiles) {
				appZip.generateFileList(fa.getFile());
			}

			appZip.zipIt(zipFile.getAbsoluteFile().toString());

			FileInputStream in = new FileInputStream(zipFile);

            return new DefaultStreamedContent(in, "application/zip", ZIP_NAME);

		} catch (IOException e) {
			logger.error("Zip Downloading Error", e.getMessage(), e);
		}

        return null;
	}

	public void postDownload() {
		logger.info("postDownload invoked {}", getUser());
		String absoluteWebPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/");
		File zipFile = new File(absoluteWebPath, ZIP_NAME);

		if (zipFile.exists()) {
			zipFile.delete();
		}

	}

	public String getCurrentDir() {
		return curDirPath;
	}

	public void setCurrentDir(String newDirectoryPath) {
		logger.info("setCurrentDir invoked {}", getUser());
		changeDirectory(newDirectoryPath);
	}

	public List<FileAdapter> getFiles() {
        return files;
	}

	public FileAdapter[] getSelectedFiles() {
        return selectedFiles;
	}

	public void setSelectedFiles(FileAdapter[] selectedFiles) {
		logger.info("setSelectedFiles invoked {} selectedFiles[{}]", getUser(), selectedFiles);
		this.selectedFiles = selectedFiles;
	}

	public void getFolderSize() {
		logger.info("getFolderSize invoked {}", getUser());
        if(selectedFiles == null) {
			return;
		}
		for (FileAdapter fileAdapter : selectedFiles) {
			if (fileAdapter.isDirectory()) {
                int index = files.indexOf(fileAdapter);
				if (index != -1) {
                    files.get(index).updateFolderSize();
				} else {
					logger.warn("Current directory {} does not contain subfolder {}", curDirPath, fileAdapter.getFile().getName());
				}
			}
		}
	}

	public void toSelected() {
		logger.info("toSelected invoked {}", getUser());
        if(selectedFiles.length != 1 || !selectedFiles[0].isDirectory()) {
            return;
        }

        changeDirectory(selectedFiles[0].getRelativePath());
	}

	public void toTextFolder() {
		// setCurrentDir was called before. Directory had been changed.
		logger.info("toTextFolder invoked {}", getUser());
	}

	public void toParentFolder() {
		logger.info("toParentFolder invoked {} curPath[{}]", getUser(), curDirPath);
		changeDirectory(new File(curDirPath).getParentFile().toString());
	}

	public boolean isDownloadNotAvailable() {
        return selectedFiles.length != 1 || selectedFiles[0].isDirectory(); // only 1 file should be selected;
    }

	public boolean isSizeBtnNotAvailable() {
		if (selectedFiles == null) {
			return true;
		}

		for (FileAdapter _file : selectedFiles) {
			if (_file.isDirectory()) {
				return false;
			}
		}
		return selectedFiles.length < 1;
	}

	public boolean isDownloadZipNotAvailable() {
		return selectedFiles.length == 0;
	}

	public boolean isOpenFolderNotAvailable() {
        return selectedFiles.length != 1 || !selectedFiles[0].isDirectory(); // only 1 folder should be selected;
    }

	public boolean isUpNotAvailable() {
        return upNotAvailable;
	}

	public boolean isShowHiddenFiles() {
        return showHiddenFiles;
	}

	public void setShowHiddenFiles(boolean showHiddenFiles) {
		this.showHiddenFiles = showHiddenFiles;
	}

	protected String getUser(){
		return System.getProperty("user.name");
	}

	private boolean updateCurrentPath(String newPath) {
		if (newPath == null) {
			return false;
		}
		try {

			IWorkspaceDispatcher workspaceDispatcher = BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class);

			File newFile = workspaceDispatcher.getFile(FolderType.ROOT, newPath);
			if (!newFile.isDirectory()) {
				return false;
			}
			curDirPath = newPath;
            upNotAvailable = ".".equals(newPath);
			return true;
		} catch (FileNotFoundException | WorkspaceSecurityException e) {
			logger.error("Update current path to {} failed", newPath, e);
			return false;
		}
	}


	private void changeDirectory(String newDirectoryPath) {
	    updateCurrentPath(newDirectoryPath);
        try {
			fillFiles();
		} catch (FileNotFoundException | WorkspaceSecurityException e) {
			logger.error("", e);
		}
        selectedFiles = new FileAdapter[0];
	}

    public boolean isShowTextNotAvailable() {
        return selectedFiles.length != 1 || selectedFiles[0].isDirectory();
    }

    public boolean isDeleteFileAvailable(FileAdapter fileAdapter) {
        if (fileAdapter.isDirectory()) {
            return false;
        }

        IWorkspaceDispatcher workspaceDispatcher = BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class);

        try {
            return workspaceDispatcher.isLastLayerFile(FolderType.ROOT, curDirPath, fileAdapter.getFile().getName());
        } catch (Exception e) {
            logger.warn("Unable to get information about file {}", fileAdapter.getFile().getName(), e);
        }
        return false;
    }

    public boolean isDeleteFilesNotAvailable() {
        if (Stream.of(selectedFiles).anyMatch(FileAdapter::isDirectory)) {
            return true;
        }

        IWorkspaceDispatcher workspaceDispatcher = BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class);

        return selectedFiles.length == 0 || Stream.of(selectedFiles)
                .map(FileAdapter::getFile).anyMatch(file ->
                {
                    try {
                        return !workspaceDispatcher.isLastLayerFile(FolderType.ROOT, curDirPath, file.getName());
                    } catch (Exception e) {
                        logger.warn("Unable to get information about file {}", file.getName(), e);
                        return true;
                    }
                });
    }

    public void deleteFiles() throws FileNotFoundException {
        if (selectedFiles.length > 0) {
            IWorkspaceDispatcher workspaceDispatcher = BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class);

            for (FileAdapter fa : selectedFiles) {
                try {
                    workspaceDispatcher.removeFile(FolderType.ROOT, curDirPath, fa.getName());
                } catch (Exception e) {
                    logger.warn("Unable to remove file {}", fa.getName(), e);
                }
            }
            fillFiles();
        }
    }

    public List<FileAdapter> getTopSelectedFiles(int number) {
        return Arrays.stream(selectedFiles)
                .limit(number)
                .sorted(Comparator.comparing(FileAdapter::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public int getNumberOfSelectedFiles() {
        return selectedFiles.length;
    }

    public void showTextContentModal() {

        loadPage("new");
    }

    public void nextPage() {
        loadPage("next");
    }

    public void previousPage() {

        loadPage("prev");
    }

    public void firstPage() {

        loadPage("first");
    }

    public void lastPage() {

        loadPage("last");
    }

    public void find(boolean reverse) {
        loadPage(reverse ? "findBack" : "findNext");
    }

    private void loadPage(String command) {
        try {
            switch (command) {
            case "new":
                pagedFileViewer = new PagedFileViewer(selectedFiles[0].getFile(), 45, 180);
                loadedPageOfFile = pagedFileViewer.readNextPage();
                searchPhrase = null;
                break;
            case "next":
                loadedPageOfFile = pagedFileViewer.readNextPage();
                searchPhrase = null;
                break;
            case "prev":
                loadedPageOfFile = pagedFileViewer.readPrevPage();
                searchPhrase = null;
                break;
            case "first":
                loadedPageOfFile = pagedFileViewer.readFirstPage();
                searchPhrase = null;
                break;
            case "last":
                loadedPageOfFile = pagedFileViewer.readLastPage();
                searchPhrase = null;
                break;
            case "findBack":
                loadedPageOfFile = pagedFileViewer.find(searchPhrase, true);
                break;
            case "findNext":
                loadedPageOfFile = pagedFileViewer.find(searchPhrase, false);
                break;
            }
        } catch (IOException e) {
            logger.error("Fail to load content of file: " + selectedFiles[0].getName(), e);
            loadedPageOfFile = "Fail to load content of file, see application log for details.";
        } finally {
            RequestContext.getCurrentInstance().update("showTextForm");
            RequestContext.getCurrentInstance().execute("PF('showTxtDlg').show()");
        }
    }

    public boolean isNotFound() {
        return pagedFileViewer != null ? pagedFileViewer.isNotFound() : true;
    }

    public boolean isPrevPageNotAvailable() {

        return pagedFileViewer == null || pagedFileViewer.isPrevPageNotAvailable();
    }

    public boolean isNextPageNotAvailable() {

        try {
            return pagedFileViewer == null || pagedFileViewer.isNextPageNotAvailable();
        } catch (IOException e) {
            return false;
        }
    }

    public String getViewedFileName() {

        return selectedFiles.length == 1 ? "Content of the selected file - " + selectedFiles[0].getName() : "";
    }

    public void contentDialogClose() {

        loadedPageOfFile = null;
        searchPhrase = null;

        try {
            pagedFileViewer.close();
        } catch (Exception e) {
            logger.error("Exception while closing resourse", e);
        } finally {
            pagedFileViewer = null;
        }
    }

    public String getLoadedPageOfFile() {
        return loadedPageOfFile;
    }

    public String getSearchPhrase() {
        return searchPhrase;
    }

    public void setSearchPhrase(String searchPhrase) {
        if(!searchPhrase.equals(this.searchPhrase) && pagedFileViewer != null) {
            pagedFileViewer.setFindInCurrentPage(true);
        }
        this.searchPhrase = searchPhrase;
    }
}

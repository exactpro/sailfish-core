/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.iomatrix.MatrixFileTypes;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.storage.DefaultMatrix;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.entities.StoredMatrix;

public class DatabaseMatrixStorage extends AbstractMatrixStorage {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMatrixStorage.class);

	private final SessionFactory sessionFactory;

	public DatabaseMatrixStorage(SessionFactory sessionFactory, IWorkspaceDispatcher workspaceDispatcher) {
	    super(workspaceDispatcher);
	    this.sessionFactory = sessionFactory;
	}

	@Override
	public IMatrix addMatrix(InputStream stream, String name, String description, String creator, SailfishURI languageURI, String link, SailfishURI matrixProviderURI) {

	    String filePath = uploadMatrixToDisk(stream, name);

		StoredMatrix storedMatrix = new StoredMatrix();
		storedMatrix.setName(name);
		storedMatrix.setFilePath(filePath);
		storedMatrix.setDescription(description);
		storedMatrix.setDate(new Date());
		storedMatrix.setCreator(creator);
		storedMatrix.setLanguage(Objects.toString(languageURI, null));
		storedMatrix.setLink(link);
		storedMatrix.setProvider(Objects.toString(matrixProviderURI, null));

        Session session = null;
		Transaction tx = null;

		try {

			session = sessionFactory.openSession();
			tx = session.beginTransaction();

			session.save(storedMatrix);

			tx.commit();

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			logger.error(e.getMessage(), e);
			throw new StorageException("Can't save matrix descriptor in DB", e);

		} finally {

			if (session != null) {
				session.close();
			}

		}
		notifyListeners();

		IMatrix matrix = convertFromStoredMatrix(storedMatrix);
        writeMatrixMetadata(matrix);

		return matrix;
	}

    @Override
	public void updateMatrix(IMatrix matrix) {

		Session session = null;
		Transaction tx = null;

		try {

			session = sessionFactory.openSession();

			StoredMatrix storedMatrix = convertToStoredMatrix(matrix, session);

			storedMatrix.setDescription(matrix.getDescription());
			storedMatrix.setName(matrix.getName());
			storedMatrix.setFilePath(matrix.getFilePath());

			tx = session.beginTransaction();

			session.update(storedMatrix);

			tx.commit();

			writeMatrixMetadata(matrix);

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			logger.error(e.getMessage(), e);
			throw new StorageException("Can't update matrix descriptor in DB", e);

		} finally {

			if (session != null) {
				session.close();
			}

		}

		notifyListeners();
	}

	@Override
	public void removeMatrix(IMatrix matrix) {

		Session session = null;
		Transaction tx = null;

		try {

			session = sessionFactory.openSession();

			StoredMatrix storedMatrix = convertToStoredMatrix(matrix, session);

			tx = session.beginTransaction();

			session.delete(storedMatrix);

			tx.commit();

            removeMatrixFolder(matrix);
		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			logger.error(e.getMessage(), e);
			throw new StorageException("Can't remove matrix descriptor from DB", e);

		} finally {

			if (session != null) {
				session.close();
			}

		}

		notifyListeners();
	}

    @SuppressWarnings("unchecked")
    @Override
	public IMatrix getMatrixById(long matrixId) {
		Session session = null;
		Criteria query = null;
		List<StoredMatrix> list  = null;

		try {

			session = sessionFactory.openSession();
			query = session.createCriteria(StoredMatrix.class).add(Restrictions.idEq(matrixId));
			query.addOrder(Order.desc("id"));
			list = query.list();

		} catch (HibernateException e) {

			logger.error(e.getMessage(), e);
			throw new StorageException("Can't load matrix descriptor from DB", e);

		} finally {
			if (session != null) {
				session.close();
			}
		}

        return !list.isEmpty() ? convertFromStoredMatrix(list.get(0)) : null;
    }

    @Override
    protected void updateReloadedMatrix(IMatrix matrix) {
        Session session = null;
        Transaction tx = null;

        try {

            session = sessionFactory.openSession();

            StoredMatrix storedMatrix = convertToStoredMatrix(matrix, session);
            storedMatrix.setName(matrix.getName());
            storedMatrix.setDate(matrix.getDate());

            tx = session.beginTransaction();

            session.update(storedMatrix);

            tx.commit();

            writeMatrixMetadata(matrix);

        } catch (HibernateException e) {

            if (tx != null) {
                tx.rollback();
            }

            logger.error(e.getMessage(), e);
            throw new StorageException("Can't update matrix descriptor in DB", e);

        } finally {

            if (session != null) {
                session.close();
            }

        }
    }


    @SuppressWarnings("unchecked")
    @Override
	public List<IMatrix> getMatrixList() {
	    try {
            Session session = sessionFactory.openSession();
            try (AutoCloseable sessionCloseable = session::close) {
                Transaction transaction = session.beginTransaction();
                try {
                    Map<String, StoredMatrix> storedMatrixMap = loadStoredMatrices(session);
                    List<StoredMatrix> restoredMatrixList = restoreLostMatrices(session, storedMatrixMap.keySet());

                    transaction.commit();
                    notifyListeners();

                    return Stream.concat(storedMatrixMap.values().stream(), restoredMatrixList.stream())
                            .sorted((matrixA, matrixB) -> matrixB.getDate().compareTo(matrixA.getDate()))
                            .map(DatabaseMatrixStorage::convertFromStoredMatrix)
                            .collect(Collectors.toList());
                } catch (HibernateException e) {
                    transaction.rollback();
                    throw e;
                }
            } catch (HibernateException e) {
                logger.error(e.getMessage(), e);
                throw new StorageException("Matrices can't be loaded from database", e);
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Loading matrices failed", e);
        }
	}

    /**
     *
     * @param session Hibernate session
     * @param storedMatrixPaths Path to stored matrices
     * @return
     * @throws FileNotFoundException
     */
    @NotNull
    private List<StoredMatrix> restoreLostMatrices(Session session, Set<String> storedMatrixPaths) {
	    try {
            List<StoredMatrix> restoredMatrixList = dispatcher.listFiles(File::isFile, FolderType.MATRIX, true, ".").stream()
                    .filter(matrixPath -> isValidMatrixPath(matrixPath) && !storedMatrixPaths.contains(matrixPath))
                    .map(this::tryCreateStoredMatrix)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (StoredMatrix storedMatrix : restoredMatrixList) {
                try {
                    session.save(storedMatrix);
                } catch (HibernateException e) {
                    logger.error("Skipped matrix '{}' can't be restored", storedMatrix.getFilePath(), e);
                }
            }
            return restoredMatrixList;
        } catch (FileNotFoundException | WorkspaceSecurityException e) {
	        logger.error("Lost matrix can't be restored from Sailfish workspace", e);
        }
	    return Collections.emptyList();
    }

    /**
     * Loads stored matrices from database.
     * Inspects passed map to contains rubbish matrices. Removes them from passed map and tries to remove from database.
     * @param session Hibernate session
     * @return Map matrix paths to {@link StoredMatrix} from database
     */
    private Map<String, StoredMatrix> loadStoredMatrices(Session session) {
        Criteria query = session.createCriteria(StoredMatrix.class);

        Map<String, StoredMatrix> storedMatrixMap = ((List<StoredMatrix>)query.list()).stream()
                .collect(Collectors.toMap(StoredMatrix::getFilePath, Function.identity()));

        for(Iterator<StoredMatrix> iterator = storedMatrixMap.values().iterator(); iterator.hasNext(); ) {
            StoredMatrix storedMatrix = iterator.next();

            if (!isValidMatrixPath(storedMatrix.getFilePath())) {
                iterator.remove();

                try {
                    session.delete(storedMatrix);
                } catch (HibernateException e) {
                    logger.error("Rubbish matrix '{}' can't be deleted from database", storedMatrix.getFilePath(), e);
                }
            }
        }

        return storedMatrixMap;
    }

    private StoredMatrix tryCreateStoredMatrix(String matrixPath) {
        try {
            File matrixFile = dispatcher.getFile(FolderType.MATRIX, matrixPath);
            BasicFileAttributes basicFileAttributes = Files.readAttributes(matrixFile.toPath(), BasicFileAttributes.class);

            StoredMatrix storedMatrix = new StoredMatrix();
            storedMatrix.setFilePath(matrixPath);
            storedMatrix.setDate(Date.from(basicFileAttributes.creationTime().toInstant()));
            storedMatrix.setName(matrixFile.getName());
            return storedMatrix;
        } catch (IOException e) {
            logger.error("Skipped matrix '{}' can't be identified", matrixPath, e);
        }
        return null;
    }

    private static boolean isValidMatrixPath(String matrixPath) {
        return !FILE_NAME_MATRIX_METADATA.equals(FilenameUtils.getName(matrixPath))
                && MatrixFileTypes.detectFileType(matrixPath).isSupported();
    }

	private static StoredMatrix convertToStoredMatrix(IMatrix matrix, Session session) {
        return matrix.getId() == null ? new StoredMatrix() : (StoredMatrix)session.load(StoredMatrix.class, matrix.getId());
    }

	private static IMatrix convertFromStoredMatrix(StoredMatrix storedMatrix) {
		try {
            return new DefaultMatrix(storedMatrix.getId(), storedMatrix.getName(), storedMatrix.getDescription(),
                    storedMatrix.getCreator(), SailfishURI.parse(storedMatrix.getLanguage()), storedMatrix.getFilePath(),
                    storedMatrix.getDate(), storedMatrix.getLink(), SailfishURI.parse(storedMatrix.getProvider()));
        } catch(SailfishURIException e) {
            throw new StorageException(e);
        }
	}
}

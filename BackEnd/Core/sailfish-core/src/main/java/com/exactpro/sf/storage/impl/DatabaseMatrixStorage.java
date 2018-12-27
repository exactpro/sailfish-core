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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.configuration.workspace.FolderType;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.DefaultMatrix;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.entities.StoredMatrix;

public class DatabaseMatrixStorage extends AbstractMatrixStorage {
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

		IMatrix result = null;

		if ( !list.isEmpty() ) {
			result = convertFromStoredMatrix(list.get(0));
		}

		return result;
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

		Session session = null;
		Criteria query = null;
		List<StoredMatrix> list  = null;

		try {

			session = sessionFactory.openSession();
			query = session.createCriteria(StoredMatrix.class);
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

        try {
            Set<String> matrices = dispatcher.listFiles(File::isFile, FolderType.MATRIX, true, ".");
            if (list.size() != matrices.size()) {
                for  (StoredMatrix sm : list) {
                    matrices.remove(sm.getFilePath());
                }
            }

            Transaction tx = null;

            try {

                session = sessionFactory.openSession();
                tx = session.beginTransaction();

                for (String matrix : matrices) {
                    StoredMatrix storedMatrix = new StoredMatrix();
                    File matrixFile = dispatcher.getFile(FolderType.MATRIX, matrix);
                    storedMatrix.setFilePath(matrix);
                    BasicFileAttributes basicFileAttributes = Files.readAttributes(matrixFile.toPath(), BasicFileAttributes.class);
                    storedMatrix.setDate(Date.from(basicFileAttributes.creationTime().toInstant()));
                    storedMatrix.setName(matrixFile.getName());

                    session.save(storedMatrix);
                    list.add(storedMatrix);
                }

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
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        List<IMatrix> result  = new ArrayList<>();
		for (StoredMatrix storedMatrix : list) {
		    result.add(convertFromStoredMatrix(storedMatrix));
		}

		return result;
	}

	private StoredMatrix convertToStoredMatrix(IMatrix matrix, Session session) {

		StoredMatrix storedMatrix = null;

		if (matrix.getId() == null) {
			storedMatrix = new StoredMatrix();
		} else {
			storedMatrix = (StoredMatrix) session.load(StoredMatrix.class, matrix.getId());
		}

		return storedMatrix;

	}

	private IMatrix convertFromStoredMatrix(StoredMatrix storedMatrix) {
		try {
            return new DefaultMatrix(storedMatrix.getId(), storedMatrix.getName(), storedMatrix.getDescription(),
                    storedMatrix.getCreator(), SailfishURI.parse(storedMatrix.getLanguage()), storedMatrix.getFilePath(),
                    storedMatrix.getDate(), storedMatrix.getLink(), SailfishURI.parse(storedMatrix.getProvider()));
        } catch(SailfishURIException e) {
            throw new StorageException(e);
        }
	}
}

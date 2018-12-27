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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FilenameUtils;

import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.FileBackedList;
import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.auth.User;
import com.google.common.collect.ImmutableSet;

public class FileAuthStorage implements IAuthStorage {
    private static final String USERS_DIR = "users";
    private static final String ROLES_DIR = "roles";

    private final List<User> users;
    private final List<String> roles;
    private final ReadWriteLock usersLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock rolesLock = new ReentrantReadWriteLock(true);

    public FileAuthStorage(String path, IWorkspaceDispatcher dispatcher) {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(dispatcher, "dispatcher cannot be null");

        this.users = new FileBackedList<>(FilenameUtils.concat(path, USERS_DIR), JSONSerializer.of(User.class), dispatcher);
        this.roles = new FileBackedList<>(FilenameUtils.concat(path, ROLES_DIR), JSONSerializer.of(String.class), dispatcher);
    }

    @Override
    public boolean userExists(String name) {
        return getUser(name) != null;
    }

    @Override
    public User getUser(String name) {
        try {
            usersLock.readLock().lock();

            for(User user : users) {
                if(user.getName().equals(name)) {
                    return user;
                }
            }

            return null;
        } finally {
            usersLock.readLock().unlock();
        }
    }

    @Override
    public void addUser(User user) {
        if(userExists(user.getName())) {
            throw new StorageException("User already exists: " + user.getName());
        }

        if(!getRoles().containsAll(user.getRoles())) {
            throw new StorageException("User has unknown roles: " + user.getRoles().removeAll(getRoles()));
        }

        try {
            usersLock.writeLock().lock();
            users.add(user);
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    @Override
    public void updateUser(User user) {
        if(!getRoles().containsAll(user.getRoles())) {
            throw new StorageException("User has unknown roles: " + user.getRoles().removeAll(getRoles()));
        }

        try {
            usersLock.writeLock().lock();

            for(int i = 0; i < users.size(); i++) {
                User oldUser = users.get(i);

                if(oldUser.getName().equals(user.getName())) {
                    oldUser.setEmail(user.getEmail());
                    oldUser.setFirstName(user.getFirstName());
                    oldUser.setLastName(user.getLastName());
                    oldUser.setPassword(user.getPassword());
                    oldUser.setRoles(user.getRoles());
                    users.set(i, oldUser);
                    return;
                }
            }

            throw new StorageException("User doesn't exist: " + user.getName());
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    @Override
    public void removeUser(String name) {
        try {
            usersLock.writeLock().lock();
            User user = getUser(name);

            if(user == null) {
                throw new StorageException("User doesn't exist: " + name);
            }

            users.remove(user);
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    @Override
    public Set<User> getUsers() {
        try {
            usersLock.readLock().lock();
            return ImmutableSet.copyOf(users);
        } finally {
            usersLock.readLock().unlock();
        }
    }

    @Override
    public boolean roleExists(String name) {
        try {
            rolesLock.readLock().lock();
            return roles.contains(name);
        } finally {
            rolesLock.readLock().unlock();
        }
    }

    @Override
    public void addRole(String name) {
        try {
            rolesLock.writeLock().lock();

            if(roles.contains(name)) {
                throw new StorageException("Role already exists: " + name);
            }

            roles.add(name);
        } finally {
            rolesLock.writeLock().unlock();
        }
    }

    @Override
    public void removeRole(String name) {
        try {
            rolesLock.writeLock().lock();

            if(!roles.contains(name)) {
                throw new StorageException("Role doesn't exist: " + name);
            }

            roles.remove(name);
        } finally {
            rolesLock.writeLock().unlock();
        }
    }

    @Override
    public Set<String> getRoles() {
        try {
            rolesLock.readLock().lock();
            return ImmutableSet.copyOf(roles);
        } finally {
            rolesLock.readLock().unlock();
        }
    }
}

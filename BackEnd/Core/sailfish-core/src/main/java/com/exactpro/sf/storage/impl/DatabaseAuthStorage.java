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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.storage.entities.AppUser;
import com.exactpro.sf.storage.entities.Role;

public class DatabaseAuthStorage implements IAuthStorage {
    private final IStorage storage;

    public DatabaseAuthStorage(IStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
    }

    @Override
    public boolean userExists(String name) {
        return storage.getEntityByField(AppUser.class, "userName", name) != null;
    }

    @Override
    public User getUser(String name) {
        AppUser appUser = storage.getEntityByField(AppUser.class, "userName", name);

        if(appUser == null) {
            return null;
        }

        return convertAppUser(appUser);
    }

    private User convertAppUser(AppUser appUser) {
        User user = new User();

        user.setEmail(appUser.getEmail());
        user.setFirstName(appUser.getRealName());
        user.setLastName(appUser.getRealSurname());
        user.setName(appUser.getUserName());
        user.setPassword(appUser.getPassword());
        user.setRegistered(appUser.getRegistered());

        Set<String> roles = new HashSet<>();

        for(Role role : appUser.getRoles()) {
            roles.add(role.getRoleName());
        }

        user.setRoles(roles);

        return user;
    }

    @Override
    public void addUser(User user) {
        if(userExists(user.getName())) {
            throw new StorageException("User already exists: " + user.getName());
        }

        if(!getRoles().containsAll(user.getRoles())) {
            throw new StorageException("User has unknown roles: " + user.getRoles().removeAll(getRoles()));
        }

        AppUser appUser = new AppUser();

        appUser.setEmail(user.getEmail());
        appUser.setPassword(user.getPassword());
        appUser.setRealName(user.getFirstName());
        appUser.setRealSurname(user.getLastName());
        appUser.setRegistered(user.getRegistered());
        appUser.setUserName(user.getName());

        storage.add(appUser);

        for(String roleName : user.getRoles()) {
            Role role = storage.getEntityByField(Role.class, "roleName", roleName);
            role.getUsers().add(appUser);
            appUser.getRoles().add(role);
            storage.update(role);
        }
    }

    @Override
    public void updateUser(User user) {
        AppUser appUser = storage.getEntityByField(AppUser.class, "userName", user.getName());

        if(appUser == null) {
            throw new StorageException("User doesn't exist: " + appUser);
        }

        if(!getRoles().containsAll(user.getRoles())) {
            throw new StorageException("User has unknown roles: " + user.getRoles().removeAll(getRoles()));
        }

        appUser.setEmail(user.getEmail());
        appUser.setPassword(user.getPassword());
        appUser.setRealName(user.getFirstName());
        appUser.setRealSurname(user.getLastName());
        appUser.setRegistered(user.getRegistered());
        appUser.setUserName(user.getName());

        Set<String> roles = user.getRoles();

        for(Role role : storage.getAllEntities(Role.class)) {
            if(roles.contains(role.getRoleName())) {
                appUser.getRoles().add(role);
                role.getUsers().add(appUser);
            } else {
                appUser.getRoles().remove(role);
                role.getUsers().remove(appUser);
            }

            storage.update(role);
        }

        storage.update(appUser);
    }

    @Override
    public void removeUser(String name) {
        AppUser appUser = storage.getEntityByField(AppUser.class, "userName", name);

        if(appUser == null) {
            throw new StorageException("User doesn't exist: " + appUser);
        }

        for(Role role : appUser.getRoles()) {
            role.getUsers().remove(appUser);
            storage.update(role);
        }

        storage.delete(appUser);
    }

    @Override
    public Set<User> getUsers() {
        Set<User> users = new HashSet<>();

        for(AppUser user : storage.getAllEntities(AppUser.class)) {
            users.add(convertAppUser(user));
        }

        return Collections.unmodifiableSet(users);
    }

    @Override
    public boolean roleExists(String name) {
        return storage.getEntityByField(Role.class, "roleName", name) != null;
    }

    @Override
    public void addRole(String name) {
        if(roleExists(name)) {
            throw new StorageException("Role already exists: " + name);
        }

        Role role = new Role();

        role.setRoleName(name);
        storage.add(role);
    }

    @Override
    public void removeRole(String name) {
        Role role = storage.getEntityByField(Role.class, "roleName", name);

        if(role == null) {
            throw new StorageException("Role doesn't exist: " + name);
        }

        for(AppUser user : storage.getAllEntities(AppUser.class)) {
            user.getRoles().remove(role);
            storage.update(user);
        }

        storage.delete(role);
    }

    @Override
    public Set<String> getRoles() {
        Set<String> roles = new HashSet<>();

        for(Role role : storage.getAllEntities(Role.class)) {
            roles.add(role.getRoleName());
        }

        return Collections.unmodifiableSet(roles);
    }
}

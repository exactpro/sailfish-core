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
package com.exactpro.sf.storage;

import java.util.Set;

import com.exactpro.sf.storage.auth.User;

public interface IAuthStorage {
    public static final String ADMIN = "admin";
    public static final String USER = "user";

    public boolean userExists(String name);
    public User getUser(String name);
    public void addUser(User user);
    public void updateUser(User user);
    public void removeUser(String name);
    public Set<User> getUsers();

    public boolean roleExists(String name);
    public void addRole(String name);
    public void removeRole(String name);
    public Set<String> getRoles();
}

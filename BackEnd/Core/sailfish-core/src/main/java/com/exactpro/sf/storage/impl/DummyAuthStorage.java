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
import java.util.Set;

import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.auth.User;

public class DummyAuthStorage implements IAuthStorage {
    @Override
    public boolean userExists(String name) {
        return false;
    }

    @Override
    public User getUser(String name) {
        return null;
    }

    @Override
    public void addUser(User user) {
        // TODO Auto-generated method stub
    }

    @Override
    public void updateUser(User user) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeUser(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<User> getUsers() {
        return Collections.emptySet();
    }

    @Override
    public boolean roleExists(String name) {
        return false;
    }

    @Override
    public void addRole(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeRole(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }

}

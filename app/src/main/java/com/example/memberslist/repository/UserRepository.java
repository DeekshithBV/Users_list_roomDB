package com.example.memberslist.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.memberslist.database.User;
import com.example.memberslist.database.UserDao;
import com.example.memberslist.database.UserDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserDao userDao;
    private final LiveData<List<User>> allUsers;
    private final ExecutorService executorService;
    public UserRepository(Application application) {
        UserDatabase database = UserDatabase.getInstance(application);
        userDao = database.userDao();
        allUsers = userDao.getAllUsers();
        executorService = Executors.newSingleThreadExecutor();
    }
    public LiveData<List<User>> getAllUsers(){
        return allUsers;
    }
    public void insert(User user) {
        executorService.execute(() -> userDao.insert(user));
    }
    public void delete(User user) {
        executorService.execute(() -> userDao.deleteUser(user));
    }
    public void update(User user) {
        executorService.execute(() -> userDao.updateUser(user));
    }
}

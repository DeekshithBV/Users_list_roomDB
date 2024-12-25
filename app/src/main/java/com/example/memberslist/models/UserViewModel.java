package com.example.memberslist.models;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.memberslist.database.User;
import com.example.memberslist.repository.UserRepository;

import java.util.List;

public class UserViewModel extends AndroidViewModel {
    //private final UserDao userDao;
    private final UserRepository userRepository;
    private final LiveData<List<User>> allUsers;
    public UserViewModel(@NonNull Application application) {
        super(application);
        //UserDatabase db = UserDatabase.getInstance(application);
        userRepository = new UserRepository(application);
        //userDao = db.userDao();
        //allUsers = userDao.getAllUsers();
        allUsers = userRepository.getAllUsers();
    }

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    public void insert(User user) {
        //new InsertAsyncTask(userDao).execute(user);
        userRepository.insert(user);
    }

    public void delete(User user) {
        //new DeleteAsyncTask(userDao).execute(user);
        userRepository.delete(user);
    }

    public void update(User user) {
        userRepository.update(user);
    }
    /*private static class InsertAsyncTask extends AsyncTask<User,Void, Void> {
        private final UserDao asyncTaskDao;
        InsertAsyncTask(UserDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(User... users) {
            asyncTaskDao.insert(users[0]);
            return null;
        }
    }

    private static class DeleteAsyncTask extends AsyncTask<User, Void, Void> {
        private final UserDao asyncTaskDao;
        DeleteAsyncTask(UserDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(User... users) {
            asyncTaskDao.deleteUser(users[0]);
            return null;
        }
    }*/
}

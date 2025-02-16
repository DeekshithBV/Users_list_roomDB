package com.example.memberslist.models;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.memberslist.database.User;
import com.example.memberslist.repository.UserRepository;

import java.util.List;

public class UserViewModel extends AndroidViewModel {
    //private final UserDao userDao;
    private final UserRepository userRepository;
    private final LiveData<List<User>> allUsers;
    private LiveData<List<User>> searchResults;
    private MutableLiveData<Boolean> isAddUserDialogVisible = new MutableLiveData<>(false);
    private MutableLiveData<User> editUserDialog = new MutableLiveData<>();
    private MutableLiveData<User> deleteUserDialog = new MutableLiveData<>();
    private MutableLiveData<User> userProfileDialog = new MutableLiveData<>();
    private MutableLiveData<User> userDetailsDialog = new MutableLiveData<>();
    public MutableLiveData<String> selectedUserCount = new MutableLiveData<>();
    public MutableLiveData<Boolean> deleteOrEditLayoutVisibility = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isAuthenticateSuccess = new MutableLiveData<>(false);
    public MutableLiveData<Integer> favouriteColor = new MutableLiveData<>();

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

    public MutableLiveData<Boolean> getIsAddUserDialogVisible() {
        return isAddUserDialogVisible;
    }

    public void setIsAddUserDialogVisible(Boolean visible) {
        isAddUserDialogVisible.setValue(visible);
    }

    public MutableLiveData<User> getUserProfileDialog() {
        return userProfileDialog;
    }

    public void setUserProfileDialog(User user) {
        userProfileDialog.setValue(user);
    }

    public MutableLiveData<User> getUserDetailsDialog() {
        return userDetailsDialog;
    }

    public void setUserDetailsDialog(User user) {
        userDetailsDialog.setValue(user);
    }

    public void setDeleteUserDialog(User user) {
        deleteUserDialog.setValue(user);
    }

    public MutableLiveData<User> getDeleteUserDialog() {
        return deleteUserDialog;
    }

    public void setEditUserDialog(User user) {
        editUserDialog.setValue(user);
    }

    public MutableLiveData<User> getEditUserDialog() {
        return editUserDialog;
    }

    public LiveData<List<User>> searchUsers(String query) {
        searchResults = userRepository.searchUsersByName(query);
        return searchResults;
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

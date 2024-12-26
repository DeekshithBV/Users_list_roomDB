package com.example.memberslist.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_table")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name, gender, dob, photoUri, age;
    public User(String name, String gender, String dob, String photoUri, String age) {
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.photoUri = photoUri;
        this.age = age;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getDob() {
        return dob;
    }
    public void setDob(String dob) {
        this.dob = dob;
    }
    public String getPhotoUri() {
        return photoUri;
    }
    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }
}

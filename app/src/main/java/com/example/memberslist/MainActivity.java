package com.example.memberslist;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.memberslist.adapter.UserAdapter;
import com.example.memberslist.database.User;
import com.example.memberslist.databinding.ActivityMainBinding;
import com.example.memberslist.databinding.DialogAddUserBinding;
import com.example.memberslist.models.UserViewModel;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;
    private Uri photoUri;
    private ActivityMainBinding mBinding;
    public DialogAddUserBinding dialogAddUserBinding;
    private UserViewModel userViewModel;
    private AlertDialog addUserDetailsDialog, deleteOldDialog;
    RecyclerView recyclerView;
    UserAdapter userAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        recyclerView = mBinding.recyclerView;
        initAddUserDetailsDialog();
        userAdapter = new UserAdapter(this, mBinding, addUserDetailsDialog);
        recyclerView.setAdapter(userAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userViewModel.getAllUsers().observe(this, userAdapter::setUsers);

        userAdapter.setDeleteClickListener(user -> {
            //deleteOldDialog does not automatically become null when the dialog is dismissed.
            //To ensure it becomes null, you need to explicitly set it to null after calling dialog.dismiss()
            //How ever after reinitializing again it will not reference to dismissed dialog instead references to new dialog.
            if (deleteOldDialog != null && deleteOldDialog.isShowing()) {
                Log.d("deleteOldDialog", "onCreate: " +deleteOldDialog.hashCode());
                deleteOldDialog.dismiss();
            }

            if (addUserDetailsDialog != null && addUserDetailsDialog.isShowing()) {
                return;
            }
            if ((userAdapter.getUserProfileDialog() != null && userAdapter.getUserProfileDialog().isShowing())
                || (userAdapter.getUserDetailsDialog() != null && userAdapter.getUserDetailsDialog().isShowing())) {
                return;
            }
            initializeDeleteDialog(user);
            deleteOldDialog.show();
            Log.d("new dialog", "onCreate: "+deleteOldDialog.hashCode());
        });

        mBinding.addIcon.setOnClickListener(v -> showUserDetailsDialog(null, null, null));

        //Below code is for selection of users and checkbox.
        mBinding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                userAdapter.notifyDataSetChanged();
                if (isChecked) {
                    userAdapter.selectAll();
                    mBinding.checkbox.setVisibility(View.VISIBLE);
                    mBinding.selectAll.setVisibility(View.VISIBLE);
                } else if (!userAdapter.getSelectedUsers().isEmpty()) {
                    mBinding.checkbox.setVisibility(View.VISIBLE);
                    mBinding.selectAll.setVisibility(View.VISIBLE);
                } else {
                    userAdapter.clearAll();
                    mBinding.checkbox.setVisibility(View.GONE);
                    mBinding.selectAll.setVisibility(View.GONE);
                }
            });
        });

        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (userAdapter.getSelectedUsers().size() < userAdapter.getItemCount()) {
                mBinding.checkbox.setChecked(false);
            }
            if (!userAdapter.getSelectedUsers().isEmpty()) {
                mBinding.checkbox.setVisibility(View.VISIBLE);
                mBinding.selectAll.setVisibility(View.VISIBLE);
            } else {
                mBinding.checkbox.setVisibility(View.GONE);
                mBinding.selectAll.setVisibility(View.GONE);
            }
        });
    }

    private void initializeDeleteDialog(@NonNull User user) {
        deleteOldDialog = new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete user " + user.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    userViewModel.delete(user);
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    Log.d("new dialog on NO click", "onCreate: "+deleteOldDialog.hashCode());
                })
                .create();
    }

    public AlertDialog getDeleteDialog() {
        return deleteOldDialog;
    }

    private void initAddUserDetailsDialog() {
        //If the xml is not enclosed inside <layout> then it will throw error for below line.

        //And also initialized this dialog inside onCreate() method but got crashed during 2nd time click of edit icon
        //Because the dialog view is not null, so you have to call removeView().
        dialogAddUserBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_add_user, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogAddUserBinding.getRoot());

        addUserDetailsDialog = builder.create();
        addUserDetailsDialog.setCanceledOnTouchOutside(false);

        //Recommended line and no need the set the background in XML.
        Objects.requireNonNull(addUserDetailsDialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corner);

        //Below line also works.
        //Objects.requireNonNull(addUserDetailsDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set up gender spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, R.layout.single_textview);
        adapter.setDropDownViewResource(R.layout.single_textview);
        dialogAddUserBinding.spinnerGender.setAdapter(adapter);
    }

    public void showUserDetailsDialog(User editUser, Dialog editUserDetailsDialog, ImageView editIcon) {
        //Below lines can't use because dialogAddUserBinding is initialized and inflated inside the onCreate
        //methode, so on 2nd time click of add button the dialog view was not appearing.

        /*if (dialogAddUserBinding != null && dialogAddUserBinding.getRoot().getParent() != null) {
            ((ViewGroup) dialogAddUserBinding.getRoot().getParent()).removeView(dialogAddUserBinding.getRoot());
        }*/

        //Populate existing user data if edit operation
        if (editUser != null) {
            dialogAddUserBinding.editTextUserName.setText(editUser.getName());
            dialogAddUserBinding.DateOfBirth.setText(editUser.getDob());
            photoUri = Uri.parse(editUser.getPhotoUri());
            Glide.with(this)
                            .load(photoUri)
                                    .circleCrop()
                                            .into(dialogAddUserBinding.imageViewPhoto);
            dialogAddUserBinding.spinnerGender.setSelection(getIndexBasedOnGenderSelected(editUser.getGender()));
        } else {
            dialogAddUserBinding.editTextUserName.setText("");
            dialogAddUserBinding.DateOfBirth.setText("");
            dialogAddUserBinding.imageViewPhoto.setImageResource(R.drawable.empty_user_circle_icon);
            dialogAddUserBinding.spinnerGender.setSelection(0);
            dialogAddUserBinding.textInputLayoutDOB.setErrorEnabled(false);
            dialogAddUserBinding.editTextUserName.setError(null);
        }

        // Handle DOB selection
        dialogAddUserBinding.DateOfBirth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String dob = dayOfMonth + "/" + (month + 1) + "/" + year;
                        dialogAddUserBinding.DateOfBirth.setText(dob);
                        dialogAddUserBinding.textInputLayoutDOB.setErrorEnabled(false);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            Log.d("time", "callDialog: "+ System.currentTimeMillis() + " Dummmy : " + 31556926L * 110);
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 31556926000L * 110);
            datePickerDialog.show();
        });

        // Handle photo selection
        dialogAddUserBinding.buttonAddPhoto.setOnClickListener(v -> {
            requestCameraPermission();
            openCamera();
        });

        dialogAddUserBinding.buttonAdd.setOnClickListener(v -> {
            String userName = dialogAddUserBinding.editTextUserName.getText().toString().trim();

            String getGenderTextFromSpinner = dialogAddUserBinding.spinnerGender.getSelectedItem().toString();
            String gender;
            if (getGenderTextFromSpinner.equalsIgnoreCase("Male")){
                gender = "(M)";
            } else if (getGenderTextFromSpinner.equalsIgnoreCase("Female")) {
                gender = "(F)";
            } else {
                gender = "(U)";
            }
            String dob = Objects.requireNonNull(dialogAddUserBinding.DateOfBirth.getText()).toString();

            if (userName.isEmpty()) {
                dialogAddUserBinding.editTextUserName.setError("Please enter user name!");
                return;
            } else if (!userName.matches("^[a-zA-Z0-9 ]+$")) {
                dialogAddUserBinding.editTextUserName.setError("User name should contain only letters, numbers and spaces!");
                return;
            } else if (userName.length() > 14) {
                dialogAddUserBinding.editTextUserName.setError("User name should be less than or equal to 14 characters!");
                return;
            }

            if (dob.isEmpty()) {
                dialogAddUserBinding.textInputLayoutDOB.setError("Please select DOB!");
                dialogAddUserBinding.DateOfBirth.setHintTextColor(getResources().getColor(R.color.black));
                return;
            } /*else {
                dialogAddUserBinding.DateOfBirth.setHint("Select DOB");
            }*/

            String age = calculateAge(dob);
            String photoPath = (photoUri != null && !photoUri.toString().startsWith("android.resource://")) ? photoUri.toString() : getDefaultPhoto(gender);
            if (editUser == null) {
                User user = new User(userName, gender, dob, photoPath, age);
                userViewModel.insert(user);
            } else {
                editUser.setName(userName);
                editUser.setGender(gender);
                editUser.setAge(age);
                editUser.setDob(dob);
                editUser.setPhotoUri(photoPath);
                userViewModel.update(editUser);
                editUserDetailsDialog.dismiss();
            }
            addUserDetailsDialog.dismiss();
            photoUri = null;
        });

        dialogAddUserBinding.buttonCancel.setOnClickListener(v -> {
            addUserDetailsDialog.dismiss();
            photoUri = null;
        });

        if (deleteOldDialog != null && deleteOldDialog.isShowing()) {
            return;
        }
        if (userAdapter.getUserProfileDialog() != null && userAdapter.getUserProfileDialog().isShowing()) {
            return;
        }
        if (userAdapter.getUserDetailsDialog() != null && userAdapter.getUserDetailsDialog().isShowing() && editIcon == null) {
            return;
        }
        addUserDetailsDialog.show();
    }

    private int getIndexBasedOnGenderSelected(@NonNull String gender) {
        if (gender.equalsIgnoreCase("(m)"))
            return 0;
        else if (gender.equalsIgnoreCase("(f)"))
            return 1;
        else return 2;
    }

    @androidx.annotation.Nullable
    private String calculateAge(String dob) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        try {
            Date date = sdf.parse(dob);
            Calendar dobCalendar = Calendar.getInstance();
            dobCalendar.setTime(date);

            Calendar today = Calendar.getInstance();
            double age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);
            double age_in_month = (double) (today.get(Calendar.MONTH) - dobCalendar.get(Calendar.MONTH)) /12;
            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            age = age + age_in_month;
            String formattedAge = String.format(getResources().getString(R.string.two_digits_after_decimal), age);
            return formattedAge +  "year(s)";
        } catch (ParseException e) {
            Log.e("printStackTrace()", "An error occurred: "+e.getMessage(), e);
            return null;
        }
    }

    @NonNull
    private String getDefaultPhoto(String gender) {
        if ("(M)".equalsIgnoreCase(gender)) {
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.male_gender).toString();
        } else if ("(F)".equalsIgnoreCase(gender)) {
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.female_gender).toString();
        } else {
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.unknown_gender).toString();
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = createImageFile();
            if (photoFile != null){
                //The provided authority below and metadata(resource) in manifest file should match.
                photoUri = FileProvider.getUriForFile(this, "com.example.memberslist.files.Pictures", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile(){
        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"");
        if (!storageDir.exists()){
            storageDir.mkdirs();
        }
        try {
            return File.createTempFile("photo_", ".jpg", storageDir);
        } catch (IOException e){
            Log.e("printStackTrace()", "An error occurred: "+e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            //Initially used findviewbyid for imageview inside this method but it was returning null,
            //so Initialized it inside the class and used that here. Hence problem resolved.
            dialogAddUserBinding.imageViewPhoto.setImageURI(photoUri);
            Glide.with(this)
                            .load(photoUri)
                                    .circleCrop()
                                            .into(dialogAddUserBinding.imageViewPhoto);//Bind the circular image to the view.
            //.placeholder(R.drawable.placeholder) --> can be used to display a temporary img while the actual img is getting loaded.
            dialogAddUserBinding.imageViewPhoto.setTag(photoUri); // Save the URI as a tag
        }
    }
}
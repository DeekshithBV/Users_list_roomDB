package com.example.memberslist;

import android.Manifest;
import android.app.DatePickerDialog;
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
import android.widget.ArrayAdapter;

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
    private DialogAddUserBinding dialogAddUserBinding;
    private UserViewModel userViewModel;
    RecyclerView recyclerView;
    UserAdapter userAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        recyclerView = mBinding.recyclerView;
        userAdapter = new UserAdapter(this, mBinding);
        recyclerView.setAdapter(userAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userViewModel.getAllUsers().observe(this, userAdapter::setUsers);

        userAdapter.setDeleteClickListener(user -> new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete user " + user.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    userViewModel.delete(user);
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show());

        mBinding.addIcon.setOnClickListener(v -> userDetailsDialog());

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

    private void userDetailsDialog() {
        //If the xml is not enclosed inside <layout> then it will throw error for below line.
        dialogAddUserBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_add_user, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogAddUserBinding.getRoot());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        //Recommended line and no need the set the background in XML.
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corner);

        //Below line also works.
        //Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set up gender spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, R.layout.single_textview);
        adapter.setDropDownViewResource(R.layout.single_textview);
        dialogAddUserBinding.spinnerGender.setAdapter(adapter);

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
            //Uri photoUri = (Uri) imageViewPhoto.getTag();

            if (userName.isEmpty()) {
                dialogAddUserBinding.editTextUserName.setError("Please enter user name!");
                return;
            } else if (!userName.matches("^[a-zA-Z0-9 ]+$")) {
                dialogAddUserBinding.editTextUserName.setError("User name should contain only letters, numbers and spaces!");
                return;
            } else if (userName.length() > 15) {
                dialogAddUserBinding.editTextUserName.setError("User name should be less than or equal to 15 characters!");
                return;
            }

            if (dob.isEmpty()) {
                dialogAddUserBinding.textInputLayoutDOB.setError("Please select DOB!");
                dialogAddUserBinding.DateOfBirth.setHintTextColor(getResources().getColor(R.color.black));
                return;
            } else {
                dialogAddUserBinding.DateOfBirth.setHint("Select DOB");
            }

            String age = calculateAge(dob);
            String photoPath = photoUri != null ? photoUri.toString() : getDefaultPhoto(gender);
            User user = new User(userName, gender, dob, photoPath, age);
            userViewModel.insert(user);
            dialog.dismiss();
            photoUri = null;
        });

        dialogAddUserBinding.buttonCancel.setOnClickListener(v -> {
            dialog.dismiss();
            photoUri = null;
        });

        dialog.show();
    }

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
            e.printStackTrace();
            return null;
        }
    }

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
                //startActivity(cameraIntent);
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
            e.printStackTrace();
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
package com.example.memberslist;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.memberslist.adapter.UserAdapter;
import com.example.memberslist.database.User;
import com.example.memberslist.databinding.ActivityMainBinding;
import com.example.memberslist.databinding.DialogAddUserBinding;
import com.example.memberslist.models.UserViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.hbb20.CountryCodePicker;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
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
    private Dialog editUserDetailsDialog;
    private boolean isProgrammaticChange = false;
    private User editUser, deleteUser;
    private ImageView editIcon;
    RecyclerView recyclerView;
    private String getGenderTextFromClick = "";
    UserAdapter userAdapter;
    ColorStateList blueColor, greenColor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        recyclerView = mBinding.recyclerView;
        initAddUserDetailsDialog();
        userAdapter = new UserAdapter(this, mBinding, addUserDetailsDialog, userViewModel);
        recyclerView.setAdapter(userAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.closeIcon.setVisibility(mBinding.searchEditText.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        this.deleteUser = userViewModel.getDeleteUserDialog().getValue();
        if (deleteUser != null) {
            initializeDeleteDialog(deleteUser);
        }

        //View model observers.
        userViewModel.getAllUsers().observe(this, userAdapter::setUsers);

        userViewModel.getIsAddUserDialogVisible().observe(this, visible -> {
            if (visible) {
                addUserDetailsDialog.show();
            }
        });

        userViewModel.getEditUserDialog().observe(this, editUser -> {
            if (editUser != null && addUserDetailsDialog.isShowing()) {
                showUserDetailsDialog(editUser, userAdapter.getUserDetailsDialog(), userAdapter.userDetailsLayoutBinding.editUser);
            }
        });

        userViewModel.getDeleteUserDialog().observe(this, user -> {
            if (user != null) {
                deleteOldDialog.show();
            }
        });

        userViewModel.getUserDetailsDialog().observe(this, user -> {
            if (user != null) {
                userAdapter.userDetailsLayoutBinding.setUser(user);
                userAdapter.getUserDetailsDialog().show();
            }
        });

        userViewModel.getUserProfileDialog().observe(this, user -> {
            if (user != null) {
                userAdapter.userProfileBinding.setUser(user);
                userAdapter.getUserProfileDialog().show();
            }
        });

        userAdapter.getUserProfileDialog().setOnDismissListener(dialog -> userViewModel.setUserProfileDialog(null));

        userAdapter.getUserDetailsDialog().setOnDismissListener(dialog -> {
            userViewModel.setUserDetailsDialog(null);

            //If i add the below line one issue solves another opens.
            //userViewModel.setEditUserDialog(null);
        });

        userAdapter.setDeleteClickListener(user -> {
            //deleteOldDialog does not automatically become null when the dialog is dismissed.
            //To ensure it becomes null, you need to explicitly set it to null after calling dialog.dismiss()
            //How ever after reinitializing again it will not reference to dismissed dialog instead references to new dialog.
            initializeDeleteDialog(user);
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
            //initializeDeleteDialog(user);
            if (deleteOldDialog != null) {
                deleteOldDialog.show();
            }
            userViewModel.setDeleteUserDialog(user);
        });

        mBinding.addIcon.setOnClickListener(v -> showUserDetailsDialog(null, null, null));

        dialogAddUserBinding.buttonCancel.setOnClickListener(v -> {
            userViewModel.setIsAddUserDialogVisible(false);
            userViewModel.setEditUserDialog(null);
            addUserDetailsDialog.dismiss();
            photoUri = null;
            dialogAddUserBinding.textInputLayoutPhoneNo.setErrorEnabled(false);
        });

        // Handle DOB selection
        dialogAddUserBinding.DateOfBirth.setOnClickListener(v -> {
            Calendar calendar, oldCalendar;
            calendar = Calendar.getInstance();
            oldCalendar = Calendar.getInstance();
            //Any modifications made to calendar will also affect oldCalendar, since they are referencing the same object in memory.
            //calendar = oldCalendar = Calendar.getInstance(); --> [Chained assignment]
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String dob = dayOfMonth + "/" + (month + 1) + "/" + year;
                        dialogAddUserBinding.DateOfBirth.setText(dob);
                        dialogAddUserBinding.textInputLayoutDOB.setErrorEnabled(false);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            oldCalendar.add(Calendar.YEAR, -110);
            long minDate = oldCalendar.getTimeInMillis();
            //datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 31556926000L * 110);
            datePickerDialog.getDatePicker().setMinDate(minDate);
            datePickerDialog.show();
        });

        // Handle photo selection
        dialogAddUserBinding.buttonAddPhoto.setOnClickListener(v -> {
            requestCameraPermission();
            openCamera();
        });

        // Add user
        dialogAddUserBinding.buttonAdd.setOnClickListener(v -> {
            boolean isValidUserName, isValidUserDOB, isValidUserPhoneNo;
            String userName = dialogAddUserBinding.editTextUserName.getText().toString().trim();
            String dob = Objects.requireNonNull(dialogAddUserBinding.DateOfBirth.getText()).toString();
            //String getGenderTextFromSpinner = dialogAddUserBinding.spinnerGender.getSelectedItem().toString();
            String phoneNumber = Objects.requireNonNull(dialogAddUserBinding.editTextPhoneNo.getText()).toString();
            String countryCode = dialogAddUserBinding.countryCodePicker.getSelectedCountryCodeWithPlus();

            String age = calculateAge(dob);
            String gender = getGenderText(getGenderTextFromClick);
            String photoPath = (photoUri != null && !photoUri.toString().startsWith("android.resource://")) ? photoUri.toString() : getDefaultPhoto(gender);

            isValidUserName = checkUserNameValidation(userName);
            if (!isValidUserName) return;

            isValidUserDOB = checkUserDOBValidation(dob);
            if (!isValidUserDOB) return;

            isValidUserPhoneNo = checkUserPhoneNoValidation(phoneNumber);
            if (!isValidUserPhoneNo) return;

            if (editUser == null) {
                User user = new User(userName, gender, dob, photoPath, age, phoneNumber, countryCode);
                userViewModel.insert(user);
            } else {
                editUser.setName(userName);
                editUser.setGender(gender);
                editUser.setAge(age);
                editUser.setDob(dob);
                editUser.setPhotoUri(photoPath);
                editUser.setPhoneNumber(phoneNumber);
                editUser.setCountryCode(countryCode);
                userViewModel.update(editUser);
                editUserDetailsDialog.dismiss();
            }
            addUserDetailsDialog.dismiss();
            userViewModel.setIsAddUserDialogVisible(false);
            userViewModel.setEditUserDialog(null);
            photoUri = null;
            dialogAddUserBinding.textInputLayoutPhoneNo.setErrorEnabled(false);
        });

        //Below code is for selection of users and checkbox.
        mBinding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isChecked) {
                    userAdapter.selectAll();
                    /*mBinding.checkbox.setVisibility(View.VISIBLE);
                    mBinding.selectAll.setVisibility(View.VISIBLE);*/
                } /*else if (!userAdapter.getSelectedUsers().isEmpty()) {
                    mBinding.checkbox.setVisibility(View.VISIBLE);
                    mBinding.selectAll.setVisibility(View.VISIBLE);
                }*/ else {
                    userAdapter.clearAll();
                    mBinding.checkbox.setVisibility(View.GONE);
                    mBinding.selectAll.setVisibility(View.GONE);
                }
                userAdapter.notifyDataSetChanged();
            });
        });

        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            isProgrammaticChange = true;
            mBinding.checkbox.setChecked(userAdapter.getSelectedUsers().size() == userAdapter.getItemCount() && userAdapter.getItemCount() != 0);
            isProgrammaticChange = false;

            if (!userAdapter.getSelectedUsers().isEmpty()) {
                mBinding.checkbox.setVisibility(View.VISIBLE);
                mBinding.selectAll.setVisibility(View.VISIBLE);
            } else {
                mBinding.checkbox.setVisibility(View.GONE);
                mBinding.selectAll.setVisibility(View.GONE);
            }
        });

        mBinding.searchEditText.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
                /*if (s.toString().isEmpty())
                    mBinding.closeIcon.setVisibility(View.GONE);
                else
                    mBinding.closeIcon.setVisibility(View.VISIBLE);*/

                //If else replaced with tertiary operator.
                mBinding.closeIcon.setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.closeIcon.setOnClickListener(v -> {
            mBinding.searchEditText.setText("");
            hideKeyboard();
        });

        //Delete user by swiping left/right
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                User deletedUser = userAdapter.deleteUserAtPosition(position);
                Toast.makeText(MainActivity.this, "User" + deletedUser.getName() + "deleted", Toast.LENGTH_SHORT).show();

                Snackbar.make(recyclerView, "User " + deletedUser.getName() + " deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            userAdapter.addUserAtPosition(deletedUser, position);
                        })
                        .show();
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        addUserDetailsDialog.setOnDismissListener(dialog -> {
            dialogAddUserBinding.editTextUserName.setCursorVisible(false);
            dialogAddUserBinding.editTextPhoneNo.setCursorVisible(false);
        });

        countryCodeSelection();
        genderSelection();
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
        deleteOldDialog.setOnDismissListener(dialog -> userViewModel.setDeleteUserDialog(null));
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
        /*ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, R.layout.single_textview);
        adapter.setDropDownViewResource(R.layout.single_textview);
        dialogAddUserBinding.spinnerGender.setAdapter(adapter);*/
        blueColor = ColorStateList.valueOf(getResources().getColor(R.color.blue, null));
        greenColor = ColorStateList.valueOf(getResources().getColor(R.color.green, null));
    }

    public void showUserDetailsDialog(User editUser, Dialog editUserDetailsDialog, ImageView editIcon) {
        //Below lines can't use because dialogAddUserBinding is initialized and inflated inside the onCreate
        //methode, so on 2nd time click of add button the dialog view was not appearing.

        /*if (dialogAddUserBinding != null && dialogAddUserBinding.getRoot().getParent() != null) {
            ((ViewGroup) dialogAddUserBinding.getRoot().getParent()).removeView(dialogAddUserBinding.getRoot());
        }*/

        this.editUser = editUser;
        this.editUserDetailsDialog = editUserDetailsDialog;

        //Populate existing user data if edit operation
        if (editUser != null) {
            editUserDetails(editUser);
        } else {
            emptyUserDialog();
        }

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
        userViewModel.setIsAddUserDialogVisible(true);
        setCursorVisibilityForEditTextUserName();
        setCursorVisibilityForEditTextPhoneNumber();
    }

    @NonNull
    private String getStringBasedOnGenderSelected(@NonNull String gender) {
        if (gender.equalsIgnoreCase("(m)"))
            return getResources().getString(R.string.male);
        else if (gender.equalsIgnoreCase("(f)"))
            return getResources().getString(R.string.female);
        else if (gender.equalsIgnoreCase("(t)"))
            return getResources().getString(R.string.transgender);
        else if (gender.equalsIgnoreCase("(i)"))
            return getResources().getString(R.string.intersex);
        else if (gender.equalsIgnoreCase("(nb)"))
            return getResources().getString(R.string.non_Binary);
        else if (gender.equalsIgnoreCase("(np)"))
            return getResources().getString(R.string.not_preferred);
        else return getResources().getString(R.string.others);
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
            double age_in_month;
            if (today.get(Calendar.MONTH) >= dobCalendar.get(Calendar.MONTH))
                age_in_month = (double) (today.get(Calendar.MONTH) - dobCalendar.get(Calendar.MONTH)) / 12;
            else {
                age_in_month = dobCalendar.get(Calendar.MONTH) - today.get(Calendar.MONTH);
                age_in_month = (12 - age_in_month)/12;
            }
            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR) &&
                    today.get(Calendar.MONTH) != dobCalendar.get(Calendar.MONTH)) {
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
        if ("(M)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.male_gender).toString();
        else if ("(F)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.female_gender).toString();
        else if ("(T)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.transgender_icon).toString();
        else if ("(I)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.intersex_icon).toString();
        else if ("(NB)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.non_binary).toString();
        else if ("(NP)".equalsIgnoreCase(gender))
            return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.no_gender_prefer_icon).toString();
        else return Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.others_icon).toString();
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

    @androidx.annotation.Nullable
    private File createImageFile(){
        //Provide app specific directory for storing pictures in external storage.
        // --> /storage/emulated/0/Android/data/com.example.memberslist/files/Pictures. (path where images are stored).
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

    private void editUserDetails(@NonNull User editUser) {
        dialogAddUserBinding.editTextUserName.setText(editUser.getName());
        dialogAddUserBinding.DateOfBirth.setText(editUser.getDob());
        photoUri = Uri.parse(editUser.getPhotoUri());
        Glide.with(this)
                .load(photoUri)
                .circleCrop()
                .into(dialogAddUserBinding.imageViewPhoto);
        //dialogAddUserBinding.spinnerGender.setSelection(getStringBasedOnGenderSelected(editUser.getGender()));
        getGenderTextFromClick = getStringBasedOnGenderSelected(editUser.getGender());
        dialogAddUserBinding.editTextPhoneNo.setText(editUser.getPhoneNumber());
        dialogAddUserBinding.countryCodePicker.setCountryForPhoneCode(Integer.parseInt(editUser.getCountryCode()));
        dialogAddUserBinding.buttonAdd.setText(getResources().getString(R.string.update));
        dialogAddUserBinding.buttonAdd.setBackgroundTintList(greenColor);
    }

    private void emptyUserDialog() {
        dialogAddUserBinding.editTextUserName.setText("");
        dialogAddUserBinding.DateOfBirth.setText("");
        dialogAddUserBinding.imageViewPhoto.setImageResource(R.drawable.empty_user_circle_icon);
        //dialogAddUserBinding.spinnerGender.setSelection(0);
        getGenderTextFromClick = getResources().getString(R.string.male);
        dialogAddUserBinding.textInputLayoutDOB.setErrorEnabled(false);
        dialogAddUserBinding.editTextUserName.setError(null);
        dialogAddUserBinding.editTextPhoneNo.setText("");
        dialogAddUserBinding.textInputLayoutPhoneNo.setErrorEnabled(false);
        dialogAddUserBinding.countryCodePicker.setCountryForPhoneCode(+91);
        dialogAddUserBinding.buttonAdd.setText(getResources().getString(R.string.Add));
        dialogAddUserBinding.buttonAdd.setBackgroundTintList(blueColor);
    }

    private void searchUsers(String query) {
        userViewModel.searchUsers(query).observe(this, users -> {
            if (users == null || users.isEmpty()) {
                mBinding.noUsersTextView.setVisibility(View.VISIBLE);
                mBinding.recyclerView.setVisibility(View.GONE);
                userAdapter.setUsers(Collections.emptyList());
            } else {
                mBinding.noUsersTextView.setVisibility(View.GONE);
                mBinding.recyclerView.setVisibility(View.VISIBLE);
                userAdapter.setUsers(users);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(mBinding.searchEditText.getWindowToken(), 0);
        }
    }

    private boolean checkUserNameValidation(@NonNull String userName) {
        if (userName.isEmpty()) {
            dialogAddUserBinding.editTextUserName.setError("Please enter user name!");
            return false;
        } else if (!userName.matches("^[a-zA-Z0-9 ]+$")) {
            dialogAddUserBinding.editTextUserName.setError("User name should contain only letters, numbers and spaces!");
            return false;
        } else if (userName.length() > 14) {
            dialogAddUserBinding.editTextUserName.setError("User name should be less than or equal to 14 characters!");
            return false;
        } else {
            return true;
        }
    }

    private boolean checkUserDOBValidation(@NonNull String dob) {
        if (dob.isEmpty()) {
            dialogAddUserBinding.textInputLayoutDOB.setError("Please select DOB!");
            dialogAddUserBinding.DateOfBirth.setHintTextColor(getResources().getColor(R.color.black, null));
            return false;
        } /*else {
                dialogAddUserBinding.DateOfBirth.setHint("Select DOB");
            }*/
        else return true;
    }

    private boolean checkUserPhoneNoValidation(@NonNull String phoneNumber) {
        if (!phoneNumber.isEmpty() && !phoneNumber.matches("\\d{10}")) {
            dialogAddUserBinding.textInputLayoutPhoneNo.setError("Phone number should must be 10 digits and only numbers are allowed.");
            return false;
        }
        else if (!phoneNumber.isEmpty() && phoneNumber.length() != 10) {
            dialogAddUserBinding.textInputLayoutPhoneNo.setError("Phone number length should be 10 digits");
            return false;
        } else return true;
    }

    @NonNull
    private String getGenderText(@NonNull String getGenderTextFromClick) {
        if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.male)))
            return "(M)";
        else if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.female)))
            return "(F)";
        else if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.transgender)))
            return "(T)";
        else if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.intersex)))
            return "(I)";
        else if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.non_Binary)))
            return "(NB)";
        else if (getGenderTextFromClick.equalsIgnoreCase(getResources().getString(R.string.not_preferred)))
            return "(NP)";
        else return "(O)";
    }

    private void setCursorVisibilityForEditTextUserName() {
        dialogAddUserBinding.editTextUserName.setOnFocusChangeListener((v, hasFocus) -> {
            dialogAddUserBinding.editTextUserName.setCursorVisible(hasFocus);
        });
    }

    private void setCursorVisibilityForEditTextPhoneNumber() {
        dialogAddUserBinding.editTextPhoneNo.setOnFocusChangeListener((v, hasFocus) -> {
            dialogAddUserBinding.editTextPhoneNo.setCursorVisible(hasFocus);
        });
    }

    private int calculateCustomHeight() {
        int[] location = new int[2];
        dialogAddUserBinding.countryCodePicker.getLocationOnScreen(location);
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        return screenHeight - (location[1] + dialogAddUserBinding.textInputLayoutPhoneNo.getHeight());
    }

    private void countryCodeSelection() {
        dialogAddUserBinding.countryCodePicker.setDialogEventsListener(new CountryCodePicker.DialogEventsListener() {
            @Override
            public void onCcpDialogOpen(Dialog dialog) {
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.width = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * .86);
                    window.setBackgroundDrawableResource(R.drawable.rounded_corner);
                    layoutParams.height = calculateCustomHeight();
                    layoutParams.gravity = Gravity.BOTTOM;
                    window.setAttributes(layoutParams);
                }
            }

            @Override
            public void onCcpDialogDismiss(DialogInterface dialogInterface) {

            }

            @Override
            public void onCcpDialogCancel(DialogInterface dialogInterface) {

            }
        });
    }
    private void genderSelection() {
        dialogAddUserBinding.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.gender_options_menu, popupMenu.getMenu());

            try {
                Field menuHelperField = PopupMenu.class.getDeclaredField("mPopup");
                menuHelperField.setAccessible(true);
                Object menuHelper = menuHelperField.get(popupMenu);
                if (menuHelper != null) {
                    Method setForceIcons = menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuHelper, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.option_transgender) {
                    getGenderTextFromClick = getResources().getString(R.string.transgender);
                }
                else if (item.getItemId() == R.id.option_intersex) {
                    getGenderTextFromClick = getResources().getString(R.string.intersex);
                }
                else if (item.getItemId() == R.id.option_non_binary) {
                    getGenderTextFromClick = getResources().getString(R.string.non_Binary);
                }
                else if (item.getItemId() == R.id.option_prefer_not_to_say) {
                    getGenderTextFromClick = getResources().getString(R.string.not_preferred);
                }
                else {
                    getGenderTextFromClick = getResources().getString(R.string.others);
                }
                return true;
            });
            popupMenu.show();
        });

        dialogAddUserBinding.btnFemale.setOnClickListener(v -> {
            getGenderTextFromClick = dialogAddUserBinding.btnFemale.getText().toString();
        });

        dialogAddUserBinding.btnMale.setOnClickListener(v -> {
            getGenderTextFromClick = dialogAddUserBinding.btnMale.getText().toString();
        });
    }
}
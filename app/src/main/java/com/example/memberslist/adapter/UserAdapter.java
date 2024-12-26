package com.example.memberslist.adapter;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.memberslist.MainActivity;
import com.example.memberslist.R;
import com.example.memberslist.database.User;
import com.example.memberslist.databinding.ActivityMainBinding;
import com.example.memberslist.databinding.DialogUserProfileBinding;
import com.example.memberslist.databinding.UserDetailsLayoutBinding;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users = new ArrayList<>();
    private final Context context;
    private OnDeleteClickListener onDeleteClickListener;
    private DialogUserProfileBinding userProfileBinding;
    private UserDetailsLayoutBinding userDetailsLayoutBinding;
    private final ActivityMainBinding activityMainBinding;
    private final Dialog userProfileDialog, userDetailsDialog;
    private AlertDialog addUserDetailsDialog, deleteDialog;
    private final WindowManager.LayoutParams params;
    private final DisplayMetrics displayMetrics;
    private int width, height;
    private boolean isSelectionMode = false;
    private final List<User> selectedUsers = new ArrayList<>();

    public UserAdapter(Context context, ActivityMainBinding mBinding) {
        this.context = context;
        this.activityMainBinding = mBinding;
        userProfileDialog = new Dialog(context);
        userDetailsDialog = new Dialog(context);
        params = new WindowManager.LayoutParams();
        displayMetrics = context.getResources().getDisplayMetrics();
    }

    public void setDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_items, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewGender.setText(user.getGender());
        holder.textViewDob.setText("DOB:" + user.getDob());
        holder.textViewAge.setText("Age:" + user.getAge());
        if (user.getPhotoUri() != null) {
            holder.photo.setImageURI(Uri.parse(user.getPhotoUri()));
            Glide.with(context)
                    .load(Uri.parse(user.getPhotoUri()))
                    .circleCrop()
                    .into(holder.photo);
        } else {
            holder.photo.setImageResource(R.drawable.ic_launcher_foreground);
        }
        holder.deleteUser.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(user);
            }
        });

        holder.photo.setOnClickListener(v -> showUserProfileDialog(user));
        holder.user_details.setOnClickListener(v -> {
            showUserDetailsLayout(user);
        });

        //Below code is for selection of users and checkbox.
        activityMainBinding.checkbox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        activityMainBinding.checkbox.setChecked(selectedUsers.size() == getItemCount());

        holder.itemView.setOnLongClickListener(v -> {
            selectedUsers.add(user);
            isSelectionMode = true;
            activityMainBinding.checkbox.setVisibility(View.VISIBLE);
            activityMainBinding.selectAll.setVisibility(View.VISIBLE);
            notifyDataSetChanged();
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedUsers.contains(user)) {
                    selectedUsers.remove(user);
                } else {
                    selectedUsers.add(user);
                }
                notifyDataSetChanged();
            }
        });

        activityMainBinding.checkbox.setOnClickListener(v -> {
            if (activityMainBinding.checkbox.isChecked()) {
                if (!selectedUsers.contains(user))
                    selectedUsers.add(user);
                else
                    selectedUsers.remove(user);
                notifyDataSetChanged();
            }
            if (activityMainBinding.checkbox.isChecked() && getSelectedUsers().size() == getItemCount()) {
                activityMainBinding.checkbox.setChecked(false);
                clearAll();
                activityMainBinding.checkbox.setVisibility(View.GONE);
                activityMainBinding.selectAll.setVisibility(View.GONE);
            }
        });
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    public void selectAll() {
        selectedUsers.clear();
        selectedUsers.addAll(users);
    }

    public void clearAll() {
        selectedUsers.clear();
        isSelectionMode = false;
    }

    private void showUserProfileDialog(User user) {
        userProfileBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_user_profile,
                null,
                false
        );
        userProfileDialog.setContentView(userProfileBinding.getRoot());
        userProfileBinding.setUser(user);

        //Below lines are not working have to look.
        /*if (addUserDetailsDialog != null && addUserDetailsDialog.isShowing()) {
            return;
        }
        if (deleteDialog != null && deleteDialog.isShowing()) {
            return;
        }*/
        if (!userProfileDialog.isShowing() && !userDetailsDialog.isShowing()) {
            userProfileDialog.show();
        }
    }

    private void showUserDetailsLayout(User user) {
        userDetailsLayoutBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.user_details_layout,
                null,
                false
        );
        userDetailsDialog.setContentView(userDetailsLayoutBinding.getRoot());
        userDetailsLayoutBinding.setUser(user);
        Window window = userDetailsDialog.getWindow();
        if (window != null) {
            params.width = width = (int) (displayMetrics.widthPixels * .9);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setLayout(params.width, params.height);
            window.setBackgroundDrawableResource(R.drawable.rounded_corner);
        }

        //Below lines are not working have to look.
        /*if (addUserDetailsDialog != null && addUserDetailsDialog.isShowing()) {
            return;
        }
        if (deleteDialog != null && deleteDialog.isShowing()) {
            return;
        }*/
        if (!userDetailsDialog.isShowing() && !userProfileDialog.isShowing()) {
            userDetailsDialog.show();
        }

        //Tried to use in this in onBindViewHolder, but caused attempting to access the editUser field
        // of UserDetailsLayoutBinding, which is null in your onBindViewHolder method. So used it here.
        userDetailsLayoutBinding.editUser.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showUserDetailsDialog(user, userDetailsDialog);
            }
        });
    }

    //If the count is 0 it won't display in the list at all.
    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName, textViewAge, textViewGender, textViewDob;
        private final ImageView photo, deleteUser;
        private final ConstraintLayout user_details;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.list_item);
            textViewAge = itemView.findViewById(R.id.age);
            textViewDob = itemView.findViewById(R.id.dob);
            textViewGender = itemView.findViewById(R.id.gender);
            photo = itemView.findViewById(R.id.photoImageView);
            deleteUser = itemView.findViewById(R.id.delete_user);
            user_details = itemView.findViewById(R.id.user_details);
        }
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(User user);
    }
}

package com.example.memberslist.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Parcelable;
import android.util.DisplayMetrics;
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
import com.example.memberslist.databinding.UserItemsBinding;
import com.example.memberslist.models.UserViewModel;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users = new ArrayList<>();
    private final Context context;
    private OnDeleteClickListener onDeleteClickListener;
    public DialogUserProfileBinding userProfileBinding;
    public UserDetailsLayoutBinding userDetailsLayoutBinding;
    private final ActivityMainBinding activityMainBinding;
    private final Dialog userProfileDialog, userDetailsDialog;
    private final AlertDialog addUserDetailsDialog;
    private final WindowManager.LayoutParams params;
    private final DisplayMetrics displayMetrics;
    private final UserViewModel userViewModel;
    private int width, height;
    private boolean isSelectionMode = false;
    private final List<User> selectedUsers = new ArrayList<>();
    private GradientDrawable gradientDrawable;
    private UserItemsBinding userItemsBinding;

    public UserAdapter(Context context, ActivityMainBinding mBinding, AlertDialog addUserDetailsDialog, UserViewModel userViewModel) {
        this.context = context;
        this.activityMainBinding = mBinding;
        this.addUserDetailsDialog = addUserDetailsDialog;
        this.userViewModel = userViewModel;
        userProfileDialog = new Dialog(context);
        userDetailsDialog = new Dialog(context);
        params = new WindowManager.LayoutParams();
        displayMetrics = context.getResources().getDisplayMetrics();
        gradientDrawable = new GradientDrawable();

        userProfileBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_user_profile,
                null,
                false
        );
        userProfileDialog.setContentView(userProfileBinding.getRoot());

        userDetailsLayoutBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.user_details_layout,
                null,
                false
        );
        userDetailsDialog.setContentView(userDetailsLayoutBinding.getRoot());

        Window window = userDetailsDialog.getWindow();
        if (window != null) {
            params.width = width = (int) (displayMetrics.widthPixels * .9);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setLayout(params.width, params.height);
            window.setBackgroundDrawableResource(R.drawable.rounded_corner);
        }

        if (userViewModel.getUserDetailsDialog().getValue() != null) {
            showUserDetailsLayout(userViewModel.getUserDetailsDialog().getValue());
        }

        userItemsBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.user_items, null, false);
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
        String Age = context.getText(R.string.age) + context.getString(R.string.colon) + user.getAge();
        String Dob = context.getText(R.string.DOB) + context.getString(R.string.colon) + user.getDob();
        holder.textViewName.setText(user.getName());
        holder.textViewGender.setText(user.getGender());
        holder.textViewDob.setText(Dob);
        holder.textViewAge.setText(Age);
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
        holder.user_details.setOnClickListener(v -> showUserDetailsLayout(user));

        //Below code is for selection of users and checkbox.
        holder.itemView.setBackgroundResource(selectedUsers.contains(user) ? R.drawable.selected_users_color : R.drawable.rounded_corner);
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                selectedUsers.add(user);
                notifyDataSetChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedUsers.contains(user)) {
                    selectedUsers.remove(user);
                    if (selectedUsers.isEmpty())
                        isSelectionMode = false;
                } else {
                    selectedUsers.add(user);
                }
                notifyDataSetChanged();
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
        userProfileBinding.setUser(user);

        if (addUserDetailsDialog != null && addUserDetailsDialog.isShowing()) {
            return;
        }

        if (((MainActivity) context).getDeleteDialog() != null && ((MainActivity) context).getDeleteDialog().isShowing()) {
            return;
        }

        if (!userProfileDialog.isShowing() && !userDetailsDialog.isShowing()) {
            userProfileDialog.show();
            userViewModel.setUserProfileDialog(user);
        }

        // This was not working here so moved it to mainActivity.
        /*userProfileDialog.setOnDismissListener(dialog -> {
            userViewModel.setUserProfileDialog(null);
        });*/
    }

    private void showUserDetailsLayout(User user) {
        userDetailsLayoutBinding.setUser(user);

        if (addUserDetailsDialog != null && addUserDetailsDialog.isShowing()) {
            return;
        }

        if (((MainActivity) context).getDeleteDialog() != null && ((MainActivity) context).getDeleteDialog().isShowing()) {
            return;
        }

        if (user.getPhoneNumber().isEmpty()) userDetailsLayoutBinding.guideline3.setGuidelinePercent(0.55F);
        else {
            userDetailsLayoutBinding.guideline3.setGuidelinePercent(0.33F);
            userDetailsLayoutBinding.guideline4.setGuidelinePercent(0.66F);
            gradientDrawable.setColor(context.getResources().getColor(R.color.sky_blue, null));
            gradientDrawable.setStroke(6, context.getColor(R.color.blue));
            gradientDrawable.setCornerRadius(20 * displayMetrics.density);
            userDetailsLayoutBinding.userPhoneVal.setBackground(gradientDrawable);
        }
        userDetailsLayoutBinding.userPhoneKey.setVisibility(user.getPhoneNumber().isEmpty() ? View.GONE : View.VISIBLE);
        userDetailsLayoutBinding.userPhoneVal.setVisibility(user.getPhoneNumber().isEmpty() ? View.GONE : View.VISIBLE);
        if (!userDetailsDialog.isShowing() && !userProfileDialog.isShowing()) {
            userDetailsDialog.show();
            userViewModel.setUserDetailsDialog(user);
        }

        // This was not working here so moved it to mainActivity.
        /*userDetailsDialog.setOnDismissListener(dialog -> {
            userViewModel.setUserDetailsDialog(null);
        });*/

        //Tried to use in this in onBindViewHolder, but caused attempting to access the editUser field
        // of UserDetailsLayoutBinding, which is null in your onBindViewHolder method. So used it here.
        userDetailsLayoutBinding.editUser.setOnClickListener(v -> {
            ((MainActivity) context).showUserDetailsDialog(user, userDetailsDialog, userDetailsLayoutBinding.editUser);
            userViewModel.setEditUserDialog(user);
            userDetailsDialog.dismiss();
        });

        userDetailsLayoutBinding.userPhoneVal.setOnClickListener(v -> {
            if (!userDetailsLayoutBinding.userPhoneVal.getText().toString().isEmpty()) {
                showPhoneNumberOptions(user.getPhoneNumber());
            }
        });

    }

    public Dialog getUserProfileDialog() {
        return userProfileDialog;
    }

    public Dialog getUserDetailsDialog() {
        return userDetailsDialog;
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

    public User deleteUserAtPosition(int position) {
        if (position >= 0 && position < users.size()) {
            User user = users.get(position);
            users.remove(position);
            notifyItemRemoved(position);

            //Notify the viewmodel to delete user from DB.
            userViewModel.delete(user);
            return user;
        }
        return null;
    }

    public void addUserAtPosition(User addDeletedUser, int position) {
        users.add(position, addDeletedUser);
        notifyItemInserted(position);
        userViewModel.insert(addDeletedUser);
    }

    private void showPhoneNumberOptions(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" +phoneNumber));

        Intent messageIntent = new Intent(Intent.ACTION_VIEW);
        messageIntent.setData(Uri.parse("sms:" + phoneNumber));

        Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
        whatsappIntent.setData(Uri.parse("https://wa.me/" + phoneNumber));

        Intent telegramIntent = new Intent(Intent.ACTION_VIEW);
        telegramIntent.setData(Uri.parse("tg://resolve?domain=" + phoneNumber));

        // List of intents for different communication apps
        List<Intent> intentList = new ArrayList<>();
        intentList.add(callIntent);
        intentList.add(messageIntent);
        intentList.add(whatsappIntent);
        intentList.add(telegramIntent);

        // Use Intent.createChooser to show all available apps
        Intent chooserIntent = Intent.createChooser(intentList.get(0), "Select Communication App");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[0]));
        context.startActivity(chooserIntent);
    }
}

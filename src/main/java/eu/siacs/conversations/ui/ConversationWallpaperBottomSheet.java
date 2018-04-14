package eu.siacs.conversations.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.List;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.databinding.FragmentConversationWallpaperBottomSheetBinding;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.persistance.FileBackend;
import eu.siacs.conversations.ui.util.AttachmentTool;
import eu.siacs.conversations.utils.FileUtils;

public class ConversationWallpaperBottomSheet extends BottomSheetDialogFragment {

    private final int REQUEST_CHOOSE_WALLPAPER = 0x0308;
    private final int REQUEST_CHOOSE_COLOR = 0x0309;
    private OnWallpaperActionClicked mListener;
    private ConversationsActivity activity;
    private Conversation conversation;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentConversationWallpaperBottomSheetBinding binding = DataBindingUtil.inflate(activity.getLayoutInflater(), R.layout.fragment_conversation_wallpaper_bottom_sheet, null, false);
        binding.addWallpaper.setOnClickListener(view -> addWallpaper());
        binding.addSolidColor.setOnClickListener(v -> startActivityForResult(new Intent(activity, SolidColorActivity.class), REQUEST_CHOOSE_COLOR));
        binding.removeWallpaper.setOnClickListener(view -> {
            removeWallpaper();
            if (mListener != null) {
                mListener.onWallpaperActionCompleted();
                dismiss();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CHOOSE_COLOR:
                    int color = data.getIntExtra("color", 0);
                    if (activity != null && conversation != null && color != 0) {
                        activity.xmppConnectionService.getFileBackend().removeConversationWallpaper(conversation.getUuid());
                        conversation.setHasWallpaper(false);
                        conversation.setColor(color);
                        activity.xmppConnectionService.updateConversation(conversation);
                        if (mListener != null) {
                            mListener.onWallpaperActionCompleted();
                            dismiss();
                        }
                    }
                    break;
                case REQUEST_CHOOSE_WALLPAPER:
                    List<Uri> wallpaperUris = AttachmentTool.extractUriFromIntent(data);
                    cropWallpaper(wallpaperUris.get(0));
                    break;
                case Crop.REQUEST_CROP:
                    if (activity == null || conversation == null) {
                        return;
                    }
                    int[] chatWallpaperDimensions = activity.getChatWallpaperDimensions();
                    int width = chatWallpaperDimensions[0];
                    int height = chatWallpaperDimensions[1];
                    File cachedFile = new File(activity.getCacheDir(), "conversationWallpaper");
                    Uri wallpaperUri = Uri.fromFile(cachedFile);
                    activity.xmppConnectionService.publishWallpaper(conversation, wallpaperUri, width, height, new UiCallback<String>() {
                        @Override
                        public void success(String object) {
                            cachedFile.delete();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    if (mListener != null) {
                                        mListener.onWallpaperActionCompleted();
                                        dismiss();
                                    }
                                });
                            }
                        }

                        @Override
                        public void error(int errorCode, String object) {
                            cachedFile.delete();
                            if (activity != null) {
                                activity.runOnUiThread(() -> Toast.makeText(activity, errorCode, Toast.LENGTH_SHORT).show());
                            }
                        }

                        @Override
                        public void userInputRequried(PendingIntent pi, String object) {
                        }
                    });
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (ConversationFragment.allGranted(grantResults)) {
                addWallpaper();
            }
        }
    }

    private boolean hasStoragePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private void cropWallpaper(Uri uri) {
        if (conversation == null || activity == null) {
            return;
        }
        if (FileBackend.weOwnFile(getActivity(), uri)) {
            Toast.makeText(getActivity(), R.string.security_error_invalid_file_access, Toast.LENGTH_SHORT).show();
            return;
        }
        int[] chatWallpaperDimensions = activity.getChatWallpaperDimensions();
        int width = chatWallpaperDimensions[0];
        int height = chatWallpaperDimensions[1];
        String original = FileUtils.getPath(activity, uri);
        if (original != null) {
            uri = Uri.parse("file://" + original);
        }
        Uri destination = Uri.fromFile(new File(activity.getCacheDir(), "conversationWallpaper"));
        Crop.of(uri, destination).withAspect(width, height).withMaxSize(width, height).start(activity, this);
    }

    private void addWallpaper() {
        if (!Config.ONLY_INTERNAL_STORAGE && !hasStoragePermission(REQUEST_CHOOSE_WALLPAPER)) {
            return;
        }
        Intent attachFileIntent = new Intent();
        attachFileIntent.setType("image/*");
        attachFileIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent chooser = Intent.createChooser(attachFileIntent, getString(R.string.wallpaper));
        startActivityForResult(chooser, REQUEST_CHOOSE_WALLPAPER);
    }

    private void removeWallpaper() {
        if (activity != null && conversation != null) {
            activity.xmppConnectionService.getFileBackend().removeConversationWallpaper(conversation.getUuid());
            conversation.setHasWallpaper(false);
            conversation.setColor(0);
            activity.xmppConnectionService.updateConversation(conversation);
        }
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public void setWallpaperActionListener(ConversationsActivity activity, OnWallpaperActionClicked mListener) {
        this.activity = activity;
        this.mListener = mListener;
    }

    public interface OnWallpaperActionClicked {

        void onWallpaperActionCompleted();
    }
}

package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private boolean hasUpdate = false;

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.website_button);
        Button mDiscordButton = view.findViewById(R.id.discord_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);
        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.discord_invite)));
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        mShareLogsButton.setOnClickListener(v -> shareLog(requireContext()));
        mOpenDirectoryButton.setOnClickListener(v -> {
            Tools.switchDemo(Tools.isDemoProfile(v.getContext()));
            openPath(v.getContext(), getCurrentProfileDirectory(), false);
        });

        mNewsButton.setOnLongClickListener(v -> {
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });

        // Appel asynchrone à isNewUpdate()
        isNewUpdate(isUpdate -> {
            requireActivity().runOnUiThread(() -> {
                if (LauncherProfiles.mainProfileJson.profiles.isEmpty() || isUpdate) {
                    mPlayButton.setText(R.string.update);
                    mPlayButton.setOnClickListener(v ->
                            Tools.swapFragment((FragmentActivity) getContext(),
                                    SearchModFragment.class,
                                    SearchModFragment.TAG, null)
                    );
                } else {
                    mPlayButton.setText(R.string.main_play);
                    mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
                }
            });
        });
    }


    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if(!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if(profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
    }

    public void isNewUpdate(Consumer<Boolean> callback) {
        ModpackApi modpackApi = new CommonApi(getString(R.string.curseforge_api_key));

        SearchFilters filters = new SearchFilters();
        filters.isModpack = true;
        filters.name = "Pokefree";

        SearchResult result = modpackApi.searchMod(filters);
        if (result == null || result.totalResultCount == 0) {
            callback.accept(false);
            return;
        }

        new SelfReferencingFuture(myFuture -> {
            ModDetail modDetail = modpackApi.getModDetails(result.results[0]);
            String latestVersion = modDetail.versionNames[0];
            String localVersion = getInstalledModVersion();
            System.out.println("dif version : " + latestVersion + "----" + localVersion);
            boolean isUpdate = !latestVersion.equals(localVersion);
            callback.accept(isUpdate);
        }).startOnExecutor(PojavApplication.sExecutorService);
    }

    private String getInstalledModVersion() {
        File versionFile = new File(Tools.DIR_GAME_HOME, "pokefree-version.txt");
        if (versionFile.exists()) {
            try {
                return new BufferedReader(new FileReader(versionFile)).readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "unknown";
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}

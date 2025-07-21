package net.kdt.pojavlaunch.modloaders.modpacks.api;


import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.fragments.SearchModFragment;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;

import java.io.IOException;
import java.util.HashMap;

/**
 *
 */
public interface ModpackApi {

    /**
     * @param searchFilters Filters
     * @param previousPageResult The result from the previous page
     * @return the list of mod items from specified offset
     */
    SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult);

    /**
     * @param searchFilters Filters
     * @return A list of mod items
     */
    default SearchResult searchMod(SearchFilters searchFilters) {
        return searchMod(searchFilters, null);
    }

    /**
     * Fetch the mod details
     * @param item The moditem that was selected
     * @return Detailed data about a mod(pack)
     */
    ModDetail getModDetails(ModItem item);

    /**
     * Download and install the mod(pack)
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    default void handleInstallation(Context context, ModDetail modDetail, int selectedVersion, Runnable onFinish, Runnable OnBeginning, Runnable OnError) {
        // Indiquer un début d'installation
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                if (OnBeginning != null) {
                    Tools.runOnUiThread(OnBeginning);
                }
                LauncherProfiles.mainProfileJson.profiles = new HashMap<>();
                LauncherProfiles.write();
                ModLoader loaderInfo = installMod(modDetail, selectedVersion);

                if (loaderInfo == null) {
                    return;
                }
                loaderInfo.getDownloadTask(new NotificationDownloadListener(context, loaderInfo)).run();
            } catch (IOException e) {
                if (OnError != null) {
                    Tools.runOnUiThread(OnError);
                }

                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            } finally {
                // Appel du callback à la fin, sur le thread UI
                if (onFinish != null) {
                    Tools.runOnUiThread(onFinish);
                }
            }
        });
    }

    /**
     * Install the mod(pack).
     * May require the download of additional files.
     * May requires launching the installation of a modloader
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    ModLoader installMod(ModDetail modDetail, int selectedVersion) throws IOException;
}

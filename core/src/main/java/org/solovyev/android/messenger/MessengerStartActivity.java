package org.solovyev.android.messenger;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.solovyev.android.messenger.api.MessengerAsyncTask;
import org.solovyev.android.messenger.realms.ConfiguredRealm;
import org.solovyev.android.messenger.realms.RealmService;
import org.solovyev.android.messenger.security.AuthService;
import org.solovyev.android.messenger.security.UserIsNotLoggedInException;
import org.solovyev.android.messenger.sync.SyncAllTaskIsAlreadyRunning;
import org.solovyev.android.messenger.sync.SyncService;
import org.solovyev.android.messenger.users.User;
import roboguice.activity.RoboActivity;

import java.util.Collection;
import java.util.List;

/**
 * User: serso
 * Date: 5/24/12
 * Time: 9:47 PM
 */
public class MessengerStartActivity extends RoboActivity {

    @Inject
    @NotNull
    private AuthService authService;

    @Inject
    @NotNull
    private RealmService realmService;

    @Inject
    @NotNull
    private SyncService syncService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Collection<ConfiguredRealm> configuredRealms = realmService.getConfiguredRealms();
        if (configuredRealms.isEmpty()) {

            boolean atLeastOneRealmLogged = false;

            for (ConfiguredRealm configuredRealm : configuredRealms) {
                try {
                    final User user = authService.getUser(configuredRealm.getRealm().getId(), this);

                    if (!user.getUserSyncData().isFirstSyncDone()) {
                        // user is logged first time => sync all data
                        try {
                            syncService.syncAll(this);
                        } catch (SyncAllTaskIsAlreadyRunning syncAllTaskIsAlreadyRunning) {
                            // do not care
                        }
                    } else {
                        // prefetch data
                        new PreloadCachedData(this).execute(user);
                    }

                    atLeastOneRealmLogged = true;
                } catch (UserIsNotLoggedInException e) {
                    Log.e(MessengerStartActivity.class.getSimpleName(), e.getMessage(), e);
                }
            }


            if (atLeastOneRealmLogged) {
                MessengerMainActivity.startActivity(this);
            } else {
                MessengerRealmsActivity.startActivity(this);
            }
        } else {
            MessengerRealmsActivity.startActivity(this);
        }

        this.finish();
    }

    private static final class PreloadCachedData extends MessengerAsyncTask<User, Void, Void> {

        private PreloadCachedData(@NotNull Context context) {
            super(context);
        }

        @Override
        protected Void doWork(@NotNull List<User> users) {
            Context context = getContext();
            if (context != null) {
                for (User user : users) {
                    AbstractMessengerApplication.getServiceLocator().getUserService().getUserContacts(user.getId(), context);
                    AbstractMessengerApplication.getServiceLocator().getUserService().getUserChats(user.getId(), context);
                }
            }

            return null;
        }

        @Override
        protected void onSuccessPostExecute(@Nullable Void result) {

        }
    }
}

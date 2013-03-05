package org.solovyev.android.messenger.realms;

import android.content.Context;
import org.solovyev.android.messenger.RealmConnection;
import org.solovyev.android.messenger.chats.RealmChatService;
import org.solovyev.android.messenger.users.RealmUserService;
import org.solovyev.android.messenger.users.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * User: serso
 * Date: 7/22/12
 * Time: 12:56 AM
 */
public interface RealmDef {

    @Nonnull
    String FAKE_REALM_ID = "fake";

    // realm's identifier. Must be unique for all existed realms
    @Nonnull
    String getId();

    int getNameResId();

    int getIconResId();

    void init(@Nonnull Context context);

    @Nonnull
    RealmUserService newRealmUserService(@Nonnull Realm realm);

    @Nonnull
    RealmChatService newRealmChatService(@Nonnull Realm realm);

    @Nonnull
    RealmConnection newRealmConnection(@Nonnull Realm realm, @Nonnull Context context);

    @Nonnull
    Class<? extends BaseRealmConfigurationFragment> getConfigurationFragmentClass();

    @Nonnull
    Realm newRealm(@Nonnull String realmId, @Nonnull User user, @Nonnull RealmConfiguration configuration);

    @Nonnull
    Class<? extends RealmConfiguration> getConfigurationClass();

    @Nonnull
    RealmBuilder newRealmBuilder(@Nonnull RealmConfiguration configuration, @Nullable Realm editedRealm);

    boolean equals(@Nullable Object o);

    int hashCode();
}
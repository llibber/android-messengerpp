package org.solovyev.android.messenger.users;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.solovyev.android.messenger.AbstractMessengerEntity;
import org.solovyev.android.messenger.entities.Entity;
import org.solovyev.android.properties.AProperty;
import org.solovyev.android.properties.Properties;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: serso
 * Date: 5/24/12
 * Time: 10:30 PM
 */
final class UserImpl extends AbstractMessengerEntity implements User {

    @Nonnull
    private String login;

    @Nonnull
    private UserSyncData userSyncData;

    // todo serso: use AProperties object
    @Nonnull
    private List<AProperty> properties = new ArrayList<AProperty>();

    @Nonnull
    private Map<String, String> propertiesMap = new HashMap<String, String>();

    @Nullable
    private String displayName;

    UserImpl(@Nonnull Entity entity) {
        super(entity);
    }

    @Nonnull
    static User newInstance(@Nonnull Entity entity,
                                   @Nonnull UserSyncData userSyncData,
                                   @Nonnull List<AProperty> properties) {
        final UserImpl result = new UserImpl(entity);

        result.login = entity.getRealmEntityId();
        result.userSyncData = userSyncData;
        result.properties.addAll(properties);

        for (AProperty property : result.properties) {
            result.propertiesMap.put(property.getName(), property.getValue());
        }

        if ( result.propertiesMap.size() != result.properties.size() ) {
            // just in case...
            result.properties.clear();
            for (Map.Entry<String, String> entry : result.propertiesMap.entrySet()) {
                result.properties.add(Properties.newProperty(entry.getKey(), entry.getValue()));
            }
        }

        return result;
    }

    @Nonnull
    public String getLogin() {
        return login;
    }

    @Override
    public Gender getGender() {
        final String result = getPropertyValueByName(User.PROPERTY_SEX);
        return result == null ? null : Gender.valueOf(result);
    }

    @Override
    public boolean isOnline() {
        return Boolean.valueOf(getPropertyValueByName(PROPERTY_ONLINE));
    }

    @Override
    @Nonnull
    public UserSyncData getUserSyncData() {
        return userSyncData;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        if ( displayName == null ) {
            displayName = createDisplayName();
        }
        return displayName;
    }

    @Nonnull
    private String createDisplayName() {
        final StringBuilder result = new StringBuilder();

        final String firstName = this.getPropertyValueByName(User.PROPERTY_FIRST_NAME);
        final String lastName = this.getPropertyValueByName(User.PROPERTY_LAST_NAME);

        boolean firstNameExists = !Strings.isEmpty(firstName);
        boolean lastNameExists = !Strings.isEmpty(lastName);

        if (!firstNameExists && !lastNameExists) {
            // first and last names are empty
            result.append(this.getEntity().getRealmEntityId());
        } else {

            if (firstNameExists) {
                result.append(firstName);
            }

            if (firstNameExists && lastNameExists) {
                result.append(" ");
            }

            if (lastNameExists) {
                result.append(lastName);
            }
        }

        return result.toString();
    }

    @Nonnull
    @Override
    public User updateChatsSyncDate() {
        final UserImpl clone = this.clone();
        clone.userSyncData = clone.userSyncData.updateChatsSyncDate();
        return clone;
    }

    @Nonnull
    @Override
    public User updatePropertiesSyncDate() {
        final UserImpl clone = this.clone();
        clone.userSyncData = clone.userSyncData.updatePropertiesSyncDate();
        return clone;
    }

    @Nonnull
    @Override
    public User updateContactsSyncDate() {
        final UserImpl clone = this.clone();
        clone.userSyncData = clone.userSyncData.updateContactsSyncDate();
        return clone;
    }

    @Nonnull
    @Override
    public User updateUserIconsSyncDate() {
        final UserImpl clone = this.clone();
        clone.userSyncData = clone.userSyncData.updateUserIconsSyncDate();
        return clone;
    }

    @Override
    @Nonnull
    public List<AProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public String getPropertyValueByName(@Nonnull String name) {
        return this.propertiesMap.get(name);
    }

    @Nonnull
    @Override
    public UserImpl clone() {
        return (UserImpl) super.clone();
    }

    @Override
    public String toString() {
        return "UserImpl{" +
                "id=" + getEntity().getEntityId() +
                '}';
    }

    @Nonnull
    @Override
    public User cloneWithNewStatus(boolean online) {
        final UserImpl clone = clone();

        Iterables.removeIf(clone.properties, new Predicate<AProperty>() {
            @Override
            public boolean apply(@Nullable AProperty property) {
                return property != null && property.getName().equals(PROPERTY_ONLINE);
            }
        });
        clone.properties.add(Properties.newProperty(PROPERTY_ONLINE, Boolean.toString(online)));
        clone.propertiesMap.put(PROPERTY_ONLINE, Boolean.toString(online));

        return clone;
    }
}

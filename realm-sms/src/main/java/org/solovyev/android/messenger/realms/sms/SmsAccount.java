package org.solovyev.android.messenger.realms.sms;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.solovyev.android.messenger.accounts.connection.AccountConnection;
import org.solovyev.android.messenger.chats.AccountChatService;
import org.solovyev.android.messenger.accounts.AbstractAccount;
import org.solovyev.android.messenger.accounts.AccountState;
import org.solovyev.android.messenger.realms.Realm;
import org.solovyev.android.messenger.users.CompositeUserChoice;
import org.solovyev.android.messenger.users.AccountUserService;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.properties.Properties;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;

import com.google.common.base.Splitter;

/**
 * User: serso
 * Date: 5/27/13
 * Time: 8:43 PM
 */
final class SmsAccount extends AbstractAccount<SmsAccountConfiguration> {

	public SmsAccount(@Nonnull String id, @Nonnull Realm realm, @Nonnull User user, @Nonnull SmsAccountConfiguration configuration, @Nonnull AccountState state) {
		super(id, realm, user, configuration, state);
	}

	@Nonnull
	@Override
	protected AccountConnection newRealmConnection0(@Nonnull Context context) {
		return new SmsAccountConnection(this, context);
	}

	@Nonnull
	@Override
	public String getDisplayName(@Nonnull Context context) {
		return context.getString(getRealm().getNameResId());
	}

	@Nonnull
	@Override
	public AccountUserService getAccountUserService() {
		return new SmsAccountUserService(this);
	}

	@Nonnull
	@Override
	public AccountChatService getAccountChatService() {
		return new SmsAccountChatService();
	}

	@Override
	public boolean isCompositeUser(@Nonnull User user) {
		return true;
	}

	@Override
	public boolean isCompositeUserDefined(@Nonnull User user) {
		final String phoneNumber = user.getPropertyValueByName(User.PROPERTY_PHONE);
		return !Strings.isEmpty(phoneNumber);
	}

	@Nonnull
	@Override
	public List<CompositeUserChoice> getCompositeUserChoices(@Nonnull User user) {
		final String phoneNumbers = user.getPropertyValueByName(User.PROPERTY_PHONES);
		if (!Strings.isEmpty(phoneNumbers)) {
			final List<CompositeUserChoice> choices = new ArrayList<CompositeUserChoice>();

			int index = 0;
			for (String phoneNumber : Splitter.on(User.PROPERTY_PHONES_SEPARATOR).omitEmptyStrings().split(phoneNumbers)) {
				choices.add(CompositeUserChoice.newInstance(phoneNumber, index));
				index++;
			}

			return choices;
		} else {
			return Collections.emptyList();
		}
	}

	@Nonnull
	@Override
	public User applyCompositeChoice(@Nonnull CompositeUserChoice compositeUserChoice, @Nonnull User user) {
		return user.cloneWithNewProperty(Properties.newProperty(User.PROPERTY_PHONE, compositeUserChoice.getName().toString()));
	}

	@Override
	public boolean isCompositeUserChoicePersisted() {
		return true;
	}

	@Override
	public int getCompositeDialogTitleResId() {
		return R.string.mpp_sms_realm_composite_dialog_title;
	}
}

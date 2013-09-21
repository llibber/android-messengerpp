package org.solovyev.android.messenger.accounts;

import android.content.Context;
import org.solovyev.android.messenger.entities.Entity;
import org.solovyev.android.messenger.entities.EntityAware;
import org.solovyev.android.messenger.realms.*;
import org.solovyev.android.messenger.security.InvalidCredentialsException;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.properties.AProperty;
import org.solovyev.common.listeners.JEventListener;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * User: serso
 * Date: 7/22/12
 * Time: 12:57 AM
 */
public interface AccountService {

	@Nonnull
	static String TAG = "AccountService";

	/**
	 * Method initializes service, must be called once before any other operations with current service
	 */
	void init();

	/**
	 * Method restores service state (e.g. loads persistence data from database)
	 */
	void load();


	/**
	 * @return collection of all configured realms in application
	 */
	@Nonnull
	Collection<Realm<? extends AccountConfiguration>> getRealmDefs();

	@Nonnull
	Collection<Account> getAccounts();

	@Nonnull
	Collection<Account> getEnabledAccounts();

	/**
	 * @return collection of users in all configured accounts
	 */
	@Nonnull
	Collection<User> getAccountUsers();

	/**
	 * @return collection of users in all configured ENABLED accounts
	 */
	@Nonnull
	Collection<User> getEnabledAccountUsers();

	/**
	 * Method returns the realm which previously has been registered in this service
	 *
	 * @param realmDefId id of realm def
	 * @return realm
	 * @throws UnsupportedAccountException if realm hasn't been registered in this service
	 */
	@Nonnull
	Realm<? extends AccountConfiguration> getRealmDefById(@Nonnull String realmDefId) throws UnsupportedAccountException;

	@Nonnull
	Account getAccountById(@Nonnull String accountId) throws UnsupportedAccountException;

	@Nonnull
	Account getAccountByEntity(@Nonnull Entity entity) throws UnsupportedAccountException;

	@Nonnull
	Account getAccountByEntityAware(@Nonnull EntityAware entityAware) throws UnsupportedAccountException;

	@Nonnull
	Account saveAccount(@Nonnull AccountBuilder accountBuilder) throws InvalidCredentialsException, AccountAlreadyExistsException;

	@Nonnull
	Account changeAccountState(@Nonnull Account account, @Nonnull AccountState newState);

	void removeAccount(@Nonnull String accountId);

	boolean isOneAccount();

    /*
	**********************************************************************
    *
    *                           LISTENERS
    *
    **********************************************************************
    */

	void addListener(@Nonnull JEventListener<AccountEvent> listener);

	void removeListener(@Nonnull JEventListener<AccountEvent> listener);

	void stopAllRealmConnections();

	List<AProperty> getUserProperties(@Nonnull User user, @Nonnull Context context);
}

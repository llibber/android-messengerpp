package org.solovyev.android.messenger.realms;

import javax.annotation.Nonnull;

public final class AccountConnectionException extends AccountException {

	public AccountConnectionException(@Nonnull String realmId) {
		super(realmId);
	}

	public AccountConnectionException(@Nonnull String realmId, @Nonnull Throwable throwable) {
		super(realmId, throwable);
	}

	public AccountConnectionException(@Nonnull RealmRuntimeException exception) {
		super(exception);
	}
}

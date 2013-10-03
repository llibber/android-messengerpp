package org.solovyev.android.messenger;

import android.app.Application;

import javax.annotation.Nonnull;

import org.solovyev.android.messenger.realms.xmpp.XmppConfiguration;
import org.solovyev.android.messenger.realms.xmpp.XmppAccount;
import org.solovyev.android.messenger.realms.xmpp.XmppAccountBuilder;
import org.solovyev.android.messenger.realms.xmpp.XmppRealm;

import com.google.inject.Inject;

public abstract class XmppTestCase extends DefaultMessengerTestCase {

	@Nonnull
	@Inject
	private XmppRealm xmppRealm;

	@Nonnull
	private XmppAccount xmppAccount;

	@Nonnull
	@Override
	protected AbstractTestMessengerModule newModule(@Nonnull Application application) {
		return new XmppTestModule(application);
	}

	protected void populateDatabase() throws Exception {
		super.populateDatabase();
		xmppAccount = getAccountService().saveAccount(new XmppAccountBuilder(xmppRealm, null, XmppConfiguration.getInstance()));
	}

	@Nonnull
	public XmppRealm getXmppRealm() {
		return xmppRealm;
	}

	@Nonnull
	public XmppAccount getXmppAccount() {
		return xmppAccount;
	}
}
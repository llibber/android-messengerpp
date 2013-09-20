package org.solovyev.android.messenger.users;

import org.solovyev.android.Fragments2;
import org.solovyev.android.messenger.MessengerApplication;
import org.solovyev.android.messenger.MessengerFragmentActivity;
import org.solovyev.android.messenger.api.MessengerAsyncTask;
import org.solovyev.android.messenger.chats.Chat;
import org.solovyev.android.messenger.chats.ChatGuiEventType;
import org.solovyev.android.messenger.realms.Account;
import org.solovyev.android.messenger.realms.AccountException;
import org.solovyev.android.messenger.realms.AccountService;
import org.solovyev.android.messenger.realms.UnsupportedAccountException;

import roboguice.RoboGuice;
import roboguice.event.EventListener;
import roboguice.event.EventManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * User: serso
 * Date: 3/5/13
 * Time: 1:54 PM
 */
public final class ContactGuiEventListener implements EventListener<ContactGuiEvent> {

	@Nonnull
	private final MessengerFragmentActivity activity;

	@Nonnull
	private final AccountService accountService;

	public ContactGuiEventListener(@Nonnull MessengerFragmentActivity activity, @Nonnull AccountService accountService) {
		this.activity = activity;
		this.accountService = accountService;
	}

	@Override
	public void onEvent(@Nonnull ContactGuiEvent event) {
		final User contact = event.getContact();
		final ContactGuiEventType type = event.getType();

		try {
			final Account account = accountService.getAccountByEntityAware(contact);
			switch (type) {
				case contact_clicked:
					if (account.isCompositeUser(contact)) {
						if (!account.isCompositeUserDefined(contact)) {
							fireEvent(ContactGuiEventType.newShowCompositeUserDialog(contact));
						} else {
							fireEvent(ContactGuiEventType.newOpenContactChat(contact));
						}
					} else {
						fireEvent(ContactGuiEventType.newOpenContactChat(contact));
					}
					break;
				case open_contact_chat:
					onOpenContactChat(contact);
					break;
				case show_composite_user_dialog:
					Fragments2.showDialog(new CompositeUserDialogFragment(contact), CompositeUserDialogFragment.FRAGMENT_TAG, activity.getSupportFragmentManager());
					break;
			}
		} catch (UnsupportedAccountException e) {
			// should not happen
			MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
		}
	}

	private void fireEvent(@Nonnull ContactGuiEvent event) {
		final EventManager eventManager = RoboGuice.getInjector(activity).getInstance(EventManager.class);
		eventManager.fire(event);
	}

	private void onOpenContactChat(final User contact) {
		new MessengerAsyncTask<Void, Void, Chat>() {

			@Override
			protected Chat doWork(@Nonnull List<Void> params) {
				Chat result = null;

				try {
					final User user = activity.getAccountService().getAccountById(contact.getEntity().getRealmId()).getUser();
					result = MessengerApplication.getServiceLocator().getChatService().getPrivateChat(user.getEntity(), contact.getEntity());
				} catch (AccountException e) {
					throwException(e);
				}

				return result;
			}

			@Override
			protected void onSuccessPostExecute(@Nullable Chat chat) {
				if (chat != null) {
					activity.getEventManager().fire(ChatGuiEventType.chat_clicked.newEvent(chat));
				}
			}

		}.executeInParallel();
	}
}

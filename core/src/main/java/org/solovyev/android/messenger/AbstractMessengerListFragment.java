package org.solovyev.android.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockListFragment;
import com.google.inject.Inject;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.internal.LoadingLayout;
import org.solovyev.android.AThreads;
import org.solovyev.android.list.ListItem;
import org.solovyev.android.messenger.api.MessengerAsyncTask;
import org.solovyev.android.messenger.chats.ChatService;
import org.solovyev.android.messenger.fragments.FragmentGuiEventType;
import org.solovyev.android.messenger.realms.RealmService;
import org.solovyev.android.messenger.security.AuthServiceFacade;
import org.solovyev.android.messenger.sync.SyncService;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.messenger.users.UserEventListener;
import org.solovyev.android.messenger.users.UserEventType;
import org.solovyev.android.messenger.users.UserService;
import org.solovyev.android.messenger.view.PublicPullToRefreshListView;
import org.solovyev.android.view.ListViewAwareOnRefreshListener;
import org.solovyev.android.view.OnRefreshListener2Adapter;
import roboguice.event.EventManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: serso
 * Date: 6/7/12
 * Time: 5:57 PM
 */
public abstract class AbstractMessengerListFragment<T, LI extends ListItem> extends RoboSherlockListFragment implements AbsListView.OnScrollListener, ListViewFilter.FilterableListView {

    /*
    **********************************************************************
    *
    *                           CONSTANTS
    *
    **********************************************************************
    */

    @Nonnull
    private static final String POSITION = "position";

    /**
     * Constants are copied from list fragment, see {@link android.support.v4.app.ListFragment}
     */
    private static final int INTERNAL_EMPTY_ID = 0x00ff0001;

    private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;

    private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    /*
    **********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */


    @Inject
    @Nonnull
    private RealmService realmService;

    @Inject
    @Nonnull
    private UserService userService;

    @Inject
    @Nonnull
    private ChatService chatService;

    @Inject
    @Nonnull
    private AuthServiceFacade authServiceFacade;

    @Inject
    @Nonnull
    private SyncService syncService;

    @Inject
    @Nonnull
    private MessengerMultiPaneManager multiPaneManager;

    @Inject
    @Nonnull
    private EventManager eventManager;

    /*
    **********************************************************************
    *
    *                           OWN FIELDS
    *
    **********************************************************************
    */

    @Nullable
    private UserEventListener userEventListener;

    private MessengerListItemAdapter<LI> adapter;

    @Nullable
    private MessengerAsyncTask<Void, Void, List<T>> listLoader;

    /**
     * Filter for list view, null if filter is disabled for current list fragment
     */
    @Nullable
    private final ListViewFilter listViewFilter;

    @Nonnull
    private final String tag;

    @Nullable
    private PublicPullToRefreshListView pullToRefreshListView;

    /**
     * Mode which is used for {@link PullToRefreshListView}.
     * Note: null if simple {@link ListView} is used instead of {@link PullToRefreshListView}.
     */
    @Nullable
    private PullToRefreshBase.Mode pullToRefreshMode;


    /**
     * First visible item in list view. The value is changed due when list view is scrolled.
     * Main purpose: to fire events like {@link AbstractMessengerListFragment#onListViewTopReached()}, {@link AbstractMessengerListFragment#onListViewBottomReached()}, etc
     */
    @Nonnull
    private final AtomicInteger firstVisibleItem = new AtomicInteger(-1);

    /**
     * If nothing selected - first list item will be selected
     */
    private boolean selectFirstItemByDefault = true;

    /*
    **********************************************************************
    *
    *                           CONSTRUCTORS
    *
    **********************************************************************
    */

    public AbstractMessengerListFragment(@Nonnull String tag, boolean filterEnabled) {
        this.tag = tag;
        if ( filterEnabled ) {
            this.listViewFilter = new ListViewFilter(this, this);
        } else {
            this.listViewFilter = null;
        }
    }

    /*
    **********************************************************************
    *
    *                           GETTERS
    *
    **********************************************************************
    */

    @Nonnull
    protected UserService getUserService() {
        return userService;
    }

    @Nonnull
    protected AuthServiceFacade getAuthServiceFacade() {
        return authServiceFacade;
    }

    @Nonnull
    protected SyncService getSyncService() {
        return syncService;
    }

    @Nonnull
    protected ChatService getChatService() {
        return chatService;
    }

    @Nonnull
    protected RealmService getRealmService() {
        return realmService;
    }

    @Nonnull
    protected MessengerListItemAdapter getAdapter() {
        return adapter;
    }

    @Nonnull
    protected EventManager getEventManager() {
        return eventManager;
    }

    @Nonnull
    protected MessengerMultiPaneManager getMultiPaneManager() {
        return multiPaneManager;
    }

    @Nonnull
    protected ListView getListView(@Nonnull View root) {
        return (ListView) root.findViewById(android.R.id.list);
    }

    @Nullable
    public PublicPullToRefreshListView getPullToRefreshListView() {
        return pullToRefreshListView;
    }

    /*
    **********************************************************************
    *
    *                           LIFECYCLE
    *
    **********************************************************************
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "onCreate: " + this);

        eventManager.fire(FragmentGuiEventType.created.newEvent(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(tag, "onCreateView: " + this);

        final LinearLayout root = new LinearLayout(this.getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        if (listViewFilter != null) {
            final View filterView = listViewFilter.createView();
            root.addView(filterView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        final View listViewParent = createListView(container);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        root.addView(listViewParent, params);


        multiPaneManager.fillContentPane(getActivity(), container, root);

        return root;
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        if (listViewFilter != null) {
            listViewFilter.loadState(savedInstanceState);
        }

        eventManager.fire(FragmentGuiEventType.shown.newEvent(this));
    }

    public void toggleFilterBox() {
        if (listViewFilter != null) {
            listViewFilter.toggleView();
        }
    }

    private View createListView(ViewGroup container) {
        final Context context = getActivity();

        final FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        final LinearLayout progressContainer = new LinearLayout(context);
        progressContainer.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setVisibility(View.GONE);
        progressContainer.setGravity(Gravity.CENTER);

        final ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        progressContainer.addView(progress, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(progressContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        final FrameLayout listViewContainer = new FrameLayout(context);
        listViewContainer.setId(INTERNAL_LIST_CONTAINER_ID);

        final TextView emptyListCaption = new TextView(context);
        emptyListCaption.setId(INTERNAL_EMPTY_ID);
        emptyListCaption.setGravity(Gravity.CENTER);
        listViewContainer.addView(emptyListCaption, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));


        final ListViewAwareOnRefreshListener topRefreshListener = getTopPullRefreshListener();
        final ListViewAwareOnRefreshListener bottomRefreshListener = getBottomPullRefreshListener();

        final View listView;

        final Resources resources = context.getResources();
        if (topRefreshListener == null && bottomRefreshListener == null) {
            pullToRefreshMode = null;
            listView = new ListView(context);
            fillListView((ListView) listView, context);
            listView.setId(android.R.id.list);
        } else if (topRefreshListener != null && bottomRefreshListener != null) {
            pullToRefreshMode = PullToRefreshBase.Mode.BOTH;
            pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

            topRefreshListener.setListView(pullToRefreshListView);
            bottomRefreshListener.setListView(pullToRefreshListView);

            fillListView(pullToRefreshListView.getRefreshableView(), context);
            pullToRefreshListView.setShowIndicator(false);
            prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView(), container);
            prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView(), container);

            pullToRefreshListView.setOnRefreshListener(new OnRefreshListener2Adapter(topRefreshListener, bottomRefreshListener));
            listView = pullToRefreshListView;
        } else if (topRefreshListener != null) {
            pullToRefreshMode = PullToRefreshBase.Mode.PULL_DOWN_TO_REFRESH;
            pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

            topRefreshListener.setListView(pullToRefreshListView);

            fillListView(pullToRefreshListView.getRefreshableView(), context);

            pullToRefreshListView.setShowIndicator(false);
            prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView(), container);
            prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView(), container);

            pullToRefreshListView.setOnRefreshListener(topRefreshListener);

            listView = pullToRefreshListView;
        } else {
            pullToRefreshMode = PullToRefreshBase.Mode.PULL_UP_TO_REFRESH;
            pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

            bottomRefreshListener.setListView(pullToRefreshListView);

            fillListView(pullToRefreshListView.getRefreshableView(), context);
            pullToRefreshListView.setShowIndicator(false);
            prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView(), container);
            prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView(), container);

            pullToRefreshListView.setOnRefreshListener(bottomRefreshListener);

            listView = pullToRefreshListView;
        }

        listViewContainer.addView(listView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        root.addView(listViewContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return root;
    }

    protected void fillListView(@Nonnull ListView lv, @Nonnull Context context) {
        lv.setScrollbarFadingEnabled(true);
        lv.setCacheColorHint(Color.TRANSPARENT);
        lv.setOnScrollListener(this);
        lv.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
        lv.setDividerHeight(1);
    }

    private void prepareLoadingView(@Nonnull Resources resources, @Nullable LoadingLayout loadingView, @Nullable ViewGroup paneParent) {
        if (loadingView != null) {
            multiPaneManager.fillLoadingLayout(this.getActivity(), paneParent, resources, loadingView);
        }
    }

    @Nullable
    protected abstract ListViewAwareOnRefreshListener getTopPullRefreshListener();

    @Nullable
    protected abstract ListViewAwareOnRefreshListener getBottomPullRefreshListener();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(tag, "onActivityCreated: " + this);

        userEventListener = new UiThreadUserEventListener();
        userService.addListener(userEventListener);

        final ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setVerticalFadingEdgeEnabled(false);
        lv.setOnItemClickListener(new ListViewOnItemClickListener());
        lv.setOnItemLongClickListener(new ListViewOnItemLongClickListener());

        final int position;
        if ( adapter != null ) {
            // adapter not null => this fragment has been created earlier (and now it just goes to the shown state)
            position = adapter.getSelectedItemPosition();
        } else {
            if (savedInstanceState != null) {
                position = savedInstanceState.getInt(POSITION, selectFirstItemByDefault ? 0 : -1);
            } else {
                position = selectFirstItemByDefault ? 0 : -1;
            }
        }

        adapter = createAdapter();
        setListAdapter(adapter);

        final PostListLoadingRunnable onPostExecute = new PostListLoadingRunnable(position, lv);

        listLoader = createAsyncLoader(adapter, onPostExecute);

        if (listLoader != null) {
            listLoader.execute();
        } else {
            // we need to schedule onPostExecute in order to be after all pending transaction in fragment manager
            new Handler().post(onPostExecute);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        eventManager.fire(FragmentGuiEventType.started.newEvent(this));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (adapter != null) {
            outState.putInt(POSITION, adapter.getSelectedItemPosition());
        }

        if (listViewFilter != null) {
            listViewFilter.saveState(outState);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listLoader != null) {
            listLoader.cancel(false);
        }

        if (userEventListener != null) {
            this.userService.removeListener(userEventListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void filter(@Nonnull CharSequence searchText) {
        if (this.adapter != null) {
            if (this.adapter.isInitialized()) {
                this.adapter.filter(searchText);
            }
        }
    }

    @Nonnull
    protected abstract MessengerListItemAdapter<LI> createAdapter();

    @Nullable
    protected abstract MessengerAsyncTask<Void, Void, List<T>> createAsyncLoader(@Nonnull MessengerListItemAdapter<LI> adapter, @Nonnull Runnable onPostExecute);

    /*
    **********************************************************************
    *
    *                           SCROLLING
    *
    **********************************************************************
    */

    @Override
    public final void onScrollStateChanged(AbsListView view, int scrollState) {
        // do nothing
    }

    @Override
    public final void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // we want to notify subclasses about several events
        // 1. onListViewTopReached
        // 2. onListViewBottomReached
        // 3. onItemReachedFromTop
        // 4. onItemReachedFromBottom

        if (this.firstVisibleItem.get() >= 0 && visibleItemCount > 0) {
            boolean scrollUp = false;
            boolean scrollDown = false;
            if (firstVisibleItem < this.firstVisibleItem.get()) {
                scrollUp = true;
            }
            if (firstVisibleItem > this.firstVisibleItem.get()) {
                scrollDown = true;
            }

            final int lastVisibleItem = firstVisibleItem + visibleItemCount;

            switch (view.getId()) {
                case android.R.id.list:
                    if (scrollUp && firstVisibleItem == 0) {
                        // reach top
                        onListViewTopReached();
                    } else {
                        if (scrollDown && lastVisibleItem == totalItemCount) {
                            // reach bottom
                            onListViewBottomReached();
                        }
                    }

                    if (scrollDown) {
                        onItemReachedFromTop(lastVisibleItem);
                    }

                    if (scrollUp) {
                        onItemReachedFromBottom(lastVisibleItem);
                    }

                    break;
            }
        }

        this.firstVisibleItem.set(firstVisibleItem);

    }

    protected void onItemReachedFromTop(int position) {
    }

    protected void onItemReachedFromBottom(int position) {
    }

    protected void onListViewBottomReached() {
    }

    protected void onListViewTopReached() {
    }

    /*
    **********************************************************************
    *
    *                           LISTENERS, HELPERS, ETC
    *
    **********************************************************************
    */

    private class UiThreadUserEventListener implements UserEventListener {

        @Override
        public void onUserEvent(@Nonnull final User eventUser, @Nonnull final UserEventType userEventType, final @Nullable Object data) {
            AThreads.tryRunOnUiThread(getActivity(), new Runnable() {
                @Override
                public void run() {
                    adapter.onUserEvent(eventUser, userEventType, data);
                }
            });
        }
    }

    private class PostListLoadingRunnable implements Runnable {

        private final int position;

        @Nonnull
        private final ListView listView;

        public PostListLoadingRunnable(int position, @Nonnull ListView lv) {
            this.position = position;
            listView = lv;
        }

        @Override
        public void run() {
            // change adapter state
            adapter.setInitialized(true);

            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {

                // change UI state
                setListShown(true);

                // apply filter if any
                if (listViewFilter != null) {
                    filter(listViewFilter.getFilterText());
                } else {
                    filter("");
                }

                if (position >= 0 && position < adapter.getCount()) {
                    adapter.getSelectedItemListener().onItemClick(position);

                    if (multiPaneManager.isDualPane(activity)) {
                        final ListItem.OnClickAction onClickAction = adapter.getItem(position).getOnClickAction();
                        if (onClickAction != null) {
                            onClickAction.onClick(activity, adapter, listView);
                        }
                    }
                }
            }
        }
    }

    private class ListViewOnItemClickListener implements AdapterView.OnItemClickListener {

        public void onItemClick(final AdapterView<?> parent,
                                final View view,
                                final int position,
                                final long id) {
            final Object itemAtPosition = parent.getItemAtPosition(position);

            if (itemAtPosition instanceof ListItem) {
                final ListItem listItem = (ListItem) itemAtPosition;
                // notify adapter

                adapter.getSelectedItemListener().onItemClick(listItem);

                final ListItem.OnClickAction onClickAction = listItem.getOnClickAction();
                if (onClickAction != null) {
                    onClickAction.onClick(getActivity(), adapter, getListView());
                }
            }
        }
    }

    private class ListViewOnItemLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final Object itemAtPosition = parent.getItemAtPosition(position);

            if (itemAtPosition instanceof ListItem) {
                final ListItem listItem = (ListItem) itemAtPosition;
                // notify adapter

                final ListItem.OnClickAction onClickAction = listItem.getOnLongClickAction();
                if (onClickAction != null) {
                    onClickAction.onClick(getActivity(), adapter, getListView());
                    return true;
                }
            }

            return false;
        }
    }
}

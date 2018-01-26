package com.marverenic.music.ui.library.recentlyadded;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentRecentlyAddedBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.common.OnSongSelectedListener;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public class RecentlyAddedFragment extends BaseFragment {

    private static final long RECENT_THRESHOLD_SEC = 30 * 24 * 60 * 60; // 30 days

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PreferenceStore mPreferenceStore;

    private FragmentRecentlyAddedBinding mBinding;
    private RecentlyAddedViewModel mViewModel;

    public static RecentlyAddedFragment newInstance() {
        return new RecentlyAddedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recently_added,
                container, false);

        mViewModel = new RecentlyAddedViewModel(getContext(), mPlayerController, mMusicStore,
                mPlaylistStore, getFragmentManager(),
                OnSongSelectedListener.defaultImplementation(getActivity(), mPreferenceStore));

        mBinding.setViewModel(mViewModel);
        setupToolbar(mBinding.toolbar);

        mMusicStore.getSongs()
                .flatMap(allSongs -> {
                    return Observable.from(allSongs)
                            .filter(song -> {
                                long dT = System.currentTimeMillis() / 1000 - song.getDateAdded();
                                return dT < RECENT_THRESHOLD_SEC;
                            })
                            .toList();
                })
                .subscribe(recentlyAdded -> {
                    mViewModel.setSongs(recentlyAdded);
                }, throwable -> {
                    Timber.e("Failed to update recently added items", throwable);
                });

        mPlayerController.getNowPlaying()
                .subscribe(mViewModel::setCurrentlyPlaying, throwable -> {
                    Timber.e("Failed to update currently playing song", throwable);
                });

        return mBinding.getRoot();
    }

    private void setupToolbar(Toolbar toolbar) {
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
        }
    }
}
